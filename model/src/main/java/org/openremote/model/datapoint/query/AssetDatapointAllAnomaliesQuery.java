package org.openremote.model.datapoint.query;

import org.openremote.model.attribute.AttributeRef;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;

public class AssetDatapointAllAnomaliesQuery extends AssetDatapointQuery {

    public AssetDatapointAllAnomaliesQuery() {}
    public AssetDatapointAllAnomaliesQuery(long fromTimestamp, long toTimestamp) {
        this.fromTimestamp = fromTimestamp;
        this.toTimestamp = toTimestamp;
    }
    public AssetDatapointAllAnomaliesQuery(LocalDateTime fromTime, LocalDateTime toTime) {
        this.fromTime = fromTime;
        this.toTime = toTime;
    }

    public String getSQLQuery(String tableName, Class<?> attributeType) throws IllegalStateException {
        boolean isNumber = Number.class.isAssignableFrom(attributeType);
        boolean isBoolean = Boolean.class.isAssignableFrom(attributeType);
        if (isNumber) {
            return "select timestamp as X, value::text::numeric as Y from " + tableName +"\n" +
                    "where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ?\n" +
                    "and "+tableName+ ".timestamp in(\n" +
                    "select timestamp from asset_anomaly\n" +
                    "where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? and anomaly_type != 1 and anomaly_type != 0) order by timestamp desc";
        } else if (isBoolean) {
            return "select timestamp as X, (case when VALUE::text::boolean is true then 1 else 0 end) as Y from " + tableName +"\n" +
                    "where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ?\n" +
                    "and "+tableName+ ".timestamp in(\n" +
                    "select timestamp from asset_anomaly\n" +
                    "where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? and anomaly_type != 1 and anomaly_type != 0) order by timestamp desc";
        } else {
            return "select distinct timestamp as X, value as Y from " + tableName +"\n" +
                    "where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ?\n" +
                    "and "+tableName+ ".timestamp in(\n" +
                    "select timestamp from asset_anomaly\n" +
                    "where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? and anomaly_type != 1 and anomaly_type != 0) order by timestamp desc";
        }
    }

    public HashMap<Integer, Object> getSQLParameters(AttributeRef attributeRef) {
        HashMap<Integer, Object> parameters = new HashMap<>();
        LocalDateTime fromTimestamp = (this.fromTime != null) ? this.fromTime : LocalDateTime.ofInstant(Instant.ofEpochMilli(this.fromTimestamp), ZoneId.systemDefault());
        LocalDateTime toTimestamp = (this.toTime != null) ? this.toTime : LocalDateTime.ofInstant(Instant.ofEpochMilli(this.toTimestamp), ZoneId.systemDefault());
        parameters.put(1, attributeRef.getId());
        parameters.put(2, attributeRef.getName());
        parameters.put(3, fromTimestamp);
        parameters.put(4, toTimestamp);
        parameters.put(5, attributeRef.getId());
        parameters.put(6, attributeRef.getName());
        parameters.put(7, fromTimestamp);
        parameters.put(8, toTimestamp);
        return parameters;
    }
}
