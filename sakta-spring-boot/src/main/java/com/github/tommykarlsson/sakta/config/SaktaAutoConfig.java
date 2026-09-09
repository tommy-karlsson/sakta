package com.github.tommykarlsson.sakta.config;

import java.util.List;

import com.github.tommykarlsson.sakta.core.ActorSystem;
import com.github.tommykarlsson.sakta.core.MailItemDecorator;
import com.github.tommykarlsson.sakta.core.MailboxFactory;
import com.github.tommykarlsson.sakta.core.MailboxFactoryDecorator;
import com.github.tommykarlsson.sakta.core.Scheduler;
import com.github.tommykarlsson.sakta.core.impl.BoundedMailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.MailItemDecoratingMailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.UnboundedMailboxFactory;
import com.github.tommykarlsson.sakta.core.impl.VirtualThreadPerActorScheduler;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ActorSystem.class)
@EnableConfigurationProperties(SaktaConfigProperties.class)
public class SaktaAutoConfig {

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

    @Bean
    ActorSystem defaultActorSystem(
            MailboxFactory mailboxFactory,
            Scheduler scheduler,
            List<MailItemDecorator> mailItemDecorators) {

        // Outside the mailbox factory, so the item decorators run before any mailbox decorator
        // that factory adds sees the item.
        return new ActorSystem(
                new MailItemDecoratingMailboxFactory(mailboxFactory, mailItemDecorators),
                scheduler);
    }
}
