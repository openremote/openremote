package org.openremote.agent.context;

import org.openremote.agent.command.Commands;
import org.openremote.agent.deploy.Deployment;
import org.openremote.agent.sensor.Sensor;
import org.openremote.agent.sensor.SensorState;
import org.openremote.agent.sensor.SensorStateUpdate;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An instance of an agent, bootstrapped from {@link Deployment}, this is the main API.
 */
public class AgentContext {

    private static final Logger LOG = Logger.getLogger(AgentContext.class.getName());

    final protected String agentID;
    final protected Deployment deployment;

    private volatile Boolean shutdownInProgress = false;

    public AgentContext(String agentID, Deployment deployment) {
        this.agentID = agentID;
        this.deployment = deployment;
    }

    public String getAgentID() {
        return agentID;
    }

    public Deployment getDeployment() {
        return deployment;
    }

    public Commands getCommands() {
        return getDeployment().getCommands();
    }

    public synchronized void start() {
        if (shutdownInProgress)
            return;
        LOG.info("Starting context: " + getAgentID());
        getDeployment().getSensorStateHandler().start(this);
        getDeployment().getRuleEngine().start(this);
        for (Sensor sensor : deployment.getSensors()) {

            // Put initial state "unknown" for each sensor
            getDeployment().getSensorStateHandler().put(
                new Sensor.UnknownState(sensor)
            );

            sensor.start(this);
        }
    }

    public synchronized void stop() {
        try {
            LOG.info("Stopping context: " + getAgentID());
            shutdownInProgress = true;
            try {
                getDeployment().getRuleEngine().stop();
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Error stopping rule engine", t);
            }
            for (Sensor sensor : deployment.getSensors()) {
                try {
                    sensor.stop();
                } catch (Throwable t) {
                    LOG.log(Level.SEVERE, "Error stopping sensor: " + sensor, t);
                }
            }
            try {
                getDeployment().getSensorStateHandler().stop();
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Error stopping state handler", t);
            }
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

        getDeployment().getRuleEngine().process(sensorStateUpdate);

        if (sensorStateUpdate.hasTerminated()) {
            LOG.fine("<== Update complete, processing terminated, no update was made for: " + sensorStateUpdate.getSensorState());
            return;
        }

        getDeployment().getSensorStateHandler().put(sensorState);
        LOG.fine("<== Update complete for: " + sensorState);
    }

    public synchronized String queryValue(int sensorID) {
        SensorState state;
        if ((state = queryState(sensorID)) == null) {
            LOG.info("Requested sensor id '" + sensorID + "' was not found. Defaulting to: " + SensorState.UNKNOWN_VALUE);
            return SensorState.UNKNOWN_VALUE;
        }
        return state.serialize();
    }

    public synchronized String queryValue(String sensorName) {
        return queryValue(deployment.getSensorID(sensorName));
    }

    public synchronized SensorState queryState(int sensorID) {
        return getDeployment().getSensorStateHandler().get(sensorID);
    }

    public synchronized SensorState queryState(String sensorName) {
        Integer sensorID = deployment.getSensorID(sensorName);
        return sensorID != null ? queryState(sensorID) : null;
    }
}
