package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.MailItemDecorator;
import com.github.tommykarlsson.sakta.core.ActorRef;
import com.github.tommykarlsson.sakta.core.Disposable;
import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.Scheduler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActorRefImpl<T> implements ActorRef<T> {

    private final T actor;
    private final Mailbox mailbox;
    private final Scheduler scheduler;
    private final List<MailItemDecorator> mailItemDecorators;
    private final Logger logger;

    private Disposable schedule;

    public ActorRefImpl(T actor, Mailbox mailbox, Scheduler scheduler, List<MailItemDecorator> mailItemDecorators) {
        this.actor = actor;
        this.mailbox = mailbox;
        this.scheduler = scheduler;
        this.mailItemDecorators = mailItemDecorators;
        this.logger = Logger.getLogger(actor.getClass().getName() + "_ActorRef");
    }

    @Override
    public void tell(Consumer<T> teller) {
        Runnable runnable = () -> {
            try {
                teller.accept(actor);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Actor tell failed", e);
            }
        };
        MailItem mailItem = new MailItem(actor.getClass(), "tell", runnable);
        for (MailItemDecorator mailItemDecorator : mailItemDecorators) {
            mailItem = mailItemDecorator.decorateItem(mailItem);
        }
        mailbox.add(mailItem);
    }

    @Override
    public <U> CompletableFuture<U> ask(Function<T, U> asker) {
        CompletableFuture<U> completion = new CompletableFuture<>();

        Runnable runnable = () -> {
            if (!completion.isCancelled()) {
                try {
                    U response = asker.apply(actor);
                    completion.complete(response);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Actor ask failed", e);
                    completion.completeExceptionally(e);
                }
            }
        };
        MailItem mailItem = new MailItem(actor.getClass(), "ask", runnable);
        for (MailItemDecorator mailItemDecorator : mailItemDecorators) {
            mailItem = mailItemDecorator.decorateItem(mailItem);
        }
        mailbox.add(mailItem);
        return completion;
    }

    @Override
    public <U> CompletableFuture<U> flatAsk(Function<T, CompletableFuture<U>> asker) {
        CompletableFuture<U> completion = new CompletableFuture<>();
        Runnable runnable = () -> {
            if (!completion.isCancelled()) {
                try {
                    CompletableFuture<U> c = asker.apply(actor);
                    c.whenComplete((v, ex) -> {
                        if (ex != null) {
                            completion.completeExceptionally(ex);
                        } else {
                            completion.complete(v);
                        }
                    });
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Actor ask failed", e);
                    completion.completeExceptionally(e);
                }
            }
        };
        MailItem mailItem = new MailItem(actor.getClass(), "ask", runnable);
        for (MailItemDecorator mailItemDecorator : mailItemDecorators) {
            mailItem = mailItemDecorator.decorateItem(mailItem);
        }
        mailbox.add(mailItem);
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
}
