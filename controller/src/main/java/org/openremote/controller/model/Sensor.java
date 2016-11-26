package org.openremote.controller.model;

import org.openremote.controller.command.EventProducerCommand;
import org.openremote.controller.command.PullCommand;
import org.openremote.controller.command.PushCommand;
import org.openremote.controller.event.Event;
import org.openremote.controller.statuscache.StatusCache;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Sensors abstract incoming events from devices, either through pulling/polling or
 * listening to devices that actively push their state changes. Sensors operate on
 * commands to execute pull or push operations with devices.
 *
 * Each pulling sensor (for passive devices) has a thread associated with it. Sensors
 * bound to push commands do not create threads of their own but the command
 * implementations themselves are usually multi-threaded.
 *
 * Each sensor can have list of properties which it makes available to implementations of
 * push or pull commands. These properties may be used by protocol implementers to
 * direct their event producer output values to suit the sensor's configuration.
 *
 * Sensors are registered with
 * {@link org.openremote.controller.statuscache.StatusCache device state cache}. Sensors create
 * {@link org.openremote.controller.event.Event} which represent the data from event
 * producers and are passed by cache to
 * {@link org.openremote.controller.event.EventProcessor}s.
 *
 * Therefore the object hierarchy for sensors is as follows:
 *
 * <pre>{@code Cache (one) <--> (many) Sensor (one) <--> (one) Event Producer}</pre>
 *
 * Event producers are created by third party integrators where as cache and sensors are part of
 * the controller framework.
 */
public abstract class Sensor {

    private static final Logger LOG = Logger.getLogger(Sensor.class.getName());

    public static final String UNKNOWN_STATUS = "N/A";

    /**
     * Helper method to allow subclasses to check if a given value matches the 'N/A' string that
     * is used for uninitialized or error states in sensor implementations.
     *
     * @param value value to compare to
     * @return true if value matches the string representation of
     * {@link org.openremote.controller.model.Sensor.UnknownEvent}, false otherwise
     */
    public static boolean isUnknownSensorValue(String value) {
        return value.equals(UNKNOWN_STATUS);
    }

    /**
     * Human readable sensor name. Used with event processors, logging, etc.
     */
    private String sensorName;

    /**
     * Sensor's unique ID. Must be unique per controller deployment.
     */
    private int sensorID;

    /**
     * Identifier of the the related command that is used to receive sensor values.
     */
    private int commandID;

    /**
     * Reference to the state cache that receives and processes the events generated from this sensor.
     */
    private StatusCache statusCache;

    /**
     * An event producer is a protocol handler that can be customized to return values to a sensor.
     * Therefore the sensor implementation remains type (as in Java class type) and protocol
     * independent and delegates these protocol specific tasks to event producer implementations.
     *
     * Two sub-categories of event producers exist today: pull and push commands. These are dealt
     * differently in that pull commands are actively polled by
     * the sensor while push commands produce events to the controller at their own schedule.
     */
    private EventProducerCommand eventProducerCommand;

    /**
     * This is a polling thread implementation for sensors that use
     * {@link PullCommand} instead of
     * {@link PushCommand}.
     */
    private DeviceReader deviceReader;

    /**
     * Sensor properties. These properties can be used by the protocol implementors to direct
     * their implementation on read commands and event listeners according to sensor configuration.
     */
    private Map<String, String> sensorProperties;

    /**
     * Constructs a new sensor with a given name, ID, sensor datatype (deprecated legacy),
     * an event producing protocol handler, reference to the managing device state cache,
     * and a set of sensor properties.
     *
     * @param name                 Human readable name of the sensor. Used with event processors, logging, etc.
     * @param sensorID             A unique sensor ID. Must be unique per controller deployment.
     * @param cache                reference to a device state cache this sensor registers itself with and pushes
     *                             value updates to
     * @param eventProducerCommand protocol handler implementation
     * @param commandID            A unique command ID. Must be unique per controller deployment.
     * @param sensorProperties     Additional sensor properties. These properties can be used by the protocol
     *                             implementors to direct their implementation according to sensor configuration.
     */
    protected Sensor(String name,
                     int sensorID,
                     StatusCache cache,
                     EventProducerCommand eventProducerCommand,
                     int commandID,
                     Map<String, String> sensorProperties) {
        if (eventProducerCommand == null) {
            throw new IllegalArgumentException("Sensor requires event producer command: " + sensorID);
        }

        if (sensorProperties == null) {
            sensorProperties = new HashMap<>(0);
        }

        this.sensorName = name;
        this.sensorID = sensorID;
        this.statusCache = cache;
        this.eventProducerCommand = eventProducerCommand;
        this.commandID = commandID;
        this.sensorProperties = sensorProperties;
    }

    /**
     * Returns the human readable name of this sensor.
     *
     * @return sensor's name as defined in tooling and controller's XML definition
     */
    public String getName() {
        return sensorName;
    }

    /**
     * Returns this sensor's ID. The sensor ID is unique within a controller deployment.
     *
     * @return sensor ID
     */
    public int getSensorID() {
        return sensorID;
    }

