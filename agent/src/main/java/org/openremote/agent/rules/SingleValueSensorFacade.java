package org.openremote.agent.rules;

import org.openremote.agent.sensor.SensorState;
import org.openremote.agent.sensor.Sensor;

public abstract class SingleValueSensorFacade<T, U extends SensorState> extends SensorFacade<T> {

    @Override
    public T name(String sensorName) throws Exception {
        SensorState sensorState = sensorStateUpdate.getAgentContext().queryState(sensorName);

        if (sensorState instanceof Sensor.UnknownState) {
            sensorState = createDefaultState(sensorState.getSensorID(), sensorState.getSensorName());
        }

        try {
            //noinspection unchecked
            return createAdapter((U) sensorState);
        } catch (ClassCastException ex) {
            throw new Exception("Sensor type mismatch: " + sensorState, ex);
        }
    }

    protected abstract U createDefaultState(int sourceID, String sourceName);

    protected abstract T createAdapter(U sensorState);
}

