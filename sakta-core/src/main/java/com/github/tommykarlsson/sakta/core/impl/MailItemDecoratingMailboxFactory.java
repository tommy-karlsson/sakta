package com.github.tommykarlsson.sakta.core.impl;

import java.util.List;

import com.github.tommykarlsson.sakta.core.MailItemDecorator;
import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.MailboxFactory;

/**
 * Creates mailboxes that apply the given mail item decorators, around the mailboxes of another
 * factory.
 */
public class MailItemDecoratingMailboxFactory implements MailboxFactory {

    private final MailboxFactory delegate;
    private final List<MailItemDecorator> mailItemDecorators;

    public MailItemDecoratingMailboxFactory(MailboxFactory delegate, List<MailItemDecorator> mailItemDecorators) {
        this.delegate = delegate;
        this.mailItemDecorators = List.copyOf(mailItemDecorators);
    }

    @Override
    public Mailbox createMailbox() {
        return new MailItemDecoratingMailbox(delegate.createMailbox(), mailItemDecorators);
    }
}
