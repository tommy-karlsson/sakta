package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.Disposable;
import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.Schedule;
import com.github.tommykarlsson.sakta.core.Scheduler;

import java.time.Duration;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

public class ForkJoinPoolScheduler implements Scheduler {

    /** How often {@link Schedule#awaitStopped(Duration)} looks at a drain it is waiting out. */
    private static final Duration POLL_INTERVAL = Duration.ofMillis(1);

    private final ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

    @Override
    public Schedule schedule(Mailbox mailbox) {
        MailboxDrain drain = new MailboxDrain(mailbox, forkJoinPool);
        return new DrainSchedule(mailbox.onAdd(drain::scheduleIfIdle), drain);
    }

    /**
     * Stops feeding a mailbox to the pool, and waits out the drain that may still be running.
     *
     * <p>The wait polls rather than being signalled, to keep the drain's own bookkeeping free of
     * anything the sending path would have to pay for.
     */
    private record DrainSchedule(Disposable listener, MailboxDrain drain) implements Schedule {

        @Override
        public void dispose() {
            listener.dispose();
        }

        @Override
        public boolean awaitStopped(Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (drain.isRunning()) {
                if (System.nanoTime() >= deadline) {
                    return false;
                }
                Thread.sleep(POLL_INTERVAL);
            }
            return true;
        }
    }

    /**
     * Drains one mailbox on the pool. At most one drain per mailbox is queued or running at any
     * time, which is what keeps the actor single threaded. It also means an add only costs a pool
     * submission when no drain is already there to pick the item up: a sender that finds a drain
     * in flight can leave the item in the mailbox and move on.
     */
    private static final class MailboxDrain implements Runnable {

        private final Mailbox mailbox;
        private final ForkJoinPool forkJoinPool;

        /** True while a drain is queued or running. Doubles as the mutex between drains. */
        private final AtomicBoolean scheduled = new AtomicBoolean();

        MailboxDrain(Mailbox mailbox, ForkJoinPool forkJoinPool) {
            this.mailbox = mailbox;
            this.forkJoinPool = forkJoinPool;
        }

        void scheduleIfIdle() {
            if (scheduled.compareAndSet(false, true)) {
                forkJoinPool.submit(this);
            }
        }

        boolean isRunning() {
            return scheduled.get();
        }

        @Override
        public void run() {
            try {
                while (!mailbox.isEmpty()) {
                    try {
                        mailbox.poll().run();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            } finally {
                scheduled.set(false);
            }

            /*
             * An item added while this drain was on its way out still needs a drain: the add saw
             * one already scheduled and skipped its own submission. Checking after clearing the
             * flag is what makes that safe, since an add that lands after the clear submits itself
             * and only one of the two can win the flag.
             */
            if (!mailbox.isEmpty()) {
                scheduleIfIdle();
            }
        }
    }
}
