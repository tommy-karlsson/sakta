package com.github.tommykarlsson.sakta.core;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The reference to an actor that must be used when interacting with the actor. ActorRef is created with
 * {@link ActorSystem#getOrCreateActorRef(Object, Supplier, Class)}.
 *
 * @param <T> The type of the POJO actor.
 */
public interface ActorRef<T> {

    /**
     * Send a message to the actor, without waiting for any form of response. This method returns immediately, i.e.
     * most likely before the actor has processed the message.
     *
     * @param teller The consumer that executes the "telling" action on the actor.
     */
    void tell(Consumer<T> teller);

    /**
     * Send a message to the actor, and expect a response. This method returns immediately, i.e. most likely before
     * the actor has processed the message, but the returned {@link CompletableFuture} can be used to await the result.
     *
     * @param asker The function that executes the asking action on the actor, and produces the result.
     * @param <U>   The result type.
     * @return Completable future that provides the result (or exception if completed exceptionally).
     */
    <U> CompletableFuture<U> ask(Function<T, U> asker);

    /**
     * Same functionality as {@link #ask(Function)}, but to be used when the result of the supplied function
     * is already a {@link CompletableFuture}.
     *
     * @param asker The function that executes the asking action on the actor, and produces the result.
     * @param <U> The result type.
     * @return Completable future that provides the result (or exception if completed exceptionally).
     */
    <U> CompletableFuture<U> flatAsk(Function<T, CompletableFuture<U>> asker);

    /**
     * Start the actor ref. Note that the actor ref is automatically started when created with
     * {@link ActorSystem#getOrCreateActorRef(Object, Supplier, Class)}.
     */
    void start();

    /**
     * Stop the actor ref.
     */
    void stop();
}
