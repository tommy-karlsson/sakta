package com.github.tommykarlsson.sakta.core;

public record MailItem(Class<?> actorType, String actionType, String actionName, Runnable action) {

    public MailItem(Class<?> actorType, String actionType, Runnable action) {
        this(actorType, actionType, "unknown", action);
    }

    public MailItem withAction(Runnable runnable) {
        return new MailItem(actorType, actionType, actionName, runnable);
    }
}
