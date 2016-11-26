package org.openremote.test.util;

import org.openremote.controller.event.Event;
import org.openremote.controller.event.EventContext;
import org.openremote.controller.event.EventProcessor;

public class EventGrabProcessor extends EventProcessor {

    public Event lastEvent;
    public int totalEventCount = 0;

    @Override
    public void push(EventContext ctx) {
        this.lastEvent = ctx.getEvent();
        totalEventCount++;
    }

    @Override
    public String getName() {
        return "Event Grab";
    }

    public Event getLastEvent() {
        return lastEvent;
    }

    public int getTotalEventCount() {
        return totalEventCount;
    }

    @Override
    public String toString() {
        return "EventGrabProcessor{" +
            "lastEvent=" + lastEvent +
            ", totalEventCount=" + totalEventCount +
            "}";
    }
}
