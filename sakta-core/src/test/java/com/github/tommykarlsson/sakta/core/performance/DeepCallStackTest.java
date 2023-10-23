package com.github.tommykarlsson.sakta.core.performance;

import com.github.tommykarlsson.sakta.core.ActorSystem;
import com.github.tommykarlsson.sakta.core.impl.BoundedMailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.ForkJoinPoolScheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class DeepCallStackTest {

    private ActorSystem actorSystem;

    @BeforeEach
    public void beforeEach() {
        this.actorSystem = new ActorSystem(new BoundedMailboxFactory(), new ForkJoinPoolScheduler());
    }

    @AfterEach
    public void afterEach() {
        this.actorSystem.close();
    }

    @Test
    void test() throws ExecutionException, InterruptedException {
        CompletableFuture<Integer> completion =
                actorSystem.getOrCreateActorRef(1, () -> new Actor(actorSystem, 1_000_000), Actor.class)
                        .flatAsk(a -> a.increment(1));
        assertEquals(1_000_000, completion.get());
    }


    private record Actor(ActorSystem actorSystem, int limit) {

        CompletableFuture<Integer> increment(int current) {
            if (current >= limit) {
                return CompletableFuture.completedFuture(1);
            }

            CompletableFuture<Integer> foo = actorSystem.getOrCreateActorRef(current + 1, () -> new Actor(actorSystem, limit), Actor.class)
                    .flatAsk(a -> a.increment(current + 1));

            return foo.thenApplyAsync(i -> i + 1);
        }
    }
}
