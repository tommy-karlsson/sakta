package com.github.tommykarlsson.sakta.core.benchmark;

import java.util.BitSet;
import java.util.concurrent.TimeUnit;

import com.github.tommykarlsson.sakta.core.ActorRef;
import com.github.tommykarlsson.sakta.core.ActorSystem;

import org.awaitility.Awaitility;

public class SaktaMassiveTellToActorGroup {
    static void run(int messageCount, int actorCount, ActorSystem actorSystem) {
        try (actorSystem) {
            BitSet bitset = new BitSet(messageCount * actorCount);

            for (int a = 0; a < actorCount; a++) {
                ActorRef<Actor> runner = actorSystem.getOrCreateActorRef(a, Actor::new, Actor.class);
                int aa = a;
                for (int m = 0; m < messageCount; m++) {
                    int mm = m;
                    runner.ask(r -> r.run(new int[] {aa, mm})).thenAccept(response -> {
                        int actorNo = response[0];
                        int messageNo = response[1];
                        bitset.clear(actorNo * messageCount + messageNo);
                    });
                }
            }

            Awaitility.await()
                    .atMost(30, TimeUnit.SECONDS)
                    .until(bitset::isEmpty);
        }
    }

    private static class Actor {
        int[] run(int[] msg) {
            return msg;
        }
    }
}
