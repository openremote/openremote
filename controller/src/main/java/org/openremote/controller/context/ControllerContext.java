package org.openremote.controller.context;

import org.openremote.controller.rules.RuleEngine;
import org.openremote.controller.sensor.SensorState;
import org.openremote.controller.sensor.SensorStateUpdate;
import org.openremote.controller.deploy.Deployment;
import org.openremote.controller.sensor.Sensor;
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
    final protected RuleEngine ruleEngine;

    private volatile Boolean shutdownInProgress = false;

    public ControllerContext(String controllerID, Deployment deployment) {
        this.controllerID = controllerID;
        this.deployment = deployment;
        this.ruleEngine  = new RuleEngine(deployment);
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

    protected SensorStateStorage getStateStorage() {
        return getDeployment().getSensorStateStorage();
    }

    public synchronized void start() {
        if (shutdownInProgress)
            return;
        LOG.info("Starting context: " + getControllerID());
        ruleEngine.start();
        for (Sensor sensor : deployment.getSensors()) {
            // Put initial state "unknown" for each sensor
            getStateStorage().put(new Sensor.UnknownState(sensor));
            sensor.start(this);
        }
    }

    /**
     * <ul>
     * <li>rule engine is stopped</li>
     * <li>sensors are stopped</li>
     * <li>the current state is cleared</li>
     * </ul>
     */
    public synchronized void stop() {
        try {
            LOG.info("Stopping context: " + getControllerID());
            shutdownInProgress = true;
            ruleEngine.stop();
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

    public synchronized void update(SensorState sensorState) {
        LOG.fine("==> Update: " + sensorState);
        if (shutdownInProgress) {
            LOG.fine("<== Shutting down. Ignoring update for: " + sensorState);
            return;
        }

        SensorStateUpdate sensorStateUpdate = new SensorStateUpdate(this, sensorState);

        ruleEngine.process(sensorStateUpdate);

        if (sensorStateUpdate.hasTerminated()) {
            LOG.fine("<== Update complete, processing terminated, no update was made for: " + sensorStateUpdate.getSensorState());
            return;
        }

        getStateStorage().put(sensorState);
        LOG.fine("<== Update complete for: " + sensorState);
    }

    public String queryValue(int sensorID) {
        if (!getStateStorage().contains(sensorID)) {
            LOG.info("Requested sensor id '" + sensorID + "' was not found. Defaulting to: " + Sensor.UNKNOWN_STATE_VALUE);
            return Sensor.UNKNOWN_STATE_VALUE;
        }
        return getStateStorage().get(sensorID).serialize();
    }

    public String queryValue(String sensorName) {
        return queryValue(deployment.getSensorID(sensorName));
    }

    public SensorState queryState(int sensorID) {
        return getStateStorage().get(sensorID);
    }

    public SensorState queryState(String sensorName) {
        Integer sensorID = deployment.getSensorID(sensorName);
        return sensorID != null ? queryState(sensorID) : null;
    }
}
