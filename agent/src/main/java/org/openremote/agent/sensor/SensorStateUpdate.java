package org.openremote.agent.sensor;

import org.openremote.agent.context.AgentContext;

import java.util.logging.Logger;

/**
 * Before the {@link AgentContext} accepts a sensor state update, it
 * executes all rules with a fresh instance of {@link SensorStateUpdate}.
 * <p>
 * Rules can terminate the update of the context, effectively cancelling the
 * original sensor state update. This is the regular behavior when your rules
 * write a sensor value through any {@link org.openremote.agent.rules.SensorFacade}.
 */
public class SensorStateUpdate {

    private static final Logger LOG = Logger.getLogger(SensorStateUpdate.class.getName());

    private AgentContext agentContext;
    private SensorState sensorState;
    private boolean terminated = false;

    public SensorStateUpdate(AgentContext agentContext, SensorState evt) {
        this.agentContext = agentContext;
        this.sensorState = evt;
    }

    public void terminate() {
        LOG.fine("Terminating state update");
        terminated = true;
    }

    public boolean hasTerminated() {
        return terminated;
    }

    public AgentContext getAgentContext() {
        return agentContext;
    }

    public SensorState getSensorState() {
        return sensorState;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "sensorState=" + sensorState +
            ", terminated=" + terminated +
            '}';
    }
}

