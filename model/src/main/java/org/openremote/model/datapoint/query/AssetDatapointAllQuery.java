package org.openremote.model.datapoint.query;

import org.openremote.model.attribute.AttributeRef;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;

public class AssetDatapointAllQuery extends AssetDatapointQuery {

    public AssetDatapointAllQuery() {}
    public AssetDatapointAllQuery(long fromTimestamp, long toTimestamp) {
        this.fromTimestamp = fromTimestamp;
        this.toTimestamp = toTimestamp;
    }
    public AssetDatapointAllQuery(LocalDateTime fromTime, LocalDateTime toTime) {
        this.fromTime = fromTime;
        this.toTime = toTime;
    }

    public String getSQLQuery(String tableName, Class<?> attributeType) throws IllegalStateException {
        boolean isNumber = Number.class.isAssignableFrom(attributeType);
        boolean isBoolean = Boolean.class.isAssignableFrom(attributeType);
        String conditionSuffix = " where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? order by timestamp desc";
        if (isNumber) {
            return "select timestamp as X, cast(value as numeric) as Y from " + tableName + conditionSuffix;
        } else if (isBoolean) {
            return "select timestamp as X, (case when cast(cast(value as text) as boolean) is true then 1 else 0 end) as Y from " + tableName + conditionSuffix;
        } else {
            return "select distinct timestamp as X, value as Y from " + tableName + conditionSuffix;
        }
    }

    public HashMap<Integer, Object> getSQLParameters(AttributeRef attributeRef) {
        HashMap<Integer, Object> parameters = new HashMap<>();
        LocalDateTime fromTimestamp = (this.fromTime != null) ? this.fromTime : Instant.ofEpochMilli(this.fromTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime toTimestamp = (this.toTime != null) ? this.toTime : Instant.ofEpochMilli(this.toTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
        parameters.put(1, attributeRef.getId());
        parameters.put(2, attributeRef.getName());
        parameters.put(3, fromTimestamp);
        parameters.put(4, toTimestamp);
        return parameters;
    }
}
