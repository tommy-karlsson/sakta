package com.github.tommykarlsson.sakta.micrometer;

import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.MailboxFactory;

import io.micrometer.core.instrument.MeterRegistry;

public class MeterMailboxFactory implements MailboxFactory {

    private final MeterRegistry meterRegistry;
    private final MailboxFactory delegate;

    public MeterMailboxFactory(MeterRegistry meterRegistry, MailboxFactory delegate) {
        this.meterRegistry = meterRegistry;
        this.delegate = delegate;
    }

    @Override
    public Mailbox createMailbox() {
        return new MeterMailboxDecorator(delegate.createMailbox(), meterRegistry);
    }
}