    /**
     * Returns the identifier of the related command that is used to update sensor values.
     *
     * @return the command ID
     */
    public int getCommandID() {
        return commandID;
    }

    /**
     * Returns sensor's properties. Properties are simply string based name-value mappings.
     * Concrete sensor implementations may specify which particular properties they expose.
     *
     * The returned map does not reference this sensor instance and can be modified freely.
     *
     * @return sensor properties or an empty collection
     */
    public Map<String, String> getProperties() {
        HashMap<String, String> props = new HashMap<>(5);
        props.putAll(sensorProperties);

        return props;
    }

    /**
     * Call path for push commands. Allow direct update of the sensor's value in the controller's
     * global state cache.
     *
     * Before updating the state cache, the value is first validated by concrete sensor
     * implementation's {@link Sensor#processEvent(String)} method.
     *
     * @param state the new value for this sensor
     */
    public void update(String state) {
        // TODO : signature update to event type -- http://jira.openremote.org/browse/ORCJAVA-97
        // TODO : event dispatcher thread -- http://jira.openremote.org/browse/ORCJAVA-99

        // Allow for sensor type specific processing of the value.
        // This can be used for mappings, validating value ranges, etc. before the value is
        // pushed into device state cache and event processor chain.

        Event evt = processEvent(state);
        LOG.fine("Processed '" + state + "', received: " + evt.getValue());
        statusCache.update(evt);
    }

    /**
     * Indicates if this sensor is bound to a command that actively pushes new data.
     */
    public boolean isPushCommand() {
        return eventProducerCommand instanceof PushCommand;
    }

