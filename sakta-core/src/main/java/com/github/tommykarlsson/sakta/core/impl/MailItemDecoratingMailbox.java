package com.github.tommykarlsson.sakta.core.impl;

import java.util.List;

import com.github.tommykarlsson.sakta.core.Disposable;
import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.MailItemDecorator;
import com.github.tommykarlsson.sakta.core.Mailbox;

/**
 * Applies the mail item decorators to everything added to the mailbox it wraps.
 *
 * <p>Decorating where the items arrive rather than where they are sent means a sender cannot
 * forget to do it, and it puts mail item decorators in the same place as the decorators that wrap
 * the mailbox itself, which is where the order between the two becomes visible.
 */
public class MailItemDecoratingMailbox implements Mailbox {

    private final Mailbox delegate;
    private final List<MailItemDecorator> mailItemDecorators;

    public MailItemDecoratingMailbox(Mailbox delegate, List<MailItemDecorator> mailItemDecorators) {
        this.delegate = delegate;
        this.mailItemDecorators = List.copyOf(mailItemDecorators);
    }

    @Override
    public void add(MailItem mailItem) {
        MailItem decorated = mailItem;
        for (MailItemDecorator mailItemDecorator : mailItemDecorators) {
            decorated = mailItemDecorator.decorateItem(decorated);
        }
        delegate.add(decorated);
    }

    @Override
    public Runnable poll() throws InterruptedException {
        return delegate.poll();
    }

    @Override
    public Disposable onAdd(Runnable r) {
        return delegate.onAdd(r);
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }
}
