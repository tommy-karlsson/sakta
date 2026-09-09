package com.github.tommykarlsson.sakta.micrometer;

import com.github.tommykarlsson.sakta.core.MailItem;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MeterActionRunMailItemDecoratorTest {

    private static final String METER_NAME = "sakta.actor.action";

    private SimpleMeterRegistry meterRegistry;
    private MeterActionRunMailItemDecorator decorator;

    @BeforeEach
    void beforeEach() {
        this.meterRegistry = new SimpleMeterRegistry();
        this.decorator = new MeterActionRunMailItemDecorator(meterRegistry);
    }

    @Test
    void runsTheActionItWasGiven() {
        AtomicInteger runs = new AtomicInteger();

        decorator.decorateItem(itemRunning(runs::incrementAndGet)).action().run();

        assertEquals(1, runs.get());
    }

    @Test
    void keepsWhatTheItemSaysAboutItself() {
        MailItem decorated = decorator.decorateItem(itemRunning(() -> { }));

        assertEquals(Actor.class, decorated.actorType());
        assertEquals("ask", decorated.actionType());
        assertEquals("divide", decorated.actionName());
    }

    @Test
    void timesAnActionThatReturns() {
        decorator.decorateItem(itemRunning(() -> { })).action().run();

        Timer timer = meterRegistry.get(METER_NAME)
                .tag("actor.type", "Actor")
                .tag("action.type", "ask")
                .tag("action.name", "divide")
                .tag("outcome", "success")
                .timer();
        assertEquals(1, timer.count());
    }

    @Test
    void timesAnActionThatThrowsAndLetsItThrough() {
        RuntimeException failure = new IllegalStateException("no");
        Runnable action = decorator.decorateItem(itemRunning(() -> {
            throw failure;
        })).action();

        RuntimeException thrown = assertThrows(IllegalStateException.class, action::run);

        assertSame(failure, thrown, "the decorator should not swallow or replace the failure");
        assertEquals(1, meterRegistry.get(METER_NAME).tag("outcome", "failed").timer().count());
    }

    /** Nothing is recorded until the action is actually run, since the timing is of the run. */
    @Test
    void recordsNothingBeforeTheActionRuns() {
        decorator.decorateItem(itemRunning(() -> { }));

        assertTrue(meterRegistry.find(METER_NAME).timers().isEmpty());
    }

    private static MailItem itemRunning(Runnable action) {
        return new MailItem(Actor.class, "ask", "divide", action);
    }

    private static class Actor {
    }
}
