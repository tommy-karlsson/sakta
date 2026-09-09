package com.github.tommykarlsson.sakta.micrometer;

import com.github.tommykarlsson.sakta.core.MailItem;
import com.github.tommykarlsson.sakta.core.Mailbox;
import com.github.tommykarlsson.sakta.core.MailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.UnboundedMailboxFactory;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeterMailboxFactoryDecoratorTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    /**
     * The whole chain, since a decorator that produces a factory that produces an unmetered
     * mailbox would leave everything working and nothing measured.
     */
    @Test
    void theMailboxesItEndsUpBuildingAreMetered() throws InterruptedException {
        MailboxFactory factory = new MeterMailboxFactoryDecorator(meterRegistry)
                .decorate(new UnboundedMailboxFactory());

        Mailbox mailbox = factory.createMailbox();
        mailbox.add(new MailItem(Actor.class, "tell", "increment", () -> { }));
        mailbox.poll().run();

        assertEquals(1, meterRegistry.get("sakta.actor.action.queue")
                .tag("actor.type", "Actor")
                .timer()
                .count());
    }

    @Test
    void buildsAMailboxAroundTheOneItWasGiven() {
        Mailbox mailbox = new MeterMailboxFactory(meterRegistry, new UnboundedMailboxFactory()).createMailbox();

        assertInstanceOf(MeterMailbox.class, mailbox);
    }

    private static class Actor {
    }
}
