package com.github.tommykarlsson.sakta.micrometer;

import com.github.tommykarlsson.sakta.core.Disposable;
import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.Mailbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Provide timer on mailbox queue items.
 */
public class MeterMailboxDecorator implements Mailbox {

    private static final String METER_NAME = "sakta.actor.action.queue";

    private final Mailbox delegate;
    private final MeterRegistry meterRegistry;

    public MeterMailboxDecorator(Mailbox delegate, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void add(MailItem item) {
        Timer.Sample sample = Timer.start();

        delegate.add(item.withAction(() -> {
            sample.stop(meterRegistry.timer(METER_NAME,
                    "actor.type", item.actorType().getSimpleName(),
                    "action.type", item.actionType(),
                    "action.name", item.actionName()
            ));
            item.action().run();
        }));
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
