package org.openremote.controller.statuscache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Use wait, notify, synchronize mechanism to do polling.
 *
 * This table is used to record the skipped changed statuses and waited changed statuses .
 *
 * <b>Use Case1:</b>&nbsp;During the process of iPhone refreshing the polling connection or
 * dealing with the response of changed state, a later (very soon, before iPhone reestablishes the next polling,
 * although it's kind of probability stuff) change won't be detected by iPhone , in other word, this polling request has left
 * the changed status. the result is when iPhone comes back to continue polling, it
 * knows nothing about what has happened just now, and iPhone will keep the old view and waiting for the next change
 * which is not synchronous any more.
 *
 * <b>Use Case2:</b>If no statuses changed, polling request will <b>WAIT</b><br /> the Corresponded ChangedStatusRecord until<br />
 * the waited polling sensor ids' statuses changed. So, the polling request will be notified and get the change statuses.<br />
 */
public class ChangedStatusTable {

    private Map<String, ChangedStatusRecord> recordList;

    public ChangedStatusTable() {
        recordList = new HashMap<>();
    }

    /**
     * Insert a changed status record.
     */
    public synchronized void insert(ChangedStatusRecord record) {
        recordList.put(record.getRecordKey(), record);
    }

    /**
     * Query changed status record by deviceID and pollingSensorIDs(pollingSensorIDs is order-insensitive).
     */
    public synchronized ChangedStatusRecord query(String key) {
        return recordList.get(key);
    }

    public void updateStatusChangedIDs(Integer statusChangedSensorID) {
        for (ChangedStatusRecord record : recordList.values()) {
            // TODO That's some bullshit synchronization
            synchronized (record) {
                if (record.getPollingSensorIDs().contains(statusChangedSensorID)) {
                    record.getStatusChangedSensorIDs().add(statusChangedSensorID);
                    record.notifyAll();
                }
            }
        }
    }

    /**
     * Reset changed status of panel in {@link ChangedStatusTable}.
     */
    @Deprecated
    public synchronized void resetChangedStatusIDs(String key) {
        ChangedStatusRecord skippedStatusRecord = this.query(key);

        if (skippedStatusRecord != null) {
            skippedStatusRecord.setStatusChangedSensorIDs(new HashSet<Integer>());
        }
    }

    /**
     * Clear all records
     */
    public void clearAllRecords() {
        this.recordList.clear();
    }
}
