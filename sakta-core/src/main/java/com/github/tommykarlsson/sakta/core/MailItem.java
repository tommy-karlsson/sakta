package com.github.tommykarlsson.sakta.core;

public record MailItem(Class<?> actorType, String actionType, String actionName, MailAction action) {

    public MailItem(Class<?> actorType, String actionType, MailAction action) {
        this(actorType, actionType, "unknown", action);
    }

    public MailItem withAction(MailAction action) {
        return new MailItem(actorType, actionType, actionName, action);
    }
}
