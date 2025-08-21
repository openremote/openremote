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
        MIN, AVG, MAX, DIFFERENCE, COUNT, SUM, MODE, MEDIAN
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
        if (!isNumber && !isBoolean) {
            throw new IllegalStateException("Query of type Interval requires either a number or a boolean attribute.");
        }

        String function = (gapFill ? "public.time_bucket_gapfill" : "public.time_bucket");
        return isNumber ? getSQLQueryForNumbers(tableName, function) : getSQLQueryForBooleans(tableName, function);
    }

    protected String getSQLQueryForNumbers(String tableName, String function) {
        switch (this.formula) {
            case DIFFERENCE:
                return "WITH interval_data AS (" +
                        "SELECT " + function +
                        "(cast(? as interval), timestamp) AS x, public.locf(public.last(value::DOUBLE PRECISION, timestamp)) AS numeric_value " +
                        "FROM " + tableName + " " + "WHERE ENTITY_ID = ? AND ATTRIBUTE_NAME = ? AND TIMESTAMP >= ? AND TIMESTAMP <= ? GROUP BY x )" +
                        "SELECT x, COALESCE(numeric_value - LAG(numeric_value, 1, numeric_value) OVER (ORDER BY x), numeric_value) AS delta FROM interval_data ORDER BY x ASC";
            case COUNT:
                return "SELECT " + function + "(cast(? as interval), timestamp) AS x, " +
                        "COUNT(*) AS datapoint_count " +
                        "FROM " + tableName + " " +
                        "WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? " +
                        "GROUP BY x ORDER by x ASC";
            case SUM:
                return "SELECT " + function + "(cast(? as interval), timestamp) AS x, " +
                        "SUM(cast(value as numeric)) AS total_sum " +
                        "FROM " + tableName + " " +
                        "WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? " +
                        "GROUP BY x ORDER by x ASC";
            case MODE:
                return "WITH bucketed AS ( " +
                        "  SELECT " + function + "(cast(? as interval), timestamp) AS x, " +
                        "         cast(value as numeric) AS num_value " +
                        "  FROM " + tableName + " " +
                        "  WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? " +
                        ") " +
                        "SELECT d.x, ( " +
                        "    SELECT num_value FROM ( " +
                        "         SELECT num_value, COUNT(*) AS freq " +
                        "         FROM bucketed b2 " +
                        "         WHERE b2.x = d.x " +
                        "         GROUP BY num_value " +
                        "         ORDER BY freq DESC, num_value " +
                        "         LIMIT 1 " +
                        "    ) AS mode_sub " +
                        ") AS mode_value " +
                        "FROM (SELECT DISTINCT x FROM bucketed) d " +
                        "ORDER BY d.x ASC";
            case MEDIAN:
                return "SELECT " + function + "(cast(? as interval), timestamp) AS x, " +
                        "PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY cast(value as numeric)) as median_value " +
                        "FROM " + tableName + " " +
                        "WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? " +
                        "GROUP BY x ORDER by x ASC";
            default:
                return "SELECT " + function +
                         "(cast(? as interval), timestamp) AS x, " + this.formula.toString().toLowerCase() +
                         "(cast(value as numeric)) FROM " + tableName +
                         " WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? GROUP BY x ORDER by x ASC";
        }
    }

    protected String getSQLQueryForBooleans(String tableName, String function) {
        switch (this.formula) {
            case DIFFERENCE:
                throw new IllegalStateException("Query of type DIFFERENCE is not applicable for boolean attributes.");
            case COUNT:
                return "SELECT " + function + "(cast(? as interval), timestamp) AS x, " +
                        "COUNT(*) AS datapoint_count " +
                        "FROM " + tableName + " " +
                        "WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? " +
                        "GROUP BY x ORDER by x ASC";
            case SUM:
                return  "SELECT " + function + "(cast(? as interval), timestamp) AS x, " +
                        "SUM(CASE WHEN cast(value as text)::boolean THEN 1 ELSE 0 END) AS true_count_sum " +
                        "FROM " + tableName + " " +
                        "WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? " +
                        "GROUP BY x ORDER by x ASC";
            case MODE:
                return "WITH bucketed AS ( " +
                        "   SELECT " + function + "(cast(? as interval), timestamp) AS x, " +
                        "          cast(value as text)::boolean AS bool_value " +
                        "   FROM " + tableName + " " +
                        "   WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? " +
                        ") " +
                        "SELECT d.x, ( " +
                        "    SELECT bool_value FROM ( " +
                        "         SELECT bool_value, COUNT(*) AS freq " +
                        "         FROM bucketed b2 " +
                        "         WHERE b2.x = d.x " +
                        "         GROUP BY bool_value " +
                        "         ORDER BY freq DESC, bool_value DESC " +
                        "         LIMIT 1 " +
                        "    ) AS mode_sub " +
                        ") AS mode_value " +
                        "FROM (SELECT DISTINCT x FROM bucketed) d " +
                        "ORDER BY d.x ASC";
            case MEDIAN:
                return "SELECT " + function + "(cast(? as interval), timestamp) AS x, " +
                        "CASE WHEN PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY (CASE WHEN cast(value as text)::boolean THEN 1 ELSE 0 END)) >= 0.5 " +
                        "THEN true ELSE false END AS median_value " +
                        "FROM " + tableName + " " +
                        "WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? " +
                        "GROUP BY x ORDER by x ASC";
            default:
                return "SELECT " + function + "(cast(? as interval), timestamp) AS x, " + this.formula.toString().toLowerCase() + "(case when cast(cast(value as text) as boolean) is true then 1 else 0 end) FROM " + tableName + " WHERE ENTITY_ID = ? and ATTRIBUTE_NAME = ? and TIMESTAMP >= ? and TIMESTAMP <= ? GROUP BY x ORDER by x ASC";
        }
    }

    @Override
    public HashMap<Integer, Object> getSQLParameters(AttributeRef attributeRef) {
        HashMap<Integer, Object> parameters = new HashMap<>();
        LocalDateTime fromTimestamp = (this.fromTime != null) ? this.fromTime : Instant.ofEpochMilli(this.fromTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime toTimestamp = (this.toTime != null) ? this.toTime : Instant.ofEpochMilli(this.toTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
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
