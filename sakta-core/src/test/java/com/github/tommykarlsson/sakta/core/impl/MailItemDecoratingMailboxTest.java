package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.ActorRef;
import com.github.tommykarlsson.sakta.core.ActorSystem;
import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.MailItemDecorator;
import com.github.tommykarlsson.sakta.core.Mailbox;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MailItemDecoratingMailboxTest {

    @Test
    void appliesTheDecoratorsInOrder() {
        List<String> applied = new ArrayList<>();
        Mailbox mailbox = new MailItemDecoratingMailbox(new UnboundedMailbox(), List.of(
                recording(applied, "first"),
                recording(applied, "second")));

        mailbox.add(new MailItem(Object.class, "tell", () -> { }));

        assertEquals(List.of("first", "second"), applied);
    }

    @Test
    void theDelegateGetsTheDecoratedItem() throws InterruptedException {
        Mailbox delegate = new UnboundedMailbox();
        AtomicInteger runs = new AtomicInteger();
        Mailbox mailbox = new MailItemDecoratingMailbox(delegate, List.of(
                item -> item.withAction(() -> {
                    runs.incrementAndGet();
                    item.action().run();
                })));

        mailbox.add(new MailItem(Object.class, "tell", () -> { }));
        delegate.poll().run();

        assertEquals(1, runs.get(), "the action taken off the delegate should be the decorated one");
    }

    /** Every way of sending goes through the mailbox, so every way of sending is decorated. */
    @Test
    void decoratesWhateverTellAskAndFlatAskPutInTheMailbox() throws ExecutionException, InterruptedException {
        AtomicInteger decorated = new AtomicInteger();
        MailItemDecorator counting = item -> {
            decorated.incrementAndGet();
            return item;
        };

        ActorSystem actorSystem = new ActorSystem(
                new MailItemDecoratingMailboxFactory(new UnboundedMailboxFactory(), List.of(counting)),
                new VirtualThreadPerActorScheduler());
        try {
            ActorRef<Actor> ref = actorSystem.getOrCreateActorRef("actor", Actor::new, Actor.class);
            ref.tell(Actor::noop);
            ref.ask(Actor::answer).get();
            ref.flatAsk(Actor::answerLater).get();

            assertEquals(3, decorated.get());
        } finally {
            actorSystem.shutdownNow();
        }
    }

    @Test
    void anEmptyDecoratorListLeavesTheItemAlone() throws InterruptedException {
        Mailbox delegate = new UnboundedMailbox();
        Mailbox mailbox = new MailItemDecoratingMailbox(delegate, List.of());
        MailItem item = new MailItem(Object.class, "tell", () -> { });

        mailbox.add(item);

        assertSame(item.action(), delegate.poll());
    }

    private static MailItemDecorator recording(List<String> applied, String name) {
        return item -> {
            applied.add(name);
            return item;
        };
    }

    private static class Actor {
        void noop() {
        }

        int answer() {
            return 42;
        }

        CompletableFuture<Integer> answerLater() {
            return CompletableFuture.completedFuture(42);
        }
    }
}
