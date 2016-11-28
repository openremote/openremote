package org.openremote.controller.model;

import org.openremote.controller.command.EventProducerCommand;
import org.openremote.controller.command.PullCommand;
import org.openremote.controller.command.PushCommand;
import org.openremote.controller.deploy.SensorDefinition;
import org.openremote.controller.event.Event;
import org.openremote.controller.context.DataContext;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sensors abstract incoming events from devices, either through pulling/polling or
 * listening to devices that actively push their state changes. Sensors operate on
 * commands to execute pull or push operations with devices.
 * <p>
 * Each pulling sensor (for passive devices) has a thread associated with it. Sensors
 * bound to push commands do not create threads of their own but the command
 * implementations themselves are usually multi-threaded.
 * <p>
 * Each sensor can have list of properties which it makes available to implementations of
 * push or pull commands. These properties may be used by protocol implementers to
 * direct their event producer output values to suit the sensor's configuration.
 * <p>
 * Sensors are registered with {@link DataContext}.
 * Sensors create {@link org.openremote.controller.event.Event}s, which represent the
 * data from devices.
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

    private SensorDefinition sensorDefinition;

    /**
     * Reference to the data context that receives and processes the events generated from this sensor.
     */
    private DataContext dataContext;

    /**
     * An event producer command provides values to a sensor, typically through pull or push.
     */
    private EventProducerCommand eventProducerCommand;

    /**
     * This is a polling thread implementation for sensors that use {@link PullCommand}.
     */
    private SensorReader sensorReader;

    protected Sensor(SensorDefinition sensorDefinition, EventProducerCommand eventProducerCommand) {
        if (eventProducerCommand == null) {
            throw new IllegalArgumentException("Event producer command required: " + sensorDefinition);
        }
        this.sensorDefinition = sensorDefinition;
        this.eventProducerCommand = eventProducerCommand;
    }

    public SensorDefinition getSensorDefinition() {
        return sensorDefinition;
    }

    /**
     * Call path for push commands. Allow direct update of the sensor's value in the controller's
     * data context.
     *
     * Before updating the data context, the value is first validated by concrete sensor
     * implementation's {@link Sensor#processEvent(String)} method.
     *
     * @param state the new value for this sensor
     */
    public void update(String state) {
        if (dataContext == null) {
            LOG.fine("Ignoring update, sensor is not running: " + getSensorDefinition());
            return;
        }

        // Allow for sensor type specific processing of the value. This can be used for
        // mappings, validating value ranges, etc. before the value is pushed through
        // the event processor chain and ultimately into the data context.
        Event evt = processEvent(state);
        LOG.fine("Update on ID " + getSensorDefinition().getSensorID() + ", processed '" + state + "', created: " + evt);
        dataContext.update(evt);
    }

    /**
     * Indicates if this sensor is bound to a command that actively pushes new data.
     */
    public boolean isPushCommand() {
        return eventProducerCommand instanceof PushCommand;
    }

    /**
     * Starts this sensor. When this sensor is bound to push command, will invoke its
     * {@link PushCommand#start(Sensor)} method. For {@link PullCommand} implementations,
     * this will start a polling thread to invoke their {@link PullCommand#read(Sensor)} method.
     */
    public void start(DataContext dataContext) {
        this.dataContext = dataContext;
        if (isPushCommand()) {
            PushCommand pushCommand = (PushCommand) eventProducerCommand;
            try {
                pushCommand.start(this);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "There was an implementation error in the push command associated with " +
                    " sensor '" + this + "'. The command implementation may not have started correctly.", t);
            }
        } else if (eventProducerCommand instanceof PullCommand) {
            sensorReader = new SensorReader((PullCommand) eventProducerCommand);
            sensorReader.start();
        } else {
            LOG.info("No action implemented for command type: " + eventProducerCommand);
        }
    }

    /**
     * Stops this sensor. In case this sensor has been bound to a push command, this invokes the
     * {@link PushCommand#stop(Sensor)} method. In case of a {@link PullCommand}, the polling thread
     * is stopped.
     */
    public void stop() {
        dataContext = null;
        if (isPushCommand()) {
            PushCommand pushCommand = (PushCommand) eventProducerCommand;
            try {
                pushCommand.stop(this);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "There was an implementation error in the push command associated with " +
                    " sensor '" + this + "'. The command implementation may not have stopped correctly.", t);
            }
        } else if (sensorReader != null) {
            sensorReader.stop();
        }
    }

    public boolean isRunning() {
        return sensorReader != null && sensorReader.pollingThreadRunning;
    }

    /**
     * Callback to subclasses to apply their event validations and other processing
     * if necessary. This method is called both when a value is pulled and when a
     * command pushes a new sensor value into data context state.
     *
     * @param value value returned by the event producer
     * @return validated and processed value of the event producer
     */
    protected abstract Event processEvent(String value);

    /**
     * Handles the {@link PullCommand#read(Sensor)} polling.
     *
     * TODO This is a major problem, every PullCommand uses a separate thread, we have hundreds of threads!
     */
    private class SensorReader implements Runnable {

        final protected PullCommand pullCommand;

        /**
         * Indicates the device polling thread's run state. Notice that for immediate stop, setting
         * this to false is not sufficient, but the thread also must be interrupted. See
         * {@link SensorReader#stop}.
         */
        private volatile boolean pollingThreadRunning = true;
        private Thread pollingThread;
        private int interval;

        public SensorReader(PullCommand pullCommand) {
            this.pullCommand = pullCommand;
            interval = pullCommand.getPollingInterval() > 0
                ? pullCommand.getPollingInterval()
                : PullCommand.POLLING_INTERVAL;
            LOG.info("Created polling thread interval '" + interval + "' for: " + sensorDefinition);
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
         * {@link SensorReader#start()}.
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
                    new Object[]{pollingThread.getName(), e.getMessage(), interval}
                );
            }
        }

        /**
         * Once every given interval defined in {@link PullCommand#POLLING_INTERVAL}, invokes the command.
         * Depending on command implementation this may create a concrete request on the device to read
         * current state, or it may return a cached value from memory.
         */
        @Override
        public void run() {
            while (pollingThreadRunning) {
                LOG.fine("Polling: " + Sensor.this);
                Sensor.this.update(read());
                try {
                    Thread.sleep(this.interval);
                } catch (InterruptedException e) {
                    pollingThreadRunning = false;
                }
            }
        }

        /**
         * Returns the current state of this sensor.
         * <p>
         * If the sensor is bound to a pull command implementation, the command is invoked --
         * this may yield an active request using the connecting transport to device unless the
         * command implementation caches certain values and returns them from memory.
         * <p>
         * In case of errors, {@link #UNKNOWN_STATUS} is returned.
         * <p>
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
            try {
                return pullCommand.read(Sensor.this);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Implementation error in pull command: " + eventProducerCommand, t);
                return UNKNOWN_STATUS;
            }
        }
    }

    /**
     * A definition of an event that can be used when either an error occurs in sensor implementation
     * (or in the underlying protocol implementation) or if the initial device state has not been
     * fetched yet.
     * <p>
     * The value returned by this event is defined in {@link #UNKNOWN_STATUS}.
     */
    public static class UnknownEvent extends Event<String> {

        public UnknownEvent(Sensor sensor) {
            super(sensor.getSensorDefinition().getSensorID(), sensor.getSensorDefinition().getName());
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

        @Override
        public String toString() {
            return "UnknownEvent{" +
                "sourceId=" + getSourceID() +
                ", source='" + getSource() + "'" +
                "}";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sensor sensor = (Sensor) o;
        return sensorDefinition.equals(sensor.sensorDefinition);
    }

    @Override
    public int hashCode() {
        return sensorDefinition.hashCode();
    }
}
