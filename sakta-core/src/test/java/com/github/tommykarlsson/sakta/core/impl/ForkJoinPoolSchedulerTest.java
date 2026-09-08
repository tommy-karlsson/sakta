package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.ActorRef;
import com.github.tommykarlsson.sakta.core.ActorSystem;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ForkJoinPoolSchedulerTest {

    private static final int SENDERS = 8;
    private static final int MESSAGES_PER_SENDER = 20_000;
    private static final int TOTAL_MESSAGES = SENDERS * MESSAGES_PER_SENDER;

    private ActorSystem actorSystem;
    private CountingActor actor;

    @BeforeEach
    public void beforeEach() {
        this.actorSystem = new ActorSystem(new UnboundedMailboxFactory(), new ForkJoinPoolScheduler());
        this.actor = new CountingActor();
    }

    @AfterEach
    public void afterEach() {
        this.actorSystem.close();
    }

    /**
     * The scheduler only submits a drain when none is already in flight, so a send that skips its
     * submission depends on the running drain noticing the item on its way out. A message stranded
     * in the mailbox shows up here as a count that never reaches the total.
     */
    @Test
    void deliversEveryMessageWhenSendersSkipSubmittingADrain() {
        sendConcurrently();

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> actor.count() == TOTAL_MESSAGES);
    }

    @Test
    void runsOneMessageAtATimePerActor() {
        sendConcurrently();

        Awaitility.await()
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> actor.count() == TOTAL_MESSAGES);
        assertEquals(0, actor.overlaps(), "two drains ran against the same actor at the same time");
    }

    private void sendConcurrently() {
        ActorRef<CountingActor> ref = actorSystem.getOrCreateActorRef("counter", () -> actor, CountingActor.class);

        try (ExecutorService senders = Executors.newFixedThreadPool(SENDERS)) {
            for (int s = 0; s < SENDERS; s++) {
                senders.submit(() -> {
                    for (int m = 0; m < MESSAGES_PER_SENDER; m++) {
                        ref.tell(CountingActor::increment);
                    }
                });
            }
        }
    }

    /**
     * Counts through atomics only so that the test thread can watch the progress without sending
     * a message of its own, which would schedule a drain and hide a message that got stranded.
     */
    private static final class CountingActor {

        private final AtomicInteger count = new AtomicInteger();
        private final AtomicInteger inProgress = new AtomicInteger();
        private final AtomicInteger overlaps = new AtomicInteger();

        void increment() {
            if (inProgress.incrementAndGet() != 1) {
                overlaps.incrementAndGet();
            }
            count.incrementAndGet();
            inProgress.decrementAndGet();
        }

        int count() {
            return count.get();
        }

        int overlaps() {
            return overlaps.get();
        }
    }
}
