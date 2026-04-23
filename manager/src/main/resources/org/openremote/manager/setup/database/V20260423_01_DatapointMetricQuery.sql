CREATE OR REPLACE FUNCTION get_benchmark_historical_aggregation()
RETURNS integer AS $$
DECLARE
target_entity text;
    target_attr text;
    result_count integer;
BEGIN

-- 1. Find the heaviest entity and attribute safely
SELECT entity_id, attribute_name
INTO target_entity, target_attr
FROM asset_datapoint
WHERE timestamp > now() - INTERVAL '1 day'
  AND jsonb_typeof(value) = 'number'
GROUP BY entity_id, attribute_name
ORDER BY count(*) DESC
    LIMIT 1;

-- 2. Run the heavy aggregation using dynamic SQL.
-- The %L formatting injects the variables as literal strings,
-- tricking the query planner into using the lightning-fast Index Only Scans!
EXECUTE format('
        WITH aggregated_points AS (
            SELECT
                time_bucket(''1 hour'', timestamp) AS bucket,
                avg((value::text)::numeric) AS avg_value
            FROM asset_datapoint
            WHERE entity_id = %L
              AND attribute_name = %L
              AND timestamp BETWEEN now() - INTERVAL ''30 days'' AND now() - INTERVAL ''7 days''
            GROUP BY bucket
        )
        SELECT count(bucket) FROM aggregated_points;
    ', target_entity, target_attr) INTO result_count;

-- 3. Return the final row count to the exporter
RETURN result_count;
END;
$$ LANGUAGE plpgsql;
