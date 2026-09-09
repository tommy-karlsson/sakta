package com.github.tommykarlsson.sakta.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sakta")
public class SaktaConfigProperties {

    private String defaultMailboxType = "unbounded";

    /** How long the actors are given to finish what they were sent, when the context shuts down. */
    private Duration shutdownTimeout = Duration.ofSeconds(30);

    private final BoundedMailboxProperties boundedMailbox = new BoundedMailboxProperties();

    public String getDefaultMailboxType() {
        return defaultMailboxType;
    }

    public void setDefaultMailboxType(String defaultMailboxType) {
        this.defaultMailboxType = defaultMailboxType;
    }

    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    public void setShutdownTimeout(Duration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
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
