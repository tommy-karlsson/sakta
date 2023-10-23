package com.github.tommykarlsson.sakta.core;

import com.github.tommykarlsson.sakta.core.impl.ActorRefImpl;
import com.github.tommykarlsson.sakta.core.impl.UnboundedMailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.VirtualThreadPerActorScheduler;

import java.lang.ref.Cleaner;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public class ActorSystem implements AutoCloseable {

    private final MailboxFactory mailboxFactory;
    private final Scheduler scheduler;
    private final List<MailItemDecorator> mailItemDecorators;
    private final State state;
    private final Cleaner.Cleanable cleanable;

    public ActorSystem() {
        this(new UnboundedMailboxFactory());
    }

    public ActorSystem(MailboxFactory mailboxFactory) {
        this(mailboxFactory, new VirtualThreadPerActorScheduler());
    }

    public ActorSystem(MailboxFactory mailboxFactory, Scheduler scheduler) {
        this(mailboxFactory, scheduler, Collections.emptyList());
    }

    public ActorSystem(MailboxFactory mailboxFactory, Scheduler scheduler, List<MailItemDecorator> mailItemDecorators) {
        this.mailboxFactory = mailboxFactory;
        this.scheduler = scheduler;
        this.mailItemDecorators = mailItemDecorators;
        this.state = new State();
        this.cleanable = Cleaner.create().register(this, state);
    }

    @SuppressWarnings("unchecked")
    public <T> ActorRef<T> getOrCreateActorRef(Object address, Supplier<T> actorSupplier, Class<T> actorType) {
        return state.actors.computeIfAbsent(new ActorKey(address, actorType), a -> createAndStartActorRef(actorSupplier));
    }

    private <T> ActorRef<T> createAndStartActorRef(Supplier<T> actorSupplier) {
        ActorRef<T> actorRef = new ActorRefImpl<>(actorSupplier.get(), mailboxFactory.createMailbox(), scheduler, mailItemDecorators);
        actorRef.start();
        return actorRef;
    }

    public void close() {
        cleanable.clean();
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
    }
}
