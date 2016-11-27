package org.openremote.controller.event;

import org.openremote.controller.context.DataContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Before the {@link DataContext} accepts an update from an event, it executes the
 * {@link EventProcessorChain} with a fresh instance of {@link EventProcessingContext}.
 * The event processing context encapsulates a single event and its journey through the
 * system.
 *
 * Processors can access the event context and if they desire, terminate it. This means the
 * original event will be discarded and not make it into the data context.
 *
 * TODO Or, as we call it now, a Camel Exchange.
 */
public class EventProcessingContext {

    private static final Logger LOG = Logger.getLogger(EventProcessingContext.class.getName());

    private DataContext dataContext;
    private Event event;
    private boolean terminated = false;

    public EventProcessingContext(DataContext dataContext, Event evt) {
        this.dataContext = dataContext;
        this.event = evt;
    }

    public void terminate() {
        LOG.fine("Terminating event context");
        terminated = true;
    }

    public boolean hasTerminated() {
        return terminated;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public Event getEvent() {
        return event;
    }

    @Override
    public String toString() {
        return "Event Context for " + event;
    }
}

