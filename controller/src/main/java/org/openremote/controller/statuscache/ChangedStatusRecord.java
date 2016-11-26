package org.openremote.controller.statuscache;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * A changed status record.
 * This Record is used to record the skipped changed statuses and waited changed statuses .
 */
public class ChangedStatusRecord {

    /**
     * A logical identity of panel+sensorIDs
     */
    private String key;

    /**
     * The ids a polling request contains
     */
    private Set<Integer> pollingSensorIDs = new TreeSet<Integer>();

    /**
     * The ids whose status had changed in the statusChangedSensorIDs
     */
    private Set<Integer> statusChangedSensorIDs = new HashSet<Integer>(3);


    public ChangedStatusRecord(String key, Set<Integer> pollingSensorIDs) {
        this.key = key;
        this.pollingSensorIDs = pollingSensorIDs;
    }


    public String getRecordKey() {
        return key;
    }

    public Set<Integer> getPollingSensorIDs() {
        return pollingSensorIDs;
    }


    public Set<Integer> getStatusChangedSensorIDs() {
        return statusChangedSensorIDs;
    }

    public void setStatusChangedSensorIDs(Set<Integer> statusChangedSensorIDs) {
        this.statusChangedSensorIDs = statusChangedSensorIDs;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ChangedStatusRecord)) {
            return false;
        }
        ChangedStatusRecord timeoutRecord = (ChangedStatusRecord) obj;
        return timeoutRecord.getRecordKey().equals(getRecordKey());
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "ChangedStatusRecord:" + key +
            " sensorID:" + this.pollingSensorIDs.toString() +
            " statusChangedSensorID:" + this.statusChangedSensorIDs;
    }
}
