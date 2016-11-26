package org.openremote.controller.event;

public class EventFacade {

    protected EventContext eventContext;

    public void pushEventContext(EventContext ctx) {
        this.eventContext = ctx;
    }

    protected void dispatchEvent(final Event event) {
        eventContext.terminate();
        Thread t = new Thread(
            () -> eventContext.getStatusCache().update(event)
        );
        t.start();
    }
}

