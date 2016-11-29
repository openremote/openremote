package org.openremote.controller.command;

import org.openremote.controller.sensor.Sensor;

/**
 * Pull commands are intended for collecting updates from "passive" devices which must
 * be called periodically to read state updates.
 */
public interface PullCommand extends SensorUpdateCommand {

    int POLLING_INTERVAL = 500;

    /**
     * Read and return raw device status.
     *
     * The {@link Sensor} is only provided as a hint, don't call
     * {@link Sensor#update} but return the current state instead.
     */
    String read(Sensor sensor);

    /**
     * Used by {@link Sensor} to determine the interval used before the next call to read() method.
     * This method can be overridden by subclasses (protocol specific implementations) to specify
     * individual polling intervals. The value must be strictly positive, if a negative or zero
     * value is provided, the default is used instead.
     *
     * @return a polling interval in milliseconds (default is 500ms)
     */
    default int getPollingInterval() {
        return POLLING_INTERVAL;
    }
}
