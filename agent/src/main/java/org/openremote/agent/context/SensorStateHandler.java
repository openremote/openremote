package org.openremote.agent.context;

import org.openremote.agent.sensor.SensorState;

/**
 * Store and/or publish sensor state.
 *
 * TODO This is not accessed concurrently at this time, what happens after we improve agent's thread handling?
 */
public interface SensorStateHandler {

    void start(AgentContext agentContext);

    void stop();

    void put(SensorState sensorState);

    SensorState get(int sensorID);

    /* TODO For polling/pushing to external system we need to be able to continue processing state from a known offset
    Iterator<SensorState> getSince(int timestamp);

    Iterator<SensorState> getSince(int timestamp, int sensorID);
    */
}
