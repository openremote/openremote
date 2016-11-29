package org.openremote.controller.sensor;

import org.openremote.controller.context.ControllerContext;

import java.util.logging.Logger;

/**
 * Before the {@link ControllerContext} accepts a sensor state update, it
 * executes all rules with a fresh instance of {@link SensorStateUpdate}.
 * <p>
 * Rules can terminate the update of the context, effectively cancelling the
 * original sensor state update. This is the regular behavior when your rules
 * write a sensor value through any {@link org.openremote.controller.rules.SensorFacade}.
 */
public class SensorStateUpdate {

    private static final Logger LOG = Logger.getLogger(SensorStateUpdate.class.getName());

    private ControllerContext controllerContext;
    private SensorState sensorState;
    private boolean terminated = false;

    public SensorStateUpdate(ControllerContext controllerContext, SensorState evt) {
        this.controllerContext = controllerContext;
        this.sensorState = evt;
    }

    public void terminate() {
        LOG.fine("Terminating state update");
        terminated = true;
    }

    public boolean hasTerminated() {
        return terminated;
    }

    public ControllerContext getControllerContext() {
        return controllerContext;
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

