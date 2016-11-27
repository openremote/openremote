package org.openremote.controller.rules;

import org.openremote.controller.event.Event;
import org.openremote.controller.event.EventProcessingContext;

import java.util.logging.Logger;

public abstract class EventFacade {

    private static final Logger LOG = Logger.getLogger(EventFacade.class.getName());

    protected EventProcessingContext eventProcessingContext;

    public void pushEventContext(EventProcessingContext ctx) {
        this.eventProcessingContext = ctx;
    }

    protected void dispatchEvent(final Event event) {
        eventProcessingContext.terminate();
        LOG.fine("Dispatching on new thread: " + event);
        Thread t = new Thread(
            () -> eventProcessingContext.getDataContext().update(event)
        );
        t.start();
    }
}

