package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.Disposable;
import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.Scheduler;

public class VirtualThreadPerActorScheduler implements Scheduler {

    @Override
    public Disposable schedule(Mailbox mailbox) {

        /*
         * This is the thread that executes all the actions queued in the mailbox. There is one of these threads per actor.
         * The actor may only be accessed from this thread (this is the key to serial single-threaded processing in the actor).
         */
        Thread thread = Thread.ofVirtual().start(() -> {
            while (true) {
                try {
                    mailbox.poll().run();
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        return thread::interrupt;
    }
}
