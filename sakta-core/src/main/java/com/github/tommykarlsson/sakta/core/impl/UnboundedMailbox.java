package com.github.tommykarlsson.sakta.core.impl;

import java.util.concurrent.LinkedBlockingQueue;

import com.github.tommykarlsson.sakta.core.MailItem;

public class UnboundedMailbox extends AbstractLinkedBlockingQueueMailbox {

    public UnboundedMailbox() {
        super(new LinkedBlockingQueue<>());
    }

    @Override
    public void add(MailItem item) {
        boolean success = this.queue.add(item);
        if (!success) {
            throw new IllegalStateException("Mailbox is full");
        } else {
            onAddListeners.forEach(Runnable::run);
        }
    }
}
