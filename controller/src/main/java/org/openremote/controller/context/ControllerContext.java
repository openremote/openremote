package org.openremote.controller.context;

import org.openremote.controller.event.Event;
import org.openremote.controller.event.EventProcessingContext;
import org.openremote.controller.event.EventProcessorChain;
import org.openremote.controller.model.Deployment;
import org.openremote.controller.model.Sensor;
import org.openremote.controller.command.Commands;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An instance of a controller, bootstrapped from {@link Deployment}, this is the main API.
 */
public class ControllerContext {

    private static final Logger LOG = Logger.getLogger(ControllerContext.class.getName());

    final protected String controllerID;
    final protected Deployment deployment;
    final protected EventProcessorChain eventProcessorChain;

    private volatile Boolean shutdownInProgress = false;

    public ControllerContext(String controllerID, Deployment deployment) {
        this.controllerID = controllerID;
        this.deployment = deployment;
        this.eventProcessorChain = new EventProcessorChain(deployment);
    }

    public String getControllerID() {
        return controllerID;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public Commands getCommands() {
        return getDeployment().getCommands();
    }

    protected StateStorage getStateStorage() {
        return getDeployment().getStateStorage();
    }

    public synchronized void start() {
        if (shutdownInProgress)
            return;
        LOG.info("Starting context: " + getControllerID());
        eventProcessorChain.start();
        for (Sensor sensor : deployment.getSensors()) {
            // Put initial state "unknown" for each sensor
            getStateStorage().put(new SensorState(new Sensor.UnknownEvent(sensor)));
            sensor.start(this);
        }
    }

    /**
     * <ul>
     * <li>event processors are stopped</li>
     * <li>sensors are stopped</li>
     * <li>the current state is cleared</li>
     * </ul>
     */
    public synchronized void stop() {
        try {
            LOG.info("Stopping context: " + getControllerID());
            shutdownInProgress = true;
            eventProcessorChain.stop();
            for (Sensor sensor : deployment.getSensors()) {
                try {
                    sensor.stop();
                } catch (Throwable t) {
                    LOG.log(Level.SEVERE, "Failed to stop sensor: " + sensor, t);
                }
            }
            getStateStorage().clear();
        } finally {
            shutdownInProgress = false;
        }
    }

    public synchronized void update(Event event) {
        LOG.fine("==> Update from event: " + event);
        if (shutdownInProgress) {
            LOG.fine("<== Shutting down. Ignoring update from: " + event.getSource());
            return;
        }

        EventProcessingContext ctx = new EventProcessingContext(this, event);
        eventProcessorChain.push(ctx);

        // Early exist if one of the processors decided to terminate the chain
        if (ctx.hasTerminated()) {
            LOG.fine("<== Updating status complete, event context terminated, no update was made for event: " + ctx.getEvent());
            return;
        }

        getStateStorage().put(new SensorState(event));
        LOG.fine("<== Updating status complete for event: " + event);
    }

    public String queryValue(int sensorID) {
        if (!getStateStorage().contains(sensorID)) {
            LOG.info("Requested sensor id '" + sensorID + "' was not found. Defaulting to: " + Sensor.UNKNOWN_STATUS);
            return Sensor.UNKNOWN_STATUS;
        }
        return getStateStorage().get(sensorID).getEvent().serialize();
    }

    public String queryValue(String sensorName) {
        return queryValue(deployment.getSensorID(sensorName));
    }

    public Event queryEvent(int sensorID) {
        return getStateStorage().get(sensorID).getEvent();
    }

    public Event queryEvent(String sensorName) {
        Integer sensorID = deployment.getSensorID(sensorName);
        return sensorID != null ? queryEvent(sensorID) : null;
    }
}
