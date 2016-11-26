package org.openremote.controller.event;

import org.openremote.controller.statuscache.StatusCache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class EventContext {

    private static final Logger LOG = Logger.getLogger(EventContext.class.getName());

    private StatusCache cache;
    private Event event;
    private boolean terminated = false;

    public EventContext(StatusCache cache, Event evt) {
        this.cache = cache;
        this.event = evt;
    }

    public void terminate() {
        LOG.fine("Terminating event context");
        terminated = true;
    }

    public boolean hasTerminated() {
        return terminated;
    }

    public StatusCache getStatusCache() {
        return cache;
    }

    public Event getEvent() {
        return event;
    }

    public Collection getEvents() {
        Set<Event> events = new HashSet<Event>();

        Iterator<Event> it = cache.getStateSnapshot();

        while (it.hasNext()) {
            Event evt = it.next();

            if (evt.getSource().equals(getEvent().getSource()) &&
                evt.getSourceID().equals(getEvent().getSourceID())) {
                continue;
            }

            events.add(evt);
        }

        events.add(getEvent());

        return events;
    }


    @Override
    public String toString() {
        return "Event Context for " + event;
    }
}

