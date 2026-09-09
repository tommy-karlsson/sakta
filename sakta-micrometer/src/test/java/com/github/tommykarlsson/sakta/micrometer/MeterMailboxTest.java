package com.github.tommykarlsson.sakta.micrometer;

import com.github.tommykarlsson.sakta.core.MailAction;
import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.impl.UnboundedMailbox;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MeterMailboxTest {

    private static final String METER_NAME = "sakta.actor.action.queue";

    private Mailbox delegate;
    private SimpleMeterRegistry meterRegistry;
    private MeterMailbox mailbox;

    @BeforeEach
    void beforeEach() {
        this.delegate = new UnboundedMailbox();
        this.meterRegistry = new SimpleMeterRegistry();
        this.mailbox = new MeterMailbox(delegate, meterRegistry);
    }

    @Test
    void passesTheItemToTheMailboxItWraps() {
        mailbox.add(itemRunning(() -> { }));

        assertFalse(delegate.isEmpty());
    }

    @Test
    void runsTheActionItWasGiven() throws InterruptedException {
        AtomicInteger runs = new AtomicInteger();

        mailbox.add(itemRunning(runs::incrementAndGet));
        delegate.poll().run();

        assertEquals(1, runs.get());
    }

    @Test
    void recordsTheWaitWhenTheActionRuns() throws InterruptedException {
        mailbox.add(itemRunning(() -> { }));
        delegate.poll().run();

        Timer timer = meterRegistry.get(METER_NAME)
                .tag("actor.type", "Actor")
                .tag("action.type", "tell")
                .tag("action.name", "increment")
                .timer();
        assertEquals(1, timer.count());
    }

    /**
     * The timer is resolved as the item is added, so it exists before anything has run. What must
     * not happen is a recording, since nothing has waited yet.
     */
    @Test
    void recordsNothingUntilTheActionRuns() {
        mailbox.add(itemRunning(() -> { }));

        assertEquals(0, meterRegistry.get(METER_NAME).timer().count());
    }

    @Test
    void delegatesPollAndIsEmptyAndOnAdd() throws InterruptedException {
        AtomicInteger notified = new AtomicInteger();
        mailbox.onAdd(notified::incrementAndGet);

        assertTrue(mailbox.isEmpty());

        mailbox.add(itemRunning(() -> { }));

        assertFalse(mailbox.isEmpty());
        assertEquals(1, notified.get(), "the listener registered through the wrapper should fire");

        assertNotNull(mailbox.poll());
        assertTrue(mailbox.isEmpty());
    }

    private static MailItem itemRunning(MailAction action) {
        return new MailItem(Actor.class, "tell", "increment", action);
    }

    private static class Actor {
    }
}
