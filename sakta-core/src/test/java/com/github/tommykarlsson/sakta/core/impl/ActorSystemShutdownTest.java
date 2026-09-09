package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.ActorSystem;
import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.Schedule;
import com.github.tommykarlsson.sakta.core.Scheduler;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ActorSystemShutdownTest {

    private static final Duration PATIENT = Duration.ofSeconds(10);
    private static final Duration BRIEF = Duration.ofMillis(200);

    @Test
    void awaitStoppedReturnsOnlyOnceTheActorThreadIsGone() throws InterruptedException {
        Mailbox mailbox = new UnboundedMailbox();
        Schedule schedule = new VirtualThreadPerActorScheduler().schedule(mailbox);

        assertFalse(schedule.awaitStopped(BRIEF), "a running actor should not report itself stopped");

        schedule.dispose();

        assertTrue(schedule.awaitStopped(PATIENT));
    }

    @Test
    void closeWithTimeoutWaitsForEveryActor() throws InterruptedException {
        ActorSystem actorSystem = newActorSystem(new VirtualThreadPerActorScheduler());
        for (int i = 0; i < 1_000; i++) {
            actorSystem.getOrCreateActorRef(i, Actor::new, Actor.class).tell(Actor::noop);
        }

        assertTrue(actorSystem.close(PATIENT));
    }

    @Test
    void closeWithTimeoutAlsoWorksForTheForkJoinScheduler() throws InterruptedException {
        ActorSystem actorSystem = newActorSystem(new ForkJoinPoolScheduler());
        for (int i = 0; i < 1_000; i++) {
            actorSystem.getOrCreateActorRef(i, Actor::new, Actor.class).tell(Actor::noop);
        }

        assertTrue(actorSystem.close(PATIENT));
    }

    /** Closing without a timeout leaves the actors winding down, but it does stop them. */
    @Test
    void closeWithoutTimeoutStillStopsTheActors() throws InterruptedException {
        ActorSystem actorSystem = newActorSystem(new VirtualThreadPerActorScheduler());
        actorSystem.getOrCreateActorRef("actor", Actor::new, Actor.class).tell(Actor::noop);

        actorSystem.close();

        assertTrue(actorSystem.close(PATIENT), "the actors signalled by close() should still finish");
    }

    private static ActorSystem newActorSystem(Scheduler scheduler) {
        return new ActorSystem(new UnboundedMailboxFactory(), scheduler);
    }

    private static class Actor {
        void noop() {
        }
    }
}
