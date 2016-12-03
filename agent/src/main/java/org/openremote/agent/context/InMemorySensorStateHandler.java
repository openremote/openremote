package org.openremote.agent.context;

import org.openremote.agent.sensor.SensorState;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class InMemorySensorStateHandler implements SensorStateHandler {

    private static final Logger LOG = Logger.getLogger(InMemorySensorStateHandler.class.getName());

    final protected Map<Integer, SensorState> sensorStates = new HashMap<>();

    @Override
    public void start(AgentContext agentContext) {

    }

    @Override
    public void stop() {
        sensorStates.clear();
    }

    @Override
    public void put(SensorState sensorState) {
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
    public SensorState get(int sensorID) {
        return sensorStates.get(sensorID);
    }
}
