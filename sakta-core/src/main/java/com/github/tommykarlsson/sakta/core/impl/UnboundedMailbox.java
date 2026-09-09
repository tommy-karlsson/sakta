package com.github.tommykarlsson.sakta.core.impl;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.RejectedExecutionException;

import com.github.tommykarlsson.sakta.core.MailItem;

public class UnboundedMailbox extends AbstractBlockingQueueMailbox {

    public UnboundedMailbox() {
        super(new LinkedTransferQueue<>());
    }

    @Override
    public void add(MailItem item) {
        if (closed) {
            throw new RejectedExecutionException("Mailbox is closed");
        }
        boolean success = this.queue.add(item);
        if (!success) {
            throw new IllegalStateException("Mailbox is full");
        } else {
            notifyItemAdded();
        }
    }
}
