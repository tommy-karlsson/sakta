package com.github.tommykarlsson.sakta.core.benchmark;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import com.github.tommykarlsson.sakta.core.ActorRef;
import com.github.tommykarlsson.sakta.core.ActorSystem;

import org.awaitility.Awaitility;

public class SaktaMassiveTellToActorGroup {
    static void run(int messageCount, int actorCount, ActorSystem actorSystem) {
        try (actorSystem) {
            OutstandingMessages outstanding = new OutstandingMessages(messageCount * actorCount);

            for (int a = 0; a < actorCount; a++) {
                ActorRef<Actor> runner = actorSystem.getOrCreateActorRef(a, Actor::new, Actor.class);
                int aa = a;
                for (int m = 0; m < messageCount; m++) {
                    int mm = m;
                    outstanding.expect(aa * messageCount + mm);
                    runner.ask(r -> r.run(new int[] {aa, mm})).thenAccept(response -> {
                        int actorNo = response[0];
                        int messageNo = response[1];
                        outstanding.received(actorNo * messageCount + messageNo);
                    });
                }
            }

            Awaitility.await()
                    .atMost(30, TimeUnit.SECONDS)
                    .until(outstanding::allReceived);
        }
    }

    /**
     * The messages that have been sent but not yet responded to, one bit per message. The bit
     * makes a response count only once, and only for a message that was actually sent, so a
     * lost or duplicated response leaves the benchmark waiting instead of passing silently.
     * Responses arrive on many actor threads, hence the atomics.
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
