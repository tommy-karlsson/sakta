package com.github.tommykarlsson.sakta.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;

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

    private volatile boolean closed;

    /**
     * Null until somebody actually listens, which for a scheduler that gives each actor a thread of
     * its own is never. A list per mailbox is not much until there are a million mailboxes.
     */
    private volatile List<Runnable> onAddListeners;

    public AbstractBlockingQueueMailbox(BlockingQueue<MailItem> queue) {
        this.queue = queue;
    }

    /**
     * An add that finds the mailbox open may still be on its way into the queue when the mailbox
     * closes, and land behind the marker where nobody will look. So it looks again once the item
     * is in. Still open means the item went in ahead of the marker, since closing sets the flag
     * before queueing the marker. Closed means it may not have, and the add takes the item back
     * and is turned away. If the item is already gone, it was either run or discarded, and
     * whoever is waiting on it has been told either way.
     *
     * <p>Checking again costs every add one read of a flag that hardly ever changes. Taking the
     * item back costs more, but only an add that races the close pays it.
     */
    @Override
    public final void add(MailItem item) {
        if (closed) {
            throw new RejectedExecutionException("Mailbox is closed");
        }
        enqueue(item);
        if (closed && queue.remove(item)) {
            /*
             * Whoever is processing the mailbox may have been on its way to wait for that item,
             * and a full queue turns the marker away when closing. Taking an item back makes room
             * for one, and a marker too many does no harm.
             */
            queue.offer(CLOSED);
            throw new RejectedExecutionException("Mailbox is closed");
        }
        notifyItemAdded();
    }

    /** Puts the item in the queue, by whatever rules the mailbox has for a queue that is full. */
    protected abstract void enqueue(MailItem item);

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
    private void notifyItemAdded() {
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
        queued.removeIf(item -> item == CLOSED);
        return queued;
    }
}
