package com.github.tommykarlsson.sakta.config;

import com.github.tommykarlsson.sakta.core.ActorSystem;
import com.github.tommykarlsson.sakta.core.MailItemDecorator;
import com.github.tommykarlsson.sakta.core.MailboxFactory;
import com.github.tommykarlsson.sakta.core.MailboxFactoryDecorator;
import com.github.tommykarlsson.sakta.core.Scheduler;
import com.github.tommykarlsson.sakta.core.impl.BoundedMailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.MailItemDecoratingMailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.MailItemDecoratingMailboxFactoryDecorator;
import com.github.tommykarlsson.sakta.core.impl.UnboundedMailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.VirtualThreadPerActorScheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@ConditionalOnClass(ActorSystem.class)
@EnableConfigurationProperties(SaktaConfigProperties.class)
public class SaktaAutoConfig {

    @Bean
    @ConditionalOnBean(MailItemDecorator.class)
    MailboxFactoryDecorator mailItemDecoratingMailboxFactoryDecorator(List<MailItemDecorator> mailItemDecorators) {
        return new MailItemDecoratingMailboxFactoryDecorator(mailItemDecorators);
    }

    @Bean
    @ConditionalOnProperty(name = "sakta.default-mailbox-type", havingValue = "bounded")
    MailboxFactory boundedMailboxFactory(
            SaktaConfigProperties saktaConfigProperties,
            List<MailboxFactoryDecorator> decorators) {

        MailboxFactory factory = new BoundedMailboxFactory(
                saktaConfigProperties.getBoundedMailbox().getAddTimeout(),
                saktaConfigProperties.getBoundedMailbox().getCapacity());
        for (MailboxFactoryDecorator decorator : decorators) {
            factory = decorator.decorate(factory);
        }
        return factory;
    }

    @Bean
    @ConditionalOnProperty(name = "sakta.default-mailbox-type", havingValue = "unbounded")
    MailboxFactory unboundedMailboxFactory(List<MailboxFactoryDecorator> decorators) {
        MailboxFactory factory = new UnboundedMailboxFactory();
        for (MailboxFactoryDecorator decorator : decorators) {
            factory = decorator.decorate(factory);
        }
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(Scheduler.class)
    VirtualThreadPerActorScheduler saktaScheduler() {
        return new VirtualThreadPerActorScheduler();
    }

    /**
     * Destroying is left to {@link SpringManagedActorSystem}, so that the actors are given time to
     * finish. An inferred destroy method would find the one that stops them accepting and returns
     * without waiting for any of it.
     */
    @Bean(destroyMethod = "")
    ActorSystem defaultActorSystem(
            MailboxFactory mailboxFactory,
            Scheduler scheduler,
            SaktaConfigProperties saktaConfigProperties) {

        return new SpringManagedActorSystem(mailboxFactory, scheduler, saktaConfigProperties.getShutdownTimeout());
    }
}
