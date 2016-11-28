package org.openremote.controller.context;

import org.openremote.controller.event.Event;

public class SensorState {

    final protected long timestamp = System.currentTimeMillis();
    final protected Event event;

    public SensorState(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }

    public boolean isNewerThan(long timestamp) {
        return this.timestamp < timestamp;
    }

    @Override
    public String toString() {
        return "SensorState{" +
            "timestamp=" + timestamp +
            ", event=" + event +
            '}';
    }
}
