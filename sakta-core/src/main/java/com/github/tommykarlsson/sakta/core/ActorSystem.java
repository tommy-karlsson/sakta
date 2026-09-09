package com.github.tommykarlsson.sakta.core;

import com.github.tommykarlsson.sakta.core.impl.ActorRefImpl;
import com.github.tommykarlsson.sakta.core.impl.UnboundedMailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.VirtualThreadPerActorScheduler;

import java.lang.ref.Cleaner;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/**
 * The actor system is not {@link AutoCloseable}: it is not the sort of thing a block owns, and how
 * long to let the actors finish is a decision for whoever is shutting the application down, not one
 * to make by leaving a scope. Ask it to {@link #shutdown()} and then
 * {@link #awaitTermination(Duration)} for as long as suits, or {@link #shutdownNow()} to stop
 * waiting.
 */
public class ActorSystem {

    private final MailboxFactory mailboxFactory;
    private final Scheduler scheduler;
    private final State state;
    private final Cleaner.Cleanable cleanable;

    public ActorSystem() {
        this(new UnboundedMailboxFactory());
    }

    public ActorSystem(MailboxFactory mailboxFactory) {
        this(mailboxFactory, new VirtualThreadPerActorScheduler());
    }

    /**
     * @param mailboxFactory The factory for the actors' mailboxes. Decorating what the actors run,
     *                       for instance with {@link MailItemDecorator}s, is a matter of what
     *                       factory is handed in here; see
     *                       {@link com.github.tommykarlsson.sakta.core.impl.MailItemDecoratingMailboxFactory}.
     * @param scheduler      The scheduler that drains the mailboxes.
     */
    public ActorSystem(MailboxFactory mailboxFactory, Scheduler scheduler) {
        this.mailboxFactory = mailboxFactory;
        this.scheduler = scheduler;
        this.state = new State();
        this.cleanable = Cleaner.create().register(this, new StopActors(state));
    }

    @SuppressWarnings("unchecked")
    public <T> ActorRef<T> getOrCreateActorRef(Object address, Supplier<T> actorSupplier, Class<T> actorType) {
        return (ActorRef<T>) state.actors
                .computeIfAbsent(new ActorKey(address, actorType), a -> createAndStartActorRef(actorSupplier))
                .actorRef();
    }

    private <T> Registration createAndStartActorRef(Supplier<T> actorSupplier) {
        Mailbox mailbox = mailboxFactory.createMailbox();
        ActorRef<T> actorRef = new ActorRefImpl<>(actorSupplier.get(), mailbox, scheduler);
        actorRef.start();
        return new Registration(actorRef, mailbox);
    }

    /**
     * Stops the actors accepting anything further, and lets them finish what they have already been
     * sent. Returns without waiting for them; {@link #awaitTermination(Duration)} is the wait.
     *
     * <p>Sending to an actor after this throws {@link java.util.concurrent.RejectedExecutionException},
     * and an ask that is rejected fails rather than never answering.
     */
    public void shutdown() {
        state.stopAccepting();
    }

    /**
     * Stops the actors accepting anything further, gives up on whatever they have not started, and
     * interrupts what they are in the middle of.
     *
     * <p>Every item given up on is discarded rather than dropped, so an ask waiting on one fails
     * with {@link java.util.concurrent.RejectedExecutionException} rather than never answering.
     *
     * @return The items that will now never run.
     */
    public List<MailItem> shutdownNow() {
        state.stopAccepting();
        List<MailItem> discarded = state.discardQueued();
        stopActors();
        return discarded;
    }

    /**
     * {@link #shutdown()}, and then waits up to the timeout for the actors to finish what they were
     * already sent. Whatever has not finished by then is given up on with {@link #shutdownNow()}.
     *
     * <p>Giving up interrupts the actors rather than waiting for them, so when this returns false
     * they may still be winding down. Wait for them with {@link #awaitTermination(Duration)} if that
     * matters.
     *
     * @param timeout How long to let the actors finish.
     * @return true if they all finished in time, false if the rest had to be given up on.
     * @throws InterruptedException If the waiting thread is interrupted.
     */
    public boolean shutdown(Duration timeout) throws InterruptedException {
        shutdown();
        if (awaitTermination(timeout)) {
            return true;
        }
        shutdownNow();
        return false;
    }

    /**
     * Waits for the actors to finish, after having been asked to stop.
     *
     * @param timeout How long to wait for all of the actors together.
     * @return true if every actor had finished within the timeout.
     * @throws InterruptedException If the waiting thread is interrupted.
     */
    public boolean awaitTermination(Duration timeout) throws InterruptedException {
        return state.awaitStopped(timeout);
    }

    /**
     * Interrupts the actors, through the cleaner, which both runs the interrupting and releases the
     * cleaner now that there is nothing left for it to do later.
     */
    private void stopActors() {
        cleanable.clean();
    }


    private record ActorKey(Object address, Class<?> actorClass) { }

    private record Registration(ActorRef<?> actorRef, Mailbox mailbox) { }

    /**
     * What the cleaner runs, which is the last resort for an actor system that is collected without
     * having been shut down. It holds the state rather than the actor system, since holding the
     * actor system would keep it from ever being collected, and it only interrupts, since the
     * cleaner's thread is no place to wait for anything.
     */
    private record StopActors(State state) implements Runnable {

        @Override
        public void run() {
            state.stopActors();
        }
    }

    private static class State {

        private final ConcurrentMap<ActorKey, Registration> actors;

        State() {
            this.actors = new ConcurrentHashMap<>();
        }

        /** Interrupts the actors, without waiting for any of them to notice. */
        void stopActors() {
            actors.values().forEach(registration -> registration.actorRef().stop());
        }

        void stopAccepting() {
            actors.values().forEach(registration -> registration.mailbox().close());
        }

        /**
         * Takes what the actors have not started and tells whoever was waiting on it. Done after
         * the mailboxes are closed, so that nothing can arrive behind the sweep.
         */
        List<MailItem> discardQueued() {
            List<MailItem> discarded = new ArrayList<>();
            for (Registration registration : actors.values()) {
                discarded.addAll(registration.mailbox().discardQueued());
            }
            RejectedExecutionException cause = new RejectedExecutionException("Actor system was shut down");
            discarded.forEach(item -> item.action().discard(cause));
            return discarded;
        }

        /**
         * Waits for actors that have already been asked to stop. Asking all of them before waiting
         * for any of them is what lets them wind down alongside each other, rather than one at a
         * time.
         */
        boolean awaitStopped(Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            boolean allStopped = true;
            for (Registration registration : actors.values()) {
                ActorRef<?> actorRef = registration.actorRef();
                Duration remaining = Duration.ofNanos(deadline - System.nanoTime());
                if (remaining.isNegative() || remaining.isZero()) {
                    return false;
                }
                allStopped &= actorRef.awaitStopped(remaining);
            }
            return allStopped;
        }
    }
}
