package org.openremote.controller.event.facade;

import org.openremote.controller.event.Event;
import org.openremote.controller.event.EventContext;

import java.util.logging.Logger;

public class EventFacade {

    private static final Logger LOG = Logger.getLogger(EventFacade.class.getName());

    protected EventContext eventContext;

    public void pushEventContext(EventContext ctx) {
        this.eventContext = ctx;
    }

    protected void dispatchEvent(final Event event) {
        eventContext.terminate();
        LOG.fine("Dispatching on new thread: " + event);
        Thread t = new Thread(
            () -> eventContext.getStatusCache().update(event)
        );
        t.start();
    }
}

