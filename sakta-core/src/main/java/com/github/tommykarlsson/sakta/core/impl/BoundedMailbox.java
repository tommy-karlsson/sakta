package com.github.tommykarlsson.sakta.core.impl;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.github.tommykarlsson.sakta.core.MailItem;

public class BoundedMailbox extends AbstractLinkedBlockingQueueMailbox {

    private final Duration addTimeout;

    public BoundedMailbox(int capacity, Duration addTimeout) {
        super(new LinkedBlockingQueue<>(capacity));
        this.addTimeout = addTimeout;
    }

    @Override
    public void add(MailItem item) {
        try {
            boolean success = this.queue.offer(item, addTimeout.toNanos(), TimeUnit.NANOSECONDS);
            if (!success) {
                throw new IllegalStateException("Mailbox is full");
            } else {
                onAddListeners.forEach(Runnable::run);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Add interrupted", e);
        }
    }
}
