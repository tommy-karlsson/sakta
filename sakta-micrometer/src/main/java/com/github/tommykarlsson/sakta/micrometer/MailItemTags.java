package com.github.tommykarlsson.sakta.micrometer;

import com.github.tommykarlsson.sakta.core.MailItem;

import io.micrometer.core.instrument.Tags;

/**
 * The tags identifying the actor and the action a meter is about.
 *
 * <p>Read off the item once, when it is decorated, so that the timed action can be held onto
 * without holding onto the item it came from.
 */
final class MailItemTags {

    private MailItemTags() {
    }

    static Tags of(MailItem item) {
        return Tags.of(
                "actor.type", item.actorType().getSimpleName(),
                "action.type", item.actionType(),
                "action.name", item.actionName());
    }
}
