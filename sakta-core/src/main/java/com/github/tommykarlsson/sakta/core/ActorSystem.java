package com.github.tommykarlsson.sakta.core;

import com.github.tommykarlsson.sakta.core.impl.ActorRefImpl;
import com.github.tommykarlsson.sakta.core.impl.UnboundedMailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.VirtualThreadPerActorScheduler;

import java.lang.ref.Cleaner;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public class ActorSystem implements AutoCloseable {

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
        this.cleanable = Cleaner.create().register(this, state);
    }

    @SuppressWarnings("unchecked")
    public <T> ActorRef<T> getOrCreateActorRef(Object address, Supplier<T> actorSupplier, Class<T> actorType) {
        return state.actors.computeIfAbsent(new ActorKey(address, actorType), a -> createAndStartActorRef(actorSupplier));
    }

    private <T> ActorRef<T> createAndStartActorRef(Supplier<T> actorSupplier) {
        ActorRef<T> actorRef = new ActorRefImpl<>(actorSupplier.get(), mailboxFactory.createMailbox(), scheduler);
        actorRef.start();
        return actorRef;
    }

    /**
     * Stops the actors and returns without waiting for them to finish. Their threads are still
     * winding down when this returns, where they compete with whatever runs next; use
     * {@link #close(Duration)} when that matters.
     */
    public void close() {
        cleanable.clean();
    }

    /**
     * Stops the actors and waits for them to finish.
     *
     * @param timeout How long to wait for all of the actors together.
     * @return true if every actor had finished within the timeout.
     * @throws InterruptedException If the waiting thread is interrupted.
     */
    public boolean close(Duration timeout) throws InterruptedException {
        close();
        return state.awaitStopped(timeout);
    }

    private record ActorKey(Object address, Class<?> actorClass) { }

    private static class State implements Runnable {
        @SuppressWarnings("rawtypes")
        private final ConcurrentMap<ActorKey, ActorRef> actors;

        State() {
            this.actors = new ConcurrentHashMap<>();
        }

        @Override
        public void run() {
            actors.values().forEach(ActorRef::stop);
        }

        /**
         * Waits for actors that {@link #run()} has already signalled. Signalling every actor before
         * waiting for any of them is what lets them wind down alongside each other, rather than one
         * at a time.
         */
        boolean awaitStopped(Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            boolean allStopped = true;
            for (ActorRef<?> actorRef : actors.values()) {
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
