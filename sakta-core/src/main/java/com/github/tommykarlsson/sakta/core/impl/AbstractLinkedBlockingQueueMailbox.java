package com.github.tommykarlsson.sakta.core.impl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import com.github.tommykarlsson.sakta.core.Disposable;
import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.Mailbox;

public abstract class AbstractLinkedBlockingQueueMailbox implements Mailbox {

    protected final LinkedBlockingQueue<MailItem> queue;
    protected final List<Runnable> onAddListeners = new CopyOnWriteArrayList<>();

    public AbstractLinkedBlockingQueueMailbox(LinkedBlockingQueue<MailItem> queue) {
        this.queue = queue;
    }

    @Override
    public Runnable poll() throws InterruptedException {
        MailItem item = this.queue.take();
        return item.action();
    }

    @Override
    public Disposable onAdd(Runnable r) {
        onAddListeners.add(r);
        return () -> onAddListeners.remove(r);
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
