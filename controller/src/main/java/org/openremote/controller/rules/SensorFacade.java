package org.openremote.controller.rules;

import org.openremote.controller.sensor.SensorState;
import org.openremote.controller.sensor.SensorStateUpdate;

import java.util.logging.Logger;

/**
 * Provide access to all sensors from rules through {@link #name}.
 */
public abstract class SensorFacade<T> {

    private static final Logger LOG = Logger.getLogger(SensorFacade.class.getName());

    protected SensorStateUpdate sensorStateUpdate;

    public void process(SensorStateUpdate sensorStateUpdate) {
        this.sensorStateUpdate = sensorStateUpdate;
    }

    protected void terminateAndReplaceWith(final SensorState newSensorState) {
        sensorStateUpdate.terminate();
        LOG.fine("Terminated sensor state update, dispatching new update: " + newSensorState);
        Thread t = new Thread(
            () -> sensorStateUpdate.getControllerContext().update(newSensorState)
        );
        t.start();
    }

    protected abstract T name(String sensorName) throws Exception;
}

