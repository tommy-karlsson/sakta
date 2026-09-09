package com.github.tommykarlsson.sakta.config;

import com.github.tommykarlsson.sakta.core.ActorRef;
import com.github.tommykarlsson.sakta.core.ActorSystem;
import com.github.tommykarlsson.sakta.core.MailItemDecorator;
import com.github.tommykarlsson.sakta.core.MailboxFactoryDecorator;
import com.github.tommykarlsson.sakta.core.impl.MailItemDecoratingMailboxFactoryDecorator;
import com.github.tommykarlsson.sakta.micrometer.MeterActionRunMailItemDecorator;
import com.github.tommykarlsson.sakta.micrometer.MeterMailboxFactoryDecorator;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SaktaAutoConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("sakta.default-mailbox-type=unbounded")
            .withConfiguration(AutoConfigurations.of(SaktaAutoConfig.class, SaktaMicrometerAutoConfig.class));

    /** The plain case: an application that contributes no decorators of its own. */
    @Test
    void startsWithoutAnyDecorators() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ActorSystem.class);
        });
    }

    @Test
    void startsWhenTheApplicationContributesDecorators() {
        runner.withUserConfiguration(DecoratorConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ActorSystem.class);
        });
    }

    /**
     * A decorator contributed by the application has to actually reach the actors, which is the
     * part that the wiring can get wrong without anything failing.
     */
    @Test
    void anApplicationDecoratorDecoratesWhatTheActorsRun() {
        runner.withUserConfiguration(DecoratorConfig.class).run(context -> {
            assertThat(context).hasNotFailed();

            ActorSystem actorSystem = context.getBean(ActorSystem.class);
            ActorRef<Actor> ref = actorSystem.getOrCreateActorRef("actor", Actor::new, Actor.class);
            ref.ask(Actor::answer).get();

            assertThat(context.getBean(CountingDecorator.class).decorated()).isEqualTo(1);
        });
    }

    /** With a registry present, the micrometer auto configuration should contribute its decorators. */
    @Test
    void contributesMicrometerDecoratorsWhenAMeterRegistryIsPresent() {
        runner.withUserConfiguration(MeterRegistryConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MeterActionRunMailItemDecorator.class);
            assertThat(context).hasSingleBean(MeterMailboxFactoryDecorator.class);
        });
    }

    /**
     * The factory decorator that applies the item decorators is conditional on there being any,
     * and the micrometer ones come from another auto configuration. Whether the condition sees
     * them depends on the order the two auto configurations are applied in.
     */
    @Test
    void appliesItemDecoratorsThatComeFromTheMicrometerAutoConfiguration() {
        runner.withUserConfiguration(MeterRegistryConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MailItemDecoratingMailboxFactoryDecorator.class);
        });
    }

    @Test
    void appliesItemDecoratorsThatComeFromTheApplication() {
        runner.withUserConfiguration(DecoratorConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MailItemDecoratingMailboxFactoryDecorator.class);
        });
    }

    /**
     * Closing the context has to let the actors finish. The destroy method spring would infer on
     * its own is the one that stops them accepting and returns without waiting, so this is what
     * says the wiring picked the other one.
     */
    @Test
    void closingTheContextLetsTheActorsFinishWhatTheyWereSent() {
        AtomicInteger handled = new AtomicInteger();
        runner.run(context -> {
            ActorRef<Actor> ref = context.getBean(ActorSystem.class)
                    .getOrCreateActorRef("actor", () -> new Actor(handled), Actor.class);
            for (int i = 0; i < 100; i++) {
                ref.tell(Actor::handleSlowly);
            }
        });

        assertThat(handled).hasValue(100);
    }

    @Test
    void startsWithABoundedMailbox() {
        runner.withPropertyValues("sakta.default-mailbox-type=bounded").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ActorSystem.class);
        });
    }

    @Test
    void contributesNoMicrometerDecoratorsWithoutAMeterRegistry() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(MailItemDecorator.class);
            assertThat(context).doesNotHaveBean(MailboxFactoryDecorator.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class DecoratorConfig {
        @Bean
        CountingDecorator countingDecorator() {
            return new CountingDecorator();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class MeterRegistryConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    static class CountingDecorator implements MailItemDecorator {

        private final AtomicInteger decorated = new AtomicInteger();

        @Override
        public com.github.tommykarlsson.sakta.core.MailItem decorateItem(
                com.github.tommykarlsson.sakta.core.MailItem item) {
            decorated.incrementAndGet();
            return item;
        }

        int decorated() {
            return decorated.get();
        }
    }

    record Actor(AtomicInteger handled) {

        Actor() {
            this(new AtomicInteger());
        }

        int answer() {
            return 42;
        }

        /** Slow enough that a destroy which did not wait would leave most of these unhandled. */
        void handleSlowly() {
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            handled.incrementAndGet();
        }
    }
}
