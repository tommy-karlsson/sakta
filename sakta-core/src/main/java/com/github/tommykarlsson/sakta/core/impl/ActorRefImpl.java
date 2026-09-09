package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.ActorRef;
import com.github.tommykarlsson.sakta.core.MailAction;
import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.Schedule;
import com.github.tommykarlsson.sakta.core.Scheduler;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActorRefImpl<T> implements ActorRef<T> {

    private final T actor;
    private final Mailbox mailbox;
    private final Scheduler scheduler;
    private final Logger logger;

    private Schedule schedule;

    public ActorRefImpl(T actor, Mailbox mailbox, Scheduler scheduler) {
        this.actor = actor;
        this.mailbox = mailbox;
        this.scheduler = scheduler;
        this.logger = Logger.getLogger(actor.getClass().getName() + "_ActorRef");
    }

    @Override
    public void tell(Consumer<T> teller) {
        mailbox.add(new MailItem(actor.getClass(), "tell", new TellAction(teller)));
    }

    @Override
    public <U> CompletableFuture<U> ask(Function<T, U> asker) {
        CompletableFuture<U> completion = new CompletableFuture<>();
        mailbox.add(new MailItem(actor.getClass(), "ask", new AskAction<>(asker, completion)));
        return completion;
    }

    @Override
    public <U> CompletableFuture<U> flatAsk(Function<T, CompletableFuture<U>> asker) {
        CompletableFuture<U> completion = new CompletableFuture<>();
        mailbox.add(new MailItem(actor.getClass(), "ask", new FlatAskAction<>(asker, completion)));
        return completion;
    }

    @Override
    public void start() {
        schedule = scheduler.schedule(mailbox);
    }

    @Override
    public void stop() {
        schedule.dispose();
    }

    @Override
    public boolean awaitStopped(Duration timeout) throws InterruptedException {
        return schedule.awaitStopped(timeout);
    }

    /*
     * What a mailbox item runs is one of the actions below rather than a lambda, so that the frame
     * between the scheduler and the actor's own method says which kind of send it came from.
     */

    private final class TellAction implements MailAction {

        private final Consumer<T> teller;

        TellAction(Consumer<T> teller) {
            this.teller = teller;
        }

        @Override
        public void run() {
            try {
                teller.accept(actor);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Actor tell failed", e);
            }
        }
    }

    private final class AskAction<U> implements MailAction {

        private final Function<T, U> asker;
        private final CompletableFuture<U> completion;

        AskAction(Function<T, U> asker, CompletableFuture<U> completion) {
            this.asker = asker;
            this.completion = completion;
        }

        @Override
        public void run() {
            if (completion.isCancelled()) {
                return;
            }
            try {
                U response = asker.apply(actor);
                completion.complete(response);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Actor ask failed", e);
                completion.completeExceptionally(e);
            }
        }

        @Override
        public void discard(Throwable cause) {
            completion.completeExceptionally(cause);
        }
    }

    private final class FlatAskAction<U> implements MailAction {

        private final Function<T, CompletableFuture<U>> asker;
        private final CompletableFuture<U> completion;

        FlatAskAction(Function<T, CompletableFuture<U>> asker, CompletableFuture<U> completion) {
            this.asker = asker;
            this.completion = completion;
        }

        @Override
        public void run() {
            if (completion.isCancelled()) {
                return;
            }
            try {
                CompletableFuture<U> c = asker.apply(actor);
                c.whenComplete(new PassOnOutcome<>(completion));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Actor ask failed", e);
                completion.completeExceptionally(e);
            }
        }

        @Override
        public void discard(Throwable cause) {
            completion.completeExceptionally(cause);
        }
    }

    /** Passes the outcome of the future the actor returned on to the one the caller was given. */
    private record PassOnOutcome<U>(CompletableFuture<U> completion) implements BiConsumer<U, Throwable> {

        @Override
        public void accept(U value, Throwable error) {
            if (error != null) {
                completion.completeExceptionally(error);
            } else {
                completion.complete(value);
            }
        }
    }
}
