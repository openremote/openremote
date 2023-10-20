package org.openremote.model.datapoint.query;

import org.openremote.model.attribute.AttributeRef;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;

public final class AssetDatapointLTTBQuery extends AssetDatapointQuery {

    public int amountOfPoints;

    public AssetDatapointLTTBQuery() {}
    public AssetDatapointLTTBQuery(long fromTimestamp, long toTimestamp, int amountOfPoints) {
        this.fromTimestamp = fromTimestamp;
        this.toTimestamp = toTimestamp;
        this.amountOfPoints = amountOfPoints;
    }
    public AssetDatapointLTTBQuery(LocalDateTime fromTime, LocalDateTime toTime, int amountOfPoints) {
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.amountOfPoints = amountOfPoints;
    }

    @Override
    public String getSQLQuery(String tableName, Class<?> attributeType) throws IllegalStateException {
        boolean isNumber = Number.class.isAssignableFrom(attributeType);
        boolean isBoolean = Boolean.class.isAssignableFrom(attributeType);
        if (isNumber) {
            return "select * from public.unnest((select public.lttb(timestamp::timestamptz, value::double precision, ?) from " + tableName + " where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ?))";
        } else if (isBoolean) {
            return "select * from public.unnest((select public.lttb(timestamp::timestamptz, (case when VALUE::text::boolean is true then 1 else 0 end), ?) from " + tableName + " where ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ?))";
        } else {
            throw new IllegalStateException("Query of type LTTB requires either a number or a boolean attribute.");
        }
    }

    @Override
    public HashMap<Integer, Object> getSQLParameters(AttributeRef attributeRef) {
        LocalDateTime fromTimestamp = (this.fromTime != null) ? this.fromTime : LocalDateTime.ofInstant(Instant.ofEpochMilli(super.fromTimestamp), ZoneId.systemDefault());
        LocalDateTime toTimestamp = (this.toTime != null) ? this.toTime : LocalDateTime.ofInstant(Instant.ofEpochMilli(super.toTimestamp), ZoneId.systemDefault());
        HashMap<Integer, Object> parameters = new HashMap<>();
        parameters.put(1, this.amountOfPoints);
        parameters.put(2, attributeRef.getId());
        parameters.put(3, attributeRef.getName());
        parameters.put(4, fromTimestamp);
        parameters.put(5, toTimestamp);
        return parameters;
    }
}
