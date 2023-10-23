package com.github.tommykarlsson.sakta.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sakta")
public class SaktaConfigProperties {

    private String defaultMailboxType = "unbounded";

    private final BoundedMailboxProperties boundedMailbox = new BoundedMailboxProperties();

    public String getDefaultMailboxType() {
        return defaultMailboxType;
    }

    public void setDefaultMailboxType(String defaultMailboxType) {
        this.defaultMailboxType = defaultMailboxType;
    }

    public BoundedMailboxProperties getBoundedMailbox() {
        return boundedMailbox;
    }

    public static class BoundedMailboxProperties {
        private int capacity;
        private Duration addTimeout;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public Duration getAddTimeout() {
            return addTimeout;
        }

        public void setAddTimeout(Duration addTimeout) {
            this.addTimeout = addTimeout;
        }
    }
}
