package org.openremote.model.datapoint.query;

import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.DatapointInterval;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;

public class AssetDatapointIntervalQuery extends AssetDatapointQuery {

    public String interval;
    public boolean gapFill;
    public Formula formula;

    public enum Formula {
        MIN, AVG, MAX
    }

    public AssetDatapointIntervalQuery() {
    }
    public AssetDatapointIntervalQuery(long fromTimestamp, long toTimestamp, String interval, Formula formula, boolean gapFill) {
        this.fromTimestamp = fromTimestamp;
        this.toTimestamp = toTimestamp;
        this.interval = formatInterval(interval);
        this.gapFill = gapFill;
        this.formula = formula;
    }
    public AssetDatapointIntervalQuery(LocalDateTime fromTime, LocalDateTime toTime, String interval, Formula formula, boolean gapFill) {
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.interval = formatInterval(interval);
        this.gapFill = gapFill;
        this.formula = formula;
    }

    @Override
    public String getSQLQuery(String tableName, Class<?> attributeType) throws IllegalStateException {
        boolean isNumber = Number.class.isAssignableFrom(attributeType);
        boolean isBoolean = Boolean.class.isAssignableFrom(attributeType);
        String function = (gapFill ? "public.time_bucket_gapfill" : "public.time_bucket");
        if (isNumber) {
            return "select " + function + "(?::interval, timestamp) AS x, " + this.formula.toString().toLowerCase() + "(value::text::numeric) FROM " + tableName + " WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? GROUP BY x;";
        } else if (isBoolean) {
            return "select " + function + "(?::interval, timestamp) AS x, " + this.formula.toString().toLowerCase() + "(case when VALUE::text::boolean is true then 1 else 0 end) FROM " + tableName + " WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? GROUP BY x;";
        } else {
            throw new IllegalStateException("Query of type Interval requires either a number or a boolean attribute.");
        }
    }

    @Override
    public HashMap<Integer, Object> getSQLParameters(AttributeRef attributeRef) {
        HashMap<Integer, Object> parameters = new HashMap<>();
        LocalDateTime fromTimestamp = (this.fromTime != null) ? this.fromTime :  LocalDateTime.ofInstant(Instant.ofEpochMilli(super.fromTimestamp), ZoneId.systemDefault());
        LocalDateTime toTimestamp = (this.toTime != null) ? this.toTime :  LocalDateTime.ofInstant(Instant.ofEpochMilli(super.toTimestamp), ZoneId.systemDefault());
        parameters.put(1, this.interval);
        parameters.put(2, attributeRef.getId());
        parameters.put(3, attributeRef.getName());
        parameters.put(4, fromTimestamp);
        parameters.put(5, toTimestamp);
        return parameters;
    }

    // Method that makes sure the interval is correctly formatted.
    // The AssetDatapointIntervalQuery requires to specify an amount such as "1 day" or "5 hours",
    // so adding an amount automatically if only a DatapointInterval such as "MINUTE" or "YEAR" is specified.
    protected String formatInterval(String interval) {
        boolean hasAmountSpecified = Arrays.stream(DatapointInterval.values()).noneMatch((dpInterval) -> dpInterval.toString().equalsIgnoreCase(interval));
        if(hasAmountSpecified) {
            return interval;
        } else {
            return "1 " + interval;
        }
    }
}
