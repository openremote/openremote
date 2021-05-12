package org.openremote.model.datapoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DatapointPeriod {

    protected String assetId;

    protected String attributeName;

    protected long oldest;

    protected long latest;

    protected DatapointPeriod() {
    }

    @JsonCreator
    public DatapointPeriod(@JsonProperty("assetId") String assetId,
                           @JsonProperty("attributeName") String attributeName,
                           @JsonProperty("oldestTimestamp") long oldest,
                           @JsonProperty("latestTimestamp") long latest) {
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
    public long getOldest() {
        return oldest;
    }

    @JsonProperty("latestTimestamp")
    public long getLatest() {
        return latest;
    }
}
