package com.github.tommykarlsson.sakta.core;

import java.time.Duration;
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
     * <p>The function runs on the actor's thread. The future it returns does not: whatever is chained onto
     * that future runs wherever the future is completed, which is typically another actor's thread. Anything
     * chained on is therefore outside the actor's serial processing, and must not touch the actor's state. To
     * act on the result as the actor, send it back to the actor as another message.
     *
     * <p>The actor also takes its next message as soon as the function returns, rather than when the future
     * completes, so two of these can complete in either order.
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
     * Stop the actor ref. This only signals the actor to stop, and returns without waiting for it
     * to finish; use {@link #awaitStopped(Duration)} when that matters.
     */
    void stop();

    /**
     * Waits for the actor to finish processing. Only meaningful once {@link #stop()} has been
     * called, as nothing else asks the actor to stop.
     *
     * @param timeout How long to wait.
     * @return true if the actor had finished within the timeout.
     * @throws InterruptedException If the waiting thread is interrupted.
     */
    boolean awaitStopped(Duration timeout) throws InterruptedException;
}
