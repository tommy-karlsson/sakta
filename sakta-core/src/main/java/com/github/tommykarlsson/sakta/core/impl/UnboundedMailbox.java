package com.github.tommykarlsson.sakta.core.impl;

import java.util.concurrent.LinkedTransferQueue;

import com.github.tommykarlsson.sakta.core.MailItem;

public class UnboundedMailbox extends AbstractBlockingQueueMailbox {

    public UnboundedMailbox() {
        super(new LinkedTransferQueue<>());
    }

    @Override
    protected void enqueue(MailItem item) {
        this.queue.add(item);
    }
}
