package org.openremote.controller.context;

import org.openremote.controller.sensor.SensorState;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class InMemorySensorStateStorage implements SensorStateStorage {

    private static final Logger LOG = Logger.getLogger(InMemorySensorStateStorage.class.getName());

    final protected Map<Integer, SensorState> sensorStates = new HashMap<>();

    @Override
    synchronized public void clear() {
        sensorStates.clear();
    }

    @Override
    synchronized public void put(SensorState sensorState) {
        int sourceID = sensorState.getSensorID();
        if (sensorStates.get(sourceID) == null) {
            LOG.fine("Inserted: " + sensorState);
            sensorStates.put(sourceID, sensorState);
        } else {
            SensorState previousState = sensorStates.get(sourceID);
            if (previousState.equals(sensorState)) {
                LOG.fine("No change, stored state equals: " + sensorState);
                return;
            }
            LOG.fine("Updated: " + sensorState);
            sensorStates.put(sourceID, sensorState);
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
