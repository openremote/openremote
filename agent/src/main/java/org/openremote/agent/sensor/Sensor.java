package org.openremote.agent.sensor;

import org.openremote.agent.command.PullCommand;
import org.openremote.agent.command.PushCommand;
import org.openremote.agent.command.SensorUpdateCommand;
import org.openremote.agent.context.AgentContext;
import org.openremote.agent.deploy.SensorDefinition;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sensors create {@link SensorState}s, which represent the data obtained from devices.
 * <p>
 * A sensor is registered with a {@link AgentContext}. State updates are obtained
 * either through pulling/polling or listening to devices that actively push their state
 * changes.
 * <p>
 * A sensor linked with a {@link PullCommand} runs a new thread when started, and executes
 * a periodic read.
 * <p>
 * A sensor linked with a {@link PushCommand} does not create threads, but delegates
 * threading and calling back with values to the command implementation.
 * <p>
 * Each sensor can have list of meta-data properties, used by command implementors.
 */
public abstract class Sensor {

    private static final Logger LOG = Logger.getLogger(Sensor.class.getName());

    final protected SensorDefinition sensorDefinition;
    final protected SensorUpdateCommand sensorUpdateCommand;
    protected AgentContext agentContext;

    /**
     * This is a polling thread implementation for sensors that use {@link PullCommand}.
     */
    protected SensorReader sensorReader;

    protected Sensor(SensorDefinition sensorDefinition, SensorUpdateCommand sensorUpdateCommand) {
        if (sensorUpdateCommand == null) {
            throw new IllegalArgumentException("Sensor update command required: " + sensorDefinition);
        }
        this.sensorDefinition = sensorDefinition;
        this.sensorUpdateCommand = sensorUpdateCommand;
    }

    public SensorDefinition getSensorDefinition() {
        return sensorDefinition;
    }

    public SensorUpdateCommand getSensorUpdateCommand() {
        return sensorUpdateCommand;
    }

    public AgentContext getAgentContext() {
        return agentContext;
    }

    /**
     * Indicates if this sensor is bound to a command that actively pushes new data.
     */
    public boolean isPushCommand() {
        return sensorUpdateCommand instanceof PushCommand;
    }

    /**
     * Starts this sensor. When this sensor is bound to a push command, this invokes the
     * {@link PushCommand#start(Sensor)} method. For {@link PullCommand} implementations,
     * this will start a polling thread to invoke the {@link PullCommand#read(Sensor)} method.
     */
    public void start(AgentContext agentContext) {
        LOG.info("Starting sensor: " + this);
        this.agentContext = agentContext;
        if (isPushCommand()) {
            PushCommand pushCommand = (PushCommand) sensorUpdateCommand;
            try {
                pushCommand.start(this);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "There was an implementation error in the push command associated with " +
                    " sensor '" + this + "'. The command implementation may not have started correctly.", t);
            }
        } else if (sensorUpdateCommand instanceof PullCommand) {
            sensorReader = new SensorReader((PullCommand) sensorUpdateCommand);
            sensorReader.start();
        } else {
            LOG.info("No action implemented for command type: " + sensorUpdateCommand);
        }
    }

    /**
     * Stops this sensor. In case this sensor has been bound to a push command, this invokes the
     * {@link PushCommand#stop(Sensor)} method. In case of a {@link PullCommand}, the polling thread
     * is stopped.
     */
    public void stop() {
        LOG.info("Stopping sensor: " + this);
        agentContext = null;
        if (isPushCommand()) {
            PushCommand pushCommand = (PushCommand) sensorUpdateCommand;
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

    /**
     * Callback for {@link PushCommand} implementations. Before updating the agent context, the
     * given value is first validated and/or transformed by concrete sensor implementation's
     * {@link Sensor#process} method.
     */
    public void update(String state) {
        if (agentContext == null) {
            LOG.fine("Ignoring update, sensor is not running: " + getSensorDefinition());
            return;
        }
        SensorState resultState = process(state);
        LOG.fine("Update on ID " + getSensorDefinition().getSensorID() + ", input string: '" + state + "', result state: " + resultState);
        agentContext.update(resultState);
    }

    /**
     * Callback to subclasses to apply their state validations and other processing
     * if necessary. This method is called both when a value is pulled and when a
     * command pushes a new sensor value into {@link AgentContext} state.
     */
    protected abstract SensorState process(String value);

    /**
     * Handles the {@link PullCommand#read(Sensor)} polling.
     * <p>
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
         * In case of errors, {@link SensorState#UNKNOWN_VALUE} is returned.
         * <p>
         * This default read() implementation does not validate the input from protocol pull commands
         * in any way (other than handling implementation errors that yield runtime exceptions).
         * concrete subclasses should override and implement {@link Sensor#process(String)}
         * to validate the inputs from commands and to ensure the sensor returns values that
         * adhere to its datatype.
         *
         * @return sensor's value, according to its datatype and provided by commands or
         * {@link SensorState#UNKNOWN_VALUE} if value cannot be found.
         */
        private String read() {
            try {
                return pullCommand.read(Sensor.this);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Implementation error in pull command: " + sensorUpdateCommand, t);
                return SensorState.UNKNOWN_VALUE;
            }
        }
    }

    /**
     * A definition of a sensor state that can be used when either an error occurs in sensor
     * implementation (or in the underlying protocol implementation) or if the initial device
     * state has not been fetched yet.
     * <p>
     * The value returned by this state is defined in {@link #UNKNOWN_VALUE}.
     */
    public static class UnknownState extends SensorState<String> {

        public UnknownState(Sensor sensor) {
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
        public UnknownState clone(String ignored) {
            return this;
        }

        @Override
        public String serialize() {
            return UNKNOWN_VALUE;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "sourceId=" + getSensorID() +
                ", source='" + getSensorName() + "'" +
                "}";
        }
    }
}
