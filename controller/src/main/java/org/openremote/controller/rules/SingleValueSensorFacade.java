package org.openremote.controller.rules;

import org.openremote.controller.sensor.SensorState;
import org.openremote.controller.sensor.Sensor;

public abstract class SingleValueSensorFacade<T, U extends SensorState> extends SensorFacade<T> {

    @Override
    public T name(String sensorName) throws Exception {
        SensorState sensorState = sensorStateUpdate.getControllerContext().queryState(sensorName);

        if (sensorState instanceof Sensor.UnknownState) {
            sensorState = createDefaultState(sensorState.getSensorID(), sensorState.getSensorName());
        }

        try {
            return createAdapter((U) sensorState);
        } catch (ClassCastException ex) {
            throw new Exception("Sensor type mismatch: " + sensorState, ex);
        }
    }

    protected abstract U createDefaultState(int sourceID, String sourceName);

    protected abstract T createAdapter(U sensorState);
}

