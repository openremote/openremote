package org.openremote.model.datapoint.query;

import org.openremote.model.attribute.AttributeRef;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;

public class AssetDatapointNearestQuery extends AssetDatapointQuery {

	public AssetDatapointNearestQuery() {
	}

	public AssetDatapointNearestQuery(long timestamp) {
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
