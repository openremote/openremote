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

    public String getSQLQuery(String tableName, Class<?> attributeType) {
        System.out.println("getSQLQuery() of AssetDatapointAllQuery."); // temp
        boolean isNumber = Number.class.isAssignableFrom(attributeType);
        if (isNumber) {
            return "select timestamp, value::text::numeric from " + tableName + " where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ?";
        } else {
            return "select timestamp, (case when VALUE::text::boolean is true then 1 else 0 end) from " + tableName + " where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ?";
        }
    }
    public HashMap<Integer, Object> getSQLParameters(AttributeRef attributeRef) {
        System.out.println("getSQLParemeters() of AssetDatapointAllQuery."); // temp
        HashMap<Integer, Object> parameters = new HashMap<>();
        LocalDateTime fromTimestamp = (this.fromTime != null) ? this.fromTime : LocalDateTime.ofInstant(Instant.ofEpochMilli(this.fromTimestamp), ZoneId.systemDefault());
        LocalDateTime toTimestamp = (this.toTime != null) ? this.toTime : LocalDateTime.ofInstant(Instant.ofEpochMilli(this.toTimestamp), ZoneId.systemDefault());
        parameters.put(1, attributeRef.getId());
        parameters.put(2, attributeRef.getName());
        parameters.put(3, fromTimestamp);
        parameters.put(4, toTimestamp);
        return parameters;
    }
}
