package org.openremote.controller.context;

public interface StateStorage {

    void clear();

    void put(SensorState sensorState);

    boolean contains(int sensorID);

    SensorState get(int sensorID);

    /* TODO For offset publishing/polling
    Iterator<SensorState> getSince(int timestamp);

    Iterator<SensorState> getSince(int timestamp, int sensorID);
    */
}
