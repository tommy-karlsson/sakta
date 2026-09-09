package com.github.tommykarlsson.sakta.core;

public interface Scheduler {

    /**
     * Arranges for the mailbox to be drained.
     *
     * @param mailbox The mailbox to process.
     * @return The handle used to stop the processing, and to wait for it to finish.
     */
    Schedule schedule(Mailbox mailbox);
}
