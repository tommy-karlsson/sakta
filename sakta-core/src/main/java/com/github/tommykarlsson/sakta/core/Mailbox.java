package com.github.tommykarlsson.sakta.core;

public interface Mailbox {
    void add(MailItem mailItem);

    Runnable poll() throws InterruptedException;

    Disposable onAdd(Runnable r);

    boolean isEmpty();
}
