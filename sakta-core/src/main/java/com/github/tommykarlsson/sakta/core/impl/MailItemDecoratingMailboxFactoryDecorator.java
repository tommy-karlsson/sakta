package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.MailItemDecorator;
import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.MailboxFactory;
import com.github.tommykarlsson.sakta.core.MailboxFactoryDecorator;

import java.util.List;

/**
 * Decorates a factory so that it creates mailboxes that applies {@link MailItemDecorator}s.
 */
public class MailItemDecoratingMailboxFactoryDecorator implements MailboxFactoryDecorator {

    private final List<MailItemDecorator> mailItemDecorators;

    public MailItemDecoratingMailboxFactoryDecorator(List<MailItemDecorator> mailItemDecorators) {
        this.mailItemDecorators = List.copyOf(mailItemDecorators);
    }

    @Override
    public MailboxFactory decorate(MailboxFactory mailboxFactory) {
        if (mailboxFactory instanceof MailItemDecoratingMailboxFactory) {
            return mailboxFactory;
        }
        return new MailItemDecoratingMailboxFactory(mailboxFactory, mailItemDecorators);
    }
}
