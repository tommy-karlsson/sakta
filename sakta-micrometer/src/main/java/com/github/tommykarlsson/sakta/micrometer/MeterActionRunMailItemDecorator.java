package com.github.tommykarlsson.sakta.micrometer;

import com.github.tommykarlsson.sakta.core.MailAction;
import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.MailItemDecorator;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

/**
 * Provide timer on action execution.
 */
public class MeterActionRunMailItemDecorator implements MailItemDecorator {

    private static final String METER_NAME = "sakta.actor.action";

    private final MeterRegistry meterRegistry;

    public MeterActionRunMailItemDecorator(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public MailItem decorateItem(MailItem item) {
        return item.withAction(new TimedAction(item.action(), meterRegistry, MailItemTags.of(item)));
    }

    /**
     * Runs the action, recording how long it took and whether it threw. It holds the action rather
     * than the item it was taken from, so that decorating an item does not keep the undecorated
     * one alive for as long as the decorated one is queued.
     */
    private record TimedAction(MailAction action, MeterRegistry meterRegistry, Tags tags) implements MailAction {

        /** An action that never ran took no time, so there is nothing to record, only to pass on. */
        @Override
        public void discard(Throwable cause) {
            action.discard(cause);
        }

        @Override
        public void run() {
            Timer.Sample sample = Timer.start();
            String outcome = "success";
            try {
                action.run();
            } catch (Error | RuntimeException e) {
                outcome = "failed";
                throw e;
            } finally {
                sample.stop(meterRegistry.timer(METER_NAME, tags.and("outcome", outcome)));
            }
        }
    }
}
