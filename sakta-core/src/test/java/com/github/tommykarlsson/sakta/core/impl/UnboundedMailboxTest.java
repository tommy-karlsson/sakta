package com.github.tommykarlsson.sakta.core.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import com.github.tommykarlsson.sakta.core.MailItem;

import com.github.tommykarlsson.sakta.core.impl.UnboundedMailbox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnboundedMailboxTest {

    private static final Runnable EMPTY_RUNNABLE = () -> {
    };
    private static final MailItem ITEM = new MailItem(Object.class, "tell", EMPTY_RUNNABLE);

    @Test
    void add() {
        UnboundedMailbox mailbox = new UnboundedMailbox();
        assertDoesNotThrow(() -> mailbox.add(ITEM));
    }

    @Test
    void poll() throws InterruptedException {
        UnboundedMailbox mailbox = new UnboundedMailbox();
        mailbox.add(ITEM);
        assertEquals(ITEM.action(), mailbox.poll());
    }

    @Test
    void onAdd() {
        UnboundedMailbox mailbox = new UnboundedMailbox();
        AtomicBoolean added = new AtomicBoolean(false);
        mailbox.onAdd(() -> added.set(true));

        assertFalse(added.get());
        mailbox.add(ITEM);
        assertTrue(added.get());
    }


    @Test
    void isEmpty() {
        UnboundedMailbox mailbox = new UnboundedMailbox();

        assertTrue(mailbox.isEmpty());
        mailbox.add(ITEM);
        assertFalse(mailbox.isEmpty());
    }
}
