package com.github.tommykarlsson.sakta.core.impl;

import java.time.Duration;

import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.Schedule;
import com.github.tommykarlsson.sakta.core.Scheduler;

public class VirtualThreadPerActorScheduler implements Scheduler {

    @Override
    public Schedule schedule(Mailbox mailbox) {
        return new ThreadSchedule(Thread.ofVirtual().start(new MailboxRunner(mailbox)));
    }

    /**
     * Runs everything queued in one mailbox, on the one thread there is per actor. The actor may
     * only be touched from that thread, which is what keeps its processing serial.
     *
     * <p>A class rather than a lambda, so that the bottom of every actor's stack trace names what
     * it is doing.
     */
    private record MailboxRunner(Mailbox mailbox) implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    mailbox.poll().run();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
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
