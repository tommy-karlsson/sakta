package com.github.tommykarlsson.sakta.core.impl;

import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.MailboxFactory;

public class UnboundedMailboxFactory implements MailboxFactory {

    @Override
    public Mailbox createMailbox() {
        return new UnboundedMailbox();
    }
}
