package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.Disposable;
import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.Scheduler;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;

public class ForkJoinPoolScheduler implements Scheduler {

    private final ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();

    @Override
    public Disposable schedule(Mailbox mailbox) {
        Semaphore semaphore = new Semaphore(1);
        return mailbox.onAdd(() -> forkJoinPool.submit(processAllItems(mailbox, semaphore)));
    }

    private Runnable processAllItems(Mailbox mailbox, Semaphore semaphore) {
        return () -> {
            if (semaphore.tryAcquire()) {
                try {
                    while (!mailbox.isEmpty()) {
                        try {
                            mailbox.poll().run();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } finally {
                    semaphore.release();
                }
                if (!mailbox.isEmpty()) {
                    forkJoinPool.submit(processAllItems(mailbox, semaphore));
                }
            }
        };
    }
}
