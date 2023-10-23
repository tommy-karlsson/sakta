package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.MailboxFactory;

import java.time.Duration;

public class BoundedMailboxFactory implements MailboxFactory {

    private final Duration addTimeout;
    private final int capacity;

    public BoundedMailboxFactory() {
        this(Duration.ofMillis(300), 3);
    }

    public BoundedMailboxFactory(Duration addTimeout, int capacity) {
        this.addTimeout = addTimeout;
        this.capacity = capacity;
    }

    @Override
    public Mailbox createMailbox() {
        return new BoundedMailbox(capacity, addTimeout);
    }
}
