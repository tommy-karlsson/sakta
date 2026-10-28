package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.impl.BoundedMailbox;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.tommykarlsson.sakta.core.MailAction;
import com.github.tommykarlsson.sakta.core.MailItem;

import static org.junit.jupiter.api.Assertions.*;

class BoundedMailboxTest {

    private static final MailAction EMPTY_RUNNABLE = () -> {
    };
    private static final MailItem ITEM = new MailItem(Object.class, "tell", EMPTY_RUNNABLE);

    @Test
    void add() {
        BoundedMailbox mailbox = new BoundedMailbox(2, Duration.ofSeconds(1));
        assertDoesNotThrow(() -> mailbox.add(ITEM));
    }

    @Test
    void addFull() {
        BoundedMailbox mailbox = new BoundedMailbox(2, Duration.ofSeconds(1));
        mailbox.add(ITEM);
        mailbox.add(ITEM);
        assertThrows(IllegalStateException.class, () -> mailbox.add(ITEM));
    }

    @Test
    void poll() throws InterruptedException {
        BoundedMailbox mailbox = new BoundedMailbox(2, Duration.ofSeconds(1));
        mailbox.add(ITEM);
        assertEquals(ITEM.action(), mailbox.poll());
    }

    /**
     * An add waiting for room when the mailbox closes may end up either way, but the sender has to
     * be told which: an add that returns was delivered, and one that was not delivered throws.
     */
    @Test
    void anAddWaitingForRoomWhenTheMailboxClosesIsDeliveredOrRejected() throws Exception {
        BoundedMailbox mailbox = new BoundedMailbox(1, Duration.ofSeconds(10));
        MailItem first = new MailItem(Object.class, "tell", () -> { });
        MailItem second = new MailItem(Object.class, "tell", () -> { });
        mailbox.add(first);
        AtomicBoolean rejected = new AtomicBoolean();
        Thread sender = Thread.ofVirtual().start(() -> {
            try {
                mailbox.add(second);
            } catch (RejectedExecutionException e) {
                rejected.set(true);
            }
        });
        while (sender.getState() != Thread.State.TIMED_WAITING) {
            Thread.onSpinWait();
        }

        mailbox.close();

        List<Runnable> polled = new ArrayList<>();
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            for (Runnable action = mailbox.poll(); action != null; action = mailbox.poll()) {
                polled.add(action);
            }
        });
        sender.join();

        List<Runnable> expected = rejected.get()
                ? List.of(first.action())
                : List.of(first.action(), second.action());
        assertEquals(expected, polled);
    }
}
