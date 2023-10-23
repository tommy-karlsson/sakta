package com.github.tommykarlsson.sakta.config;

import com.github.tommykarlsson.sakta.micrometer.MeterActionRunMailItemDecorator;
import com.github.tommykarlsson.sakta.micrometer.MeterMailboxFactory;
import com.github.tommykarlsson.sakta.micrometer.MeterMailboxFactoryDecorator;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import io.micrometer.core.instrument.MeterRegistry;

@AutoConfiguration
@ConditionalOnClass({MeterMailboxFactory.class, MeterRegistry.class})
public class SaktaMicrometerAutoConfig {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    MeterMailboxFactoryDecorator meteredMailboxFactoryDecorator(MeterRegistry meterRegistry) {
        return new MeterMailboxFactoryDecorator(meterRegistry);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    MeterActionRunMailItemDecorator meterActionRunMailItemDecorator(MeterRegistry meterRegistry) {
        return new MeterActionRunMailItemDecorator(meterRegistry);
    }
}
