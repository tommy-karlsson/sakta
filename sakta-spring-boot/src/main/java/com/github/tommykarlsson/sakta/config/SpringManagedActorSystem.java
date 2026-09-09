package com.github.tommykarlsson.sakta.config;

import java.time.Duration;

import com.github.tommykarlsson.sakta.core.ActorSystem;
import com.github.tommykarlsson.sakta.core.MailboxFactory;
import com.github.tommykarlsson.sakta.core.Scheduler;

import org.springframework.beans.factory.DisposableBean;

/**
 * An actor system that stops with the application context: it stops accepting, gives the actors the
 * configured time to finish what they were already sent, and gives up on whatever is left.
 *
 * <p>The shutting down belongs to the bean itself rather than to something standing beside it,
 * because the context destroys everything that depends on a bean before the bean, so by the time
 * this runs whatever was sending to these actors has already been destroyed.
 */
public class SpringManagedActorSystem extends ActorSystem implements DisposableBean {

    private final Duration shutdownTimeout;

    public SpringManagedActorSystem(MailboxFactory mailboxFactory, Scheduler scheduler, Duration shutdownTimeout) {
        super(mailboxFactory, scheduler);
        this.shutdownTimeout = shutdownTimeout;
    }

    @Override
    public void destroy() throws InterruptedException {
        shutdown(shutdownTimeout);
    }
}
