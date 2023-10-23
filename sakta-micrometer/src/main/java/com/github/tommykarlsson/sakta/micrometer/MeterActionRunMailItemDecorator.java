package com.github.tommykarlsson.sakta.micrometer;

import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.MailItemDecorator;

import io.micrometer.core.instrument.MeterRegistry;
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
        return item.withAction(() -> {
            Timer.Sample sample = Timer.start();
            String outcome = "success";
            try {
                item.action().run();
            } catch (Error | RuntimeException e) {
                outcome = "failed";
                throw e;
            } finally {
                sample.stop(meterRegistry.timer(METER_NAME,
                        "actor.type", item.actorType().getSimpleName(),
                        "action.type", item.actionType(),
                        "action.name", item.actionName(),
                        "outcome", outcome
                ));
            }
        });
    }
}
