package com.github.tommykarlsson.sakta.core.performance;

import com.github.tommykarlsson.sakta.core.ActorRef;
import com.github.tommykarlsson.sakta.core.ActorSystem;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParallelCallsTest {

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
    void test() {
        int actorCount = 100;
        int messageCount = 100_000;
        BitSet bitset = new BitSet(messageCount * actorCount);

        for (int a = 0; a < actorCount; a++) {
            ActorRef<Actor> runner = actorSystem.getOrCreateActorRef(a, Actor::new, Actor.class);
            int aa = a;
            for (int m = 0; m < messageCount; m++) {
                int mm = m;
                runner.ask(r -> r.run(new int[]{aa, mm})).thenAccept(response -> {
                    int actorNo = response[0];
                    int messageNo = response[1];
                    bitset.clear(actorNo * messageCount + messageNo);
                });
            }
        }

        Awaitility.await().untilAsserted(() -> assertTrue(bitset.isEmpty()));
    }

    private static class Actor {
        int[] run(int[] msg) {
            return msg;
        }
    }
}
