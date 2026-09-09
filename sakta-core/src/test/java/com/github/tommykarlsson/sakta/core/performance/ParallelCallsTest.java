package com.github.tommykarlsson.sakta.core.performance;

import com.github.tommykarlsson.sakta.core.ActorRef;
import com.github.tommykarlsson.sakta.core.ActorSystem;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ParallelCallsTest {

    private static final int ACTOR_COUNT = 100;
    private static final int MESSAGE_COUNT = 100_000;

    private ActorSystem actorSystem;

    @BeforeEach
    public void beforeEach() {
        this.actorSystem = new ActorSystem();
    }

    @AfterEach
    public void afterEach() {
        this.actorSystem.close();
    }

    @Test
    void everyMessageIsRespondedTo() {
        OutstandingMessages outstanding = new OutstandingMessages(MESSAGE_COUNT * ACTOR_COUNT);

        for (int a = 0; a < ACTOR_COUNT; a++) {
            ActorRef<Actor> runner = actorSystem.getOrCreateActorRef(a, Actor::new, Actor.class);
            int aa = a;
            for (int m = 0; m < MESSAGE_COUNT; m++) {
                int mm = m;
                outstanding.expect(aa * MESSAGE_COUNT + mm);
                runner.ask(r -> r.run(new int[]{aa, mm})).thenAccept(response -> {
                    int actorNo = response[0];
                    int messageNo = response[1];
                    outstanding.received(actorNo * MESSAGE_COUNT + messageNo);
                });
            }
        }

        Awaitility.await()
                .atMost(Duration.ofMinutes(1))
                .until(outstanding::allReceived);
    }

    @Test
    void anUnansweredMessageIsNotMistakenForSuccess() {
        OutstandingMessages outstanding = new OutstandingMessages(2);
        outstanding.expect(0);
        outstanding.expect(1);

        outstanding.received(0);

        assertFalse(outstanding.allReceived(), "one message is still outstanding");
    }

    /**
     * The messages that have been sent but not yet responded to, one bit per message, so that a
     * response counts only once and only for a message that was actually sent. Responses arrive on
     * many actor threads, hence the atomics.
     */
    private static final class OutstandingMessages {

        private final AtomicLongArray sent;
        private final AtomicLong outstanding = new AtomicLong();

        OutstandingMessages(int messages) {
            this.sent = new AtomicLongArray((messages + Long.SIZE - 1) / Long.SIZE);
        }

        /** Must be called before sending, so that the response cannot arrive before the bit is set. */
        void expect(int message) {
            int word = message >>> 6;
            long bit = 1L << message;
            long current;
            do {
                current = sent.get(word);
            } while (!sent.compareAndSet(word, current, current | bit));
            outstanding.incrementAndGet();
        }

        void received(int message) {
            int word = message >>> 6;
            long bit = 1L << message;
            long current;
            do {
                current = sent.get(word);
                if ((current & bit) == 0) {
                    return;
                }
            } while (!sent.compareAndSet(word, current, current & ~bit));
            outstanding.decrementAndGet();
        }

        boolean allReceived() {
            return outstanding.get() == 0;
        }
    }

    private static class Actor {
        int[] run(int[] msg) {
            return msg;
        }
    }
}
