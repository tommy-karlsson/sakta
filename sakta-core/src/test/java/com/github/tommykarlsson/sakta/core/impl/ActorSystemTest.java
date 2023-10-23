package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.ActorRef;
import com.github.tommykarlsson.sakta.core.ActorSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class ActorSystemTest {

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
    void tellAndAsk() throws ExecutionException, InterruptedException {

        ActorRef<CounterActor> fooRef = actorSystem.getOrCreateActorRef("foo", CounterActor::new, CounterActor.class);

        fooRef.tell(c -> c.add(5));
        fooRef.tell(c -> c.add(13));
        int count = fooRef.ask(CounterActor::getCount).get();

        assertEquals(18, count);
    }

    @Test
    void askError() {

        ActorRef<CounterActor> fooRef = actorSystem.getOrCreateActorRef("foo", CounterActor::new, CounterActor.class);

        CompletableFuture<Integer> cf = fooRef.ask(c -> c.divideByZero(12));
        ExecutionException ex = assertThrows(ExecutionException.class, cf::get);
        assertEquals(ex.getCause().getClass(), ArithmeticException.class);
    }


    private static class CounterActor {

        private int count = 0;

        void add(int amount) {
            this.count += amount;
        }

        int getCount() {
            return count;
        }

        int divideByZero(int value) {
            return value / 0;
        }
    }
}
