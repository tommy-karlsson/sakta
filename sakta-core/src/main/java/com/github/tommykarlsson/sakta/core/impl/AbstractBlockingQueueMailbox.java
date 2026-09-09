package com.github.tommykarlsson.sakta.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.tommykarlsson.sakta.core.Disposable;
import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.Mailbox;

public abstract class AbstractBlockingQueueMailbox implements Mailbox {

    /**
     * Put in the queue when the mailbox is closed, so that whoever is waiting on the queue wakes up
     * and finds out, having first taken everything that was queued ahead of it.
     */
    private static final MailItem CLOSED = new MailItem(Void.class, "close", "close", () -> { });

    protected final BlockingQueue<MailItem> queue;

    protected volatile boolean closed;

    /**
     * Null until somebody actually listens, which for a scheduler that gives each actor a thread of
     * its own is never. A list per mailbox is not much until there are a million mailboxes.
     */
    private volatile List<Runnable> onAddListeners;

    public AbstractBlockingQueueMailbox(BlockingQueue<MailItem> queue) {
        this.queue = queue;
    }

    @Override
    public Runnable poll() throws InterruptedException {
        if (closed && queue.isEmpty()) {
            return null;
        }
        MailItem item = this.queue.take();
        if (item == CLOSED) {
            return null;
        }
        return item.action();
    }

    @Override
    public Disposable onAdd(Runnable r) {
        List<Runnable> listeners = listeners();
        listeners.add(r);
        return () -> listeners.remove(r);
    }

    /** Tells whoever is listening that there is something to come for. */
    protected void notifyItemAdded() {
        List<Runnable> listeners = onAddListeners;
        if (listeners != null) {
            listeners.forEach(Runnable::run);
        }
    }

    private synchronized List<Runnable> listeners() {
        if (onAddListeners == null) {
            onAddListeners = new CopyOnWriteArrayList<>();
        }
        return onAddListeners;
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * The marker is all it takes to stop whoever is processing this mailbox. A scheduler that gives
     * an actor a thread of its own is waiting on the queue and will take the marker; one that only
     * processes a mailbox when something arrives is either already processing this one, if it has
     * anything in it, or has nothing to come back for.
     *
     * <p>Which is why the listeners are not told: telling them would ask for a mailbox to be
     * processed once per mailbox, and there can be an awful lot of mailboxes.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        queue.offer(CLOSED);
    }

    /**
     * Nothing is allocated for a mailbox with nothing in it, which is the usual case and, when
     * there are a great many mailboxes, the difference between a list each and no lists at all.
     */
    @Override
    public List<MailItem> discardQueued() {
        if (queue.isEmpty()) {
            return List.of();
        }
        List<MailItem> queued = new ArrayList<>();
        queue.drainTo(queued);
        queued.remove(CLOSED);
        return queued;
    }
}
