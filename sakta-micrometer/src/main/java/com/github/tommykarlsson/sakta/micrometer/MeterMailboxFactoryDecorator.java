package com.github.tommykarlsson.sakta.micrometer;

import com.github.tommykarlsson.sakta.core.MailboxFactory;
import com.github.tommykarlsson.sakta.core.MailboxFactoryDecorator;

import io.micrometer.core.instrument.MeterRegistry;

public class MeterMailboxFactoryDecorator implements MailboxFactoryDecorator {

    private final MeterRegistry meterRegistry;

    public MeterMailboxFactoryDecorator(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public MailboxFactory decorate(MailboxFactory mailboxFactory) {
        return new MeterMailboxFactory(meterRegistry, mailboxFactory);
    }
}
