package org.openremote.controller.context;

/**
 * Store controller context (sensor) state.
 */
public interface StateStorage {

    void clear();

    // TODO: Trigger notification of client that stuff has changed? Put it in a message broker/queue/topic?
    void put(SensorState sensorState);

    boolean contains(int sensorID);

    SensorState get(int sensorID);

    /* TODO For polling/pushing to external system we need to be able to continue processing events from a known offset
    Iterator<SensorState> getSince(int timestamp);

    Iterator<SensorState> getSince(int timestamp, int sensorID);
    */
}