    /**
     * Starts this sensor. When this sensor is bound to push command, will invoke its
     * {@link PushCommand#setSensor(Sensor)} method.
     * For {@link PullCommand} implementations, this will start a polling thread to invoke
     * their {@link PullCommand#read(Sensor)} method.
     */
    public void start() {
        if (isPushCommand()) {
            PushCommand pushCommand = (PushCommand) eventProducerCommand;
            try {
                pushCommand.setSensor(this);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "There was an implementation error in the push command associated with " +
                    " sensor '" + this + "'. The command implementation may not have started correctly.", t);
            }
        } else {
            deviceReader = new DeviceReader();
            deviceReader.start();
        }
    }

    /**
     * Stops this sensor. In case this sensor has been bound to a push command, the stop
     * invokes its {@link PushCommand#stop(Sensor)} method. In case of a
     * {@link PullCommand}, the polling thread is stopped.
     *
     * @see org.openremote.controller.model.Sensor#start()
     */
    public void stop() {
        if (isPushCommand()) {
            PushCommand pushCommand = (PushCommand) eventProducerCommand;
            try {
                pushCommand.stop(this);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "There was an implementation error in the push command associated with " +
                    " sensor '" + this + "'. The command implementation may not have stopped correctly.", t);
            }
        } else {
            if (deviceReader != null) {
                deviceReader.stop();
            }
        }
    }

    public boolean isRunning() {
        return deviceReader != null && deviceReader.pollingThreadRunning;
    }

    /**
     * Callback to subclasses to apply their event validations and other processing
     * if necessary. This method is called both when a value is
     * pulled or a command pushes a new sensor value to state cache.
     *
     * @param value value returned by the event producer
     * @return validated and processed value of the event producer
     * @see Sensor#update
     */
    protected abstract Event processEvent(String value);

    /**
     * Handles the {@link PullCommand#read(Sensor)} polling.
     */
    private class DeviceReader implements Runnable {

        /**
         * Indicates the device polling thread's run state. Notice that for immediate stop, setting
         * this to false is not sufficient, but the thread also must be interrupted. See
         * {@link DeviceReader#stop}.
         */
        private volatile boolean pollingThreadRunning = true;

        /**
         * The actual thread reference being used by the owning sensor instance when
         * {@link PullCommand} is used as event producer.
         */
        private Thread pollingThread;

        /**
         * The actual interval used
         */
        private int interval;

        public DeviceReader() {
            if (eventProducerCommand instanceof PullCommand) {
                int pollingInterval = ((PullCommand) eventProducerCommand).getPollingInterval();
                if (pollingInterval > 0) {
                    this.interval = ((PullCommand) eventProducerCommand).getPollingInterval();
                } else {
                    this.interval = PullCommand.POLLING_INTERVAL;
                }
            } else {
                this.interval = PullCommand.POLLING_INTERVAL;
            }
            LOG.log(Level.INFO,
                "Created polling thread for sensor (ID = {0}, name = {1}), polling interval {3}",
                new Object[]{getSensorID(), getName(), this.interval}
            );
        }

        /**
         * Starts the device polling thread.
         */
        public void start() {
            pollingThreadRunning = true;
            pollingThread = new Thread(this);
            pollingThread.start();
        }

        /**
         * Stops the device polling thread. Notice that stopping the thread will cause it to exit.
         * The same thread cannot be resumed but a new thread will be created via
         * {@link DeviceReader#start()}.
         */
        public void stop() {
            pollingThreadRunning = false;
            try {
                AccessController.doPrivilegedWithCombiner((PrivilegedAction<Void>) () -> {
                    pollingThread.interrupt();
                    return null;
                });
            } catch (SecurityException e) {
                LOG.log(
                    Level.WARNING,
                    "Could not interrupt device polling thread ''{0}'' due to security constraints: {1}\n" +
                        "the thread will exit in {2} milliseconds...",
                    new Object[]{pollingThread.getName(), e.getMessage(), PullCommand.POLLING_INTERVAL}
                );
            }
        }

        /**
         * Once every given interval defined in {@link PullCommand#POLLING_INTERVAL}, invokes a read()
         * request on the sensor and the underlying event producer. Depending on event producer
         * implementation this may create a concrete request on the device to read current state, or
         * it may return a cached value from memory.
         */
        @Override
        public void run() {
            LOG.log(
                Level.INFO,
                "Started polling thread for sensor (ID = {0}, name = {1}).",
                new Object[]{getSensorID(), getName()}
            );

            while (pollingThreadRunning) {
                Sensor.this.update(read());

                try {
                    Thread.sleep(this.interval);
                } catch (InterruptedException e) {
                    pollingThreadRunning = false;
                    LOG.log(
                        Level.INFO,
                        "Shutting down polling thread of sensor (ID = {0}, name = {1}).",
                        new Object[]{getSensorID(), getName()}
                    );
                    Thread.currentThread().interrupt();
                }
            }
        }


        /**
         * Returns the current state of this sensor.
         *
         * If the sensor is bound to a pull command implementation, the command is invoked --
         * this may yield an active request using the connecting transport to device unless the
         * command implementation caches certain values and returns them from memory.
         *
         * In case of a push command, this method does not invoke anything on the command itself
         * but returns the last stored state from the controller's device state cache associated with
         * this sensor's ID. A push command implementation is responsible of actively updating and
         * inserting the device state values into the controller's cache.
         *
         * In case of errors, {@link #UNKNOWN_STATUS} is returned.
         *
         * This default read() implementation does not validate the input from protocol pull commands
         * in any way (other than handling implementation errors that yield runtime exceptions).
         * concrete subclasses should override and implement {@link Sensor#processEvent(String)}
         * to validate the inputs from commands and to ensure the sensor returns values that
         * adhere to its datatype.
         *
         * @return sensor's value, according to its datatype and provided by commands or
         * {@link #UNKNOWN_STATUS} if value cannot be found.
         */
        private String read() {

            // If this sensor abstracts a push command, the read() will not invoke the protocol
            // handler associated with this sensor directly -- instead we try to fetch the latest
            // value produced by a push command from the controller's global device state cache.

            if (isPushCommand()) {
                return statusCache.queryStatus(sensorID);
            } else if (eventProducerCommand instanceof PullCommand) {
                // If we are dealing with pull commands, execute it to explicitly fetch the
                // device state...
                PullCommand command = (PullCommand) eventProducerCommand;
                try {
                return command.read(Sensor.this);
                } catch (Throwable t) {
                    LOG.log(Level.SEVERE, "Implementation error in pull command: " + eventProducerCommand, t);
                    return UNKNOWN_STATUS;
                }
            } else {
                throw new IllegalArgumentException(
                    "Sensor has been initialized with an event producer that is neither a pull nor a push command. " +
                        "This type can not be handled: " + eventProducerCommand.getClass().getName()
                );
            }
        }
    }

    /**
     * A definition of an event that can be used when either an error occurs in sensor implementation
     * (or in the underlying protocol implementation) or if the initial device state has not been
     * fetched yet.
     *
     * The value returned by this event is defined in {@link #UNKNOWN_STATUS}.
     */
    public static class UnknownEvent extends Event<String> {

        public UnknownEvent(Sensor sensor) {
            super(sensor.getSensorID(), sensor.getName());
        }

        @Override
        public String getValue() {
            return serialize();
        }

        @Override
        public void setValue(String str) {
            // NOOP
        }

        @Override
        public UnknownEvent clone(String ignored) {
            return this;
        }

        @Override
        public boolean isEqual(Object o) {
            if (o == null) {
                return false;
            }
            if (o.getClass() != this.getClass()) {
                return false;
            }
            UnknownEvent ue = (UnknownEvent) o;
            return ue.getSourceID().equals(this.getSourceID())
                && ue.getSource().equals(this.getSource());
        }

        @Override
        public String serialize() {
            return UNKNOWN_STATUS;
        }
    }

    /**
     * Test sensor object equality based on unique identifier (as returned by {@link #getSensorID}.
     *
     * Subclasses are considered equal, despite what their data values are, as long as the sensor
     * ID is equal.
     *
     * @param o object to compare to
     * @return true if equals, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (!(o instanceof Sensor))
            return false;

        Sensor sensor = (Sensor) o;

        return sensor.getSensorID() == this.getSensorID();
    }

    @Override
    public int hashCode() {
        return sensorID;
    }
}
