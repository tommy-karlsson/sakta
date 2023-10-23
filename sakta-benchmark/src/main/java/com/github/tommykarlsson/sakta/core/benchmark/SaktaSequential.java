package com.github.tommykarlsson.sakta.core.benchmark;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.github.tommykarlsson.sakta.core.ActorSystem;

public class SaktaSequential {
    static void run(int actorCount, ActorSystem actorSystem) {

        try (actorSystem) {
            CompletableFuture<Integer> completion =
                    actorSystem.getOrCreateActorRef(1, () -> new Actor(actorSystem, actorCount), Actor.class)
                            .flatAsk(a -> a.increment(1));
            completion.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    private record Actor(ActorSystem actorSystem, int limit) {

        CompletableFuture<Integer> increment(int current) {
            if (current >= limit) {
                return CompletableFuture.completedFuture(1);
            }

            CompletableFuture<Integer> foo = actorSystem.getOrCreateActorRef(current + 1, () -> new Actor(actorSystem, limit), Actor.class)
                    .flatAsk(a -> a.increment(current + 1));

            if (current % 500 == 0) {
                return foo.thenApplyAsync(i -> i + 1);
            } else {
                return foo.thenApply(i -> i + 1);
            }
        }
    }
}
