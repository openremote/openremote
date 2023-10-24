package org.openremote.model.asset;

import org.openremote.model.value.MetaItemType;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * <p>
 * A {@link org.openremote.model.value.ValueType} that can store 2 and only 2 {@link java.time.LocalDateTime} values,
 * that indicate the start-time and the end-time of a time-constrained {@link Asset} state.
 * Its use is meant to assist in retrieving historical {@link org.openremote.model.datapoint.Datapoint}s between
 * two different {@link java.time.Instant}s.
 * </>
 * <p>
 * By utilizing a {@link AssetStateDuration} {@link org.openremote.model.attribute.Attribute}
 * in conjunction with {@link MetaItemType#STORE_DATA_POINTS}, users can easily request the needed
 * {@link AssetStateDuration}s, for any given time duration, e.g. 30 days, with
 * which they can then request the specific periods for which an {@link Asset} was in a certain, user-defined, state.
 * </>
 * <p>
 *     As an example, assume a Bike-share service, and each {@link Asset} is a single bike.
 *     At the end of a fiscal quarter, we would like to analyze the usage of each bicycle, and the time and duration
 *     at which it moved, to then gauge the profitability of the bike.
 *
 *     Instead of manually retrieving every single datapoint from the asset and then analyzing it to retrieve the value
 *     changes, we can have an {@link AssetStateDuration} {@link org.openremote.model.attribute.Attribute}, which at
 *     the end of any given session, or trip, stores the start-time and the end-time of the bike.
 *
 *     As the Attribute has {@link MetaItemType#STORE_DATA_POINTS}, we can retrieve the {@link AssetStateDuration}s
 *     for the quarter that passed, and for each of the {@link AssetStateDuration}s, request the data-points between
 *     the start and end Timestamps.
 *
 *     In this way, we have access to every single Duration that the Asset was at any given state.
 *
 *     <i>
 *         We can then apply any sort of filtering on the set of {@link AssetStateDuration}s to retrieve our needed data.
 *     </i>
 * </p>
 */
public class AssetStateDuration implements Serializable {
    private Timestamp startTime;
    private Timestamp endTime;

    public AssetStateDuration(Timestamp startTime, Timestamp endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time cannot be null");
        }
        if (startTime.after(endTime)) {
            throw new IllegalArgumentException("Start time cannot be after end time");
        }
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return "AssetStateDuration{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
