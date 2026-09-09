package com.github.tommykarlsson.sakta.core.impl;

import java.time.Duration;

import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.Schedule;
import com.github.tommykarlsson.sakta.core.Scheduler;

public class VirtualThreadPerActorScheduler implements Scheduler {

    @Override
    public Schedule schedule(Mailbox mailbox) {

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
        return new ThreadSchedule(thread);
    }

    private record ThreadSchedule(Thread thread) implements Schedule {

        @Override
        public void dispose() {
            thread.interrupt();
        }

        @Override
        public boolean awaitStopped(Duration timeout) throws InterruptedException {
            return thread.join(timeout);
        }
    }
}
