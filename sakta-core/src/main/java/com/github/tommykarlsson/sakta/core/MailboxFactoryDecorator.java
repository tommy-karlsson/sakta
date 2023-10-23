package com.github.tommykarlsson.sakta.core;

public interface MailboxFactoryDecorator {

    MailboxFactory decorate(MailboxFactory mailboxFactory);
}
