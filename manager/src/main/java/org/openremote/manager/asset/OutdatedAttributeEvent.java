package org.openremote.manager.asset;

import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.Event;

/**
 * This is an event for use by internal event subscribers to be notified of an outdated {@link AttributeEvent} that has
 * just been processed.
 */
public class OutdatedAttributeEvent extends Event {
    protected AttributeEvent event;

    public OutdatedAttributeEvent(AttributeEvent event) {
        super(event.getTimestamp());
        this.event = event;
    }
}
