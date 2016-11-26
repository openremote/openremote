package org.openremote.controller.command;

import org.openremote.controller.model.Sensor;

public abstract class PullCommand implements EventProducerCommand {

    public final static int POLLING_INTERVAL = 500;

    /**
     * Read a device status and translate it to a sensor's datatype.
     */
    public abstract String read(Sensor sensor);

    /**
     * Used by {@link Sensor.DeviceReader} to determine the interval used before the next call to read() method.
     * This method can be overridden by subclasses (protocol specific implementations) to specify individual polling intervals.
     * The value must be strictly positive, if a negative or zero value is provided, the default is used instead.
     *
     * @return a polling interval in milliseconds (default is 500ms)
     */
    public int getPollingInterval() {
        return POLLING_INTERVAL;
    }

}
