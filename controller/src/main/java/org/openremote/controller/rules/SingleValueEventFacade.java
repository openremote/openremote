package org.openremote.controller.rules;

import org.openremote.controller.event.Event;
import org.openremote.controller.model.Sensor;

public abstract class SingleValueEventFacade<T, U extends Event> extends EventFacade {

    public T name(String sensorName) throws Exception {
        Event evt = eventProcessingContext.getDataContext().queryEvent(sensorName);

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

