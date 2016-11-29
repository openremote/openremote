package org.openremote.controller.context;

import org.openremote.controller.event.Event;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class InMemoryStateStorage implements StateStorage {

    private static final Logger LOG = Logger.getLogger(InMemoryStateStorage.class.getName());

    final protected Map<Integer, SensorState> sensorStates = new HashMap<>();

    @Override
    synchronized public void clear() {
        sensorStates.clear();
    }

    @Override
    synchronized public void put(SensorState sensorState) {
        Event event = sensorState.getEvent();
        int sourceID = event.getSourceID();
        if (sensorStates.get(sourceID) == null) {
            LOG.fine("Inserted: " + event);
            sensorStates.put(sourceID, new SensorState(event));
        } else {
            SensorState previousState = sensorStates.get(sourceID);
            if (previousState.getEvent().equals(event)) {
                LOG.fine("No change, stored state equals: " + event);
                return;
            }
            LOG.fine("Updated: " + event);
            sensorStates.put(sourceID, new SensorState(event));
        }
    }

    @Override
    synchronized public boolean contains(int sensorID) {
        return sensorStates.containsKey(sensorID);
    }

    @Override
    public SensorState get(int sensorID) {
        return sensorStates.get(sensorID);
    }
}
