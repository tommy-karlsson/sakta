package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.impl.BoundedMailbox;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import com.github.tommykarlsson.sakta.core.MailItem;

import static org.junit.jupiter.api.Assertions.*;

class BoundedMailboxTest {

    private static final Runnable EMPTY_RUNNABLE = () -> {
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
}
