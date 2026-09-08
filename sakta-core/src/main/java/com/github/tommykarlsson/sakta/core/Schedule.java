package com.github.tommykarlsson.sakta.core;

import java.time.Duration;

/**
 * The handle to an actor's scheduled processing, returned by {@link Scheduler#schedule(Mailbox)}.
 *
 * <p>Disposing the handle only signals the processing to stop. Use {@link #awaitStopped(Duration)}
 * when it matters that the processing has actually finished, for instance to keep a stopped actor
 * from competing with whatever runs next.
 */
public interface Schedule extends Disposable {

    /**
     * Waits for the processing to finish. Only meaningful once {@link #dispose()} has been called,
     * as nothing else asks the processing to stop.
     *
     * @param timeout How long to wait.
     * @return true if the processing had finished within the timeout.
     * @throws InterruptedException If the waiting thread is interrupted.
     */
    boolean awaitStopped(Duration timeout) throws InterruptedException;
}
