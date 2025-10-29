-- Set retention policy for asset_datapoint table
DO $$
DECLARE
    retention_interval TEXT;
    retention_interval_predicted TEXT;
    retention_sql TEXT;
BEGIN
    -- Get the retention period for asset_datapoint from environment variable
    SELECT current_setting('or.asset_datapoint_retention', true) INTO retention_interval;
    
    -- If the environment variable is set, create or update the retention policy
    IF retention_interval IS NOT NULL AND retention_interval != '' THEN
        retention_sql := format(
            'SELECT add_retention_policy(''asset_datapoint'', INTERVAL %L);',
            retention_interval
        );
        EXECUTE retention_sql;
        RAISE NOTICE 'Set retention policy for asset_datapoint to %', retention_interval;
    ELSE
        RAISE NOTICE 'asset_datapoint_retention not set, skipping asset_datapoint retention policy';
    END IF;

    -- Get the retention period for asset_predicted_datapoint from environment variable
    SELECT current_setting('or.asset_predicted_datapoint_retention', true) INTO retention_interval_predicted;
    
    -- If the environment variable is set, create or update the retention policy
    IF retention_interval_predicted IS NOT NULL AND retention_interval_predicted != '' THEN
        retention_sql := format(
            'SELECT add_retention_policy(''asset_predicted_datapoint'', INTERVAL %L);',
            retention_interval_predicted
        );
        EXECUTE retention_sql;
        RAISE NOTICE 'Set retention policy for asset_predicted_datapoint to %', retention_interval_predicted;
    ELSE
        RAISE NOTICE 'asset_predicted_datapoint_retention not set, skipping asset_predicted_datapoint retention policy';
    END IF;
END
$$;
