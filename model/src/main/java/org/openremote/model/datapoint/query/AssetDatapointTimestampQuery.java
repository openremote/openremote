package org.openremote.model.datapoint.query;

import org.openremote.model.attribute.AttributeRef;

import java.util.HashMap;

/**
 * This `AssetDatapointQuery` retrieves the value of a datapoint
 * associated with a specific `AttributeRef` at a specified timestamp. It is designed to find the
 * closest datapoint that precedes the requested timestamp.
 */
public class AssetDatapointTimestampQuery extends AssetDatapointQuery {

    public AssetDatapointTimestampQuery() {
    }

    public AssetDatapointTimestampQuery(long timestamp) {
        // Convert to seconds from millis
        this.fromTimestamp = timestamp/1000;
    }

    @Override
    public String getSQLQuery(String tableName, Class<?> attributeType) throws IllegalStateException {
        return "WITH nearest_row AS ( " +
                "    SELECT * " +
                "    FROM " + tableName + " " +
                "    WHERE entity_id = ? AND attribute_name = ? " +
                "      AND timestamp <= to_timestamp(?) " +
                "    ORDER BY timestamp DESC " +
                "    LIMIT 1 " +
                ") " +
                "select timestamp as X, value::text::numeric as Y FROM nearest_row;";
    }
    @Override
    public HashMap<Integer, Object> getSQLParameters(AttributeRef attributeRef) {
        HashMap<Integer, Object> parameters = new HashMap<>();
        // nearest_row parameters
        parameters.put(1, attributeRef.getId());
        parameters.put(2, attributeRef.getName());
        parameters.put(3, this.fromTimestamp);

        return parameters;
    }
}