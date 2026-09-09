package com.github.tommykarlsson.sakta.core;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

public interface Mailbox {

    /**
     * @throws RejectedExecutionException If the mailbox has been closed.
     */
    void add(MailItem mailItem);

    /**
     * Takes the next action, waiting for one to arrive.
     *
     * @return The next action, or null once the mailbox has been closed and everything left in it
     *         has been taken. A null is how whoever is processing the mailbox knows to stop.
     */
    Runnable poll() throws InterruptedException;

    Disposable onAdd(Runnable r);

    boolean isEmpty();

    /**
     * Stops the mailbox accepting anything further. What is already in it is still there to be
     * taken, and whoever is processing it is woken so that they can finish it and stop.
     */
    void close();

    /**
     * Takes everything still waiting, leaving the mailbox empty. For giving up on a mailbox rather
     * than finishing it, so the caller can tell whoever was waiting on those items.
     *
     * @return The items that were still waiting, in the order they would have run.
     */
    List<MailItem> discardQueued();
}
