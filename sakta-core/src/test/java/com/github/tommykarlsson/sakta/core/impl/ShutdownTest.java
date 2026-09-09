package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.ActorRef;
import com.github.tommykarlsson.sakta.core.ActorSystem;
import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.Scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ShutdownTest {

    private static final Duration PATIENT = Duration.ofSeconds(10);

    static List<Supplier<Scheduler>> schedulers() {
        return List.of(VirtualThreadPerActorScheduler::new, ForkJoinPoolScheduler::new);
    }

    @ParameterizedTest
    @MethodSource("schedulers")
    void shutdownRunsWhatWasAlreadySent(Supplier<Scheduler> scheduler) throws Exception {
        ActorSystem system = newSystem(scheduler);
        Counter counter = new Counter();
        ActorRef<Counter> ref = system.getOrCreateActorRef("counter", () -> counter, Counter.class);
        for (int i = 0; i < 500; i++) {
            ref.tell(Counter::increment);
        }

        system.shutdown();

        assertTrue(system.awaitTermination(PATIENT));
        assertEquals(500, counter.count(), "shutdown should let the actors finish what they were sent");
    }

    @ParameterizedTest
    @MethodSource("schedulers")
    void sendingAfterShutdownIsRejected(Supplier<Scheduler> scheduler) {
        ActorSystem system = newSystem(scheduler);
        ActorRef<Counter> ref = system.getOrCreateActorRef("counter", Counter::new, Counter.class);

        system.shutdown();

        assertThrows(RejectedExecutionException.class, () -> ref.tell(Counter::increment));
    }

    /**
     * The case that used to leave a caller waiting on a future nobody would ever complete: an ask
     * queued behind a busy actor when the system is stopped without draining.
     */
    @ParameterizedTest
    @MethodSource("schedulers")
    void shutdownNowFailsTheAsksItGivesUpOn(Supplier<Scheduler> scheduler) throws Exception {
        ActorSystem system = newSystem(scheduler);
        Blocker blocker = new Blocker();
        ActorRef<Blocker> ref = system.getOrCreateActorRef("blocker", () -> blocker, Blocker.class);

        ref.ask(Blocker::block);
        assertTrue(blocker.entered.await(5, TimeUnit.SECONDS));
        CompletableFuture<Integer> queued = ref.ask(Blocker::answer);

        List<MailItem> discarded = system.shutdownNow();
        blocker.release.countDown();

        ExecutionException failure = assertThrows(ExecutionException.class, () -> queued.get(5, TimeUnit.SECONDS));
        assertInstanceOf(RejectedExecutionException.class, failure.getCause());
        assertEquals(1, discarded.size(), "the ask that never ran should be reported as given up on");
    }

    /** Waiting for an actor that will not finish reports that it did not, and leaves it running. */
    @Test
    void awaitTerminationReportsAnActorThatWillNotFinish() throws Exception {
        ActorSystem system = newSystem(VirtualThreadPerActorScheduler::new);
        Blocker blocker = new Blocker();
        ActorRef<Blocker> ref = system.getOrCreateActorRef("blocker", () -> blocker, Blocker.class);
        ref.ask(Blocker::block);
        assertTrue(blocker.entered.await(5, TimeUnit.SECONDS));

        system.shutdown();

        assertFalse(system.awaitTermination(Duration.ofMillis(200)), "it should report that it could not drain");

        system.shutdownNow();
        blocker.release.countDown();
        assertTrue(system.awaitTermination(PATIENT), "giving up should still leave the system stopped");
    }

    @Test
    void anAskThatIsStillQueuedWhenTheSystemDrainsIsAnswered() throws Exception {
        ActorSystem system = newSystem(VirtualThreadPerActorScheduler::new);
        ActorRef<Counter> ref = system.getOrCreateActorRef("counter", Counter::new, Counter.class);
        for (int i = 0; i < 100; i++) {
            ref.tell(Counter::increment);
        }
        CompletableFuture<Integer> last = ref.ask(Counter::count);

        system.shutdown();
        assertTrue(system.awaitTermination(PATIENT));

        assertEquals(100, last.get(5, TimeUnit.SECONDS));
    }

    /**
     * An actor with nothing to do is waiting on its mailbox, and has no reason to look at it again.
     * Closing the mailbox has to be what rouses it, or it waits there for good.
     */
    @ParameterizedTest
    @MethodSource("schedulers")
    void shutdownStopsAnActorThatIsSittingIdle(Supplier<Scheduler> scheduler) throws Exception {
        ActorSystem system = newSystem(scheduler);
        ActorRef<Counter> ref = system.getOrCreateActorRef("counter", Counter::new, Counter.class);
        ref.ask(Counter::count).get(5, TimeUnit.SECONDS);
        Thread.sleep(100);

        system.shutdown();

        assertTrue(system.awaitTermination(Duration.ofSeconds(2)), "an idle actor should still stop");
    }

    /** An action that is already running is left to finish, rather than being cut off. */
    @ParameterizedTest
    @MethodSource("schedulers")
    void shutdownLetsAnActionThatIsAlreadyRunningFinish(Supplier<Scheduler> scheduler) throws Exception {
        ActorSystem system = newSystem(scheduler);
        Blocker blocker = new Blocker();
        ActorRef<Blocker> ref = system.getOrCreateActorRef("blocker", () -> blocker, Blocker.class);
        CompletableFuture<Integer> running = ref.ask(Blocker::block);
        assertTrue(blocker.entered.await(5, TimeUnit.SECONDS));

        system.shutdown();
        blocker.release.countDown();

        assertEquals(1, running.get(5, TimeUnit.SECONDS));
        assertTrue(system.awaitTermination(PATIENT));
    }

    /**
     * Stopping used to depend entirely on the interrupt, so an action that swallowed it kept the
     * actor alive for good. The closed mailbox is now what ends the loop.
     */
    @Test
    void shutdownNowStopsAnActorWhoseActionSwallowsTheInterrupt() throws Exception {
        ActorSystem system = newSystem(VirtualThreadPerActorScheduler::new);
        Blocker blocker = new Blocker();
        ActorRef<Blocker> ref = system.getOrCreateActorRef("blocker", () -> blocker, Blocker.class);
        ref.ask(Blocker::block);
        assertTrue(blocker.entered.await(5, TimeUnit.SECONDS));

        system.shutdownNow();
        blocker.release.countDown();

        assertTrue(system.awaitTermination(PATIENT), "the closed mailbox should stop it regardless");
    }

    /** Terminated means the work is done and no more can arrive. */
    @ParameterizedTest
    @MethodSource("schedulers")
    void terminationMeansTheActorsAreDone(Supplier<Scheduler> scheduler) throws Exception {
        ActorSystem system = newSystem(scheduler);
        Counter counter = new Counter();
        ActorRef<Counter> ref = system.getOrCreateActorRef("counter", () -> counter, Counter.class);
        ref.tell(Counter::increment);

        system.shutdown();
        assertTrue(system.awaitTermination(PATIENT));

        assertEquals(1, counter.count(), "what was sent before the shutdown should have run");
        assertThrows(RejectedExecutionException.class, () -> ref.tell(Counter::increment));
    }

    private static ActorSystem newSystem(Supplier<Scheduler> scheduler) {
        return new ActorSystem(new UnboundedMailboxFactory(), scheduler.get());
    }

    private static class Counter {
        private final AtomicInteger count = new AtomicInteger();

        void increment() {
            count.incrementAndGet();
        }

        int count() {
            return count.get();
        }
    }

    private static class Blocker {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        int block() {
            entered.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 1;
        }

        int answer() {
            return 2;
        }
    }
}
