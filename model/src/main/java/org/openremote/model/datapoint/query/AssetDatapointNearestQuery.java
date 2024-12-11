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
		return "WITH after_row AS ( " +
				"    SELECT * " +
				"    FROM " + tableName + " " +
				"    WHERE entity_id = ? AND attribute_name = ? " +
				"      AND timestamp >= to_timestamp(?) " +
				"    ORDER BY timestamp ASC " +
				"    LIMIT 1 " +
				"), before_row AS ( " +
				"    SELECT * " +
				"    FROM " + tableName + " " +
				"    WHERE entity_id = ? AND attribute_name = ? " +
				"      AND timestamp < to_timestamp(?) " +
				"    ORDER BY timestamp DESC " +
				"    LIMIT 1 " +
				") " +
				"SELECT * FROM ( " +
				"    SELECT *, ABS(EXTRACT(EPOCH FROM (timestamp - to_timestamp(?)))) AS diff FROM after_row " +
				"    UNION ALL " +
				"    SELECT *, ABS(EXTRACT(EPOCH FROM (timestamp - to_timestamp(?)))) AS diff FROM before_row " +
				") combined " +
				"ORDER BY diff LIMIT 1;";
	}
	@Override
	public HashMap<Integer, Object> getSQLParameters(AttributeRef attributeRef) {
		HashMap<Integer, Object> parameters = new HashMap<>();
		// after_row parameters
		parameters.put(1, attributeRef.getId());
		parameters.put(2, attributeRef.getName());
		parameters.put(3, this.fromTimestamp);

		// before_row parameters
		parameters.put(4, attributeRef.getId());
		parameters.put(5, attributeRef.getName());
		parameters.put(6, this.fromTimestamp);

		// diff calculations in the final SELECT
		parameters.put(7, this.fromTimestamp);
		parameters.put(8, this.fromTimestamp);

		return parameters;
	}
}
