package org.openremote.model.datapoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DatapointPeriod {

    protected String assetId;

    protected String attributeName;

    protected Long oldest;

    protected Long latest;

    protected DatapointPeriod() {
    }

    @JsonCreator
    public DatapointPeriod(@JsonProperty("assetId") String assetId,
                           @JsonProperty("attributeName") String attributeName,
                           @JsonProperty("oldestTimestamp") Long oldest,
                           @JsonProperty("latestTimestamp") Long latest) {
        this.assetId = assetId;
        this.attributeName = attributeName;
        this.oldest = oldest;
        this.latest = latest;
    }

    @JsonProperty("assetId")
    public String getAssetId() {
        return assetId;
    }

    @JsonProperty("attributeName")
    public String getAttributeName() {
        return attributeName;
    }

    @JsonProperty("oldestTimestamp")
    public Long getOldest() {
        return oldest;
    }

    @JsonProperty("latestTimestamp")
    public Long getLatest() {
        return latest;
    }
}
