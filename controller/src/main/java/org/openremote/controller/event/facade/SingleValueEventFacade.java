package org.openremote.controller.event.facade;

import org.openremote.controller.event.Event;
import org.openremote.controller.model.Sensor;

public abstract class SingleValueEventFacade<T, U extends Event> extends EventFacade {

    public T name(String name) throws Exception {
        Event evt = eventContext.getStatusCache().queryStatus(name);

        if (evt instanceof Sensor.UnknownEvent) {
            evt = createDefaultEvent(evt.getSourceID(), evt.getSource());
        }

        try {
            return createAdapter((U) evt);
        } catch (ClassCastException e) {
            throw new Exception("event class mismatch");
        }
    }

    protected abstract U createDefaultEvent(int sourceID, String sourceName);

    protected abstract T createAdapter(U event);
}

