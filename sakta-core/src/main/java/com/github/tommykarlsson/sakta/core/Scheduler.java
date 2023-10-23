package com.github.tommykarlsson.sakta.core;

public interface Scheduler {
    Disposable schedule(Mailbox mailbox);
}
