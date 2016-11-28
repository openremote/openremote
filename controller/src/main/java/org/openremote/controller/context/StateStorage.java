package org.openremote.controller.context;

public interface StateStorage {

    void clear();

    void put(SensorState sensorState);

    boolean contains(int sensorID);

    SensorState get(int sensorID);

    /* TODO For polling/pushing to external system we need to be able to continue processing events from a known offset
    Iterator<SensorState> getSince(int timestamp);

    Iterator<SensorState> getSince(int timestamp, int sensorID);
    */
}
