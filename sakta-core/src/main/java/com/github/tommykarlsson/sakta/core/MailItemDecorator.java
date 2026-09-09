package com.github.tommykarlsson.sakta.core;

public interface MailItemDecorator {

    /**
     * Invoked when an item is added to a mailbox.
     *
     * @param item The original item.
     * @return The decorated item.
     */
    MailItem decorateItem(MailItem item);
}
