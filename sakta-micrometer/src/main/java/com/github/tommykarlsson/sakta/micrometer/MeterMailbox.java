package com.github.tommykarlsson.sakta.micrometer;

import java.util.List;

import com.github.tommykarlsson.sakta.core.Disposable;
import com.github.tommykarlsson.sakta.core.MailAction;
import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.Mailbox;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Provide timer on mailbox queue items.
 */
public class MeterMailbox implements Mailbox {

    private static final String METER_NAME = "sakta.actor.action.queue";

    private final Mailbox delegate;
    private final MeterRegistry meterRegistry;

    public MeterMailbox(Mailbox delegate, MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void add(MailItem item) {
        delegate.add(item.withAction(new QueueTimedAction(
                item.action(),
                meterRegistry.timer(METER_NAME, MailItemTags.of(item)),
                Timer.start())));
    }

    /**
     * Records how long the action waited before it ran, and then runs it. The sample is started as
     * the item is added, which is what makes the measurement the time spent queued.
     *
     * <p>Everything the recording needs is resolved up front, so that a queued action holds neither
     * the item it was taken from nor the registry it reports to. The timer being resolved on the
     * way in also means it is registered then, rather than when the first action of its kind runs.
     */
    private record QueueTimedAction(MailAction action, Timer timer, Timer.Sample sample) implements MailAction {

        @Override
        public void run() {
            sample.stop(timer);
            action.run();
        }

        /** An action given up on never came off the queue, so its wait is not a wait worth recording. */
        @Override
        public void discard(Throwable cause) {
            action.discard(cause);
        }
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public List<MailItem> discardQueued() {
        return delegate.discardQueued();
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
