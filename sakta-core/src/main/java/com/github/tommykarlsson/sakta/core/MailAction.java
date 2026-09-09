package com.github.tommykarlsson.sakta.core;

/**
 * What a {@link MailItem} does when the actor gets to it.
 *
 * <p>An item that is thrown away instead of run is discarded rather than simply dropped, so that
 * whoever is waiting on it is told rather than left waiting. Anything that wraps an action is
 * expected to pass that on to the action it wraps.
 */
@FunctionalInterface
public interface MailAction extends Runnable {

    /**
     * Called instead of {@link #run()} when the item will never run, because the actor system was
     * shut down before the actor got to it.
     *
     * @param cause Why the item was thrown away.
     */
    default void discard(Throwable cause) {
    }
}
