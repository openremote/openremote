-- FAIL FAST IF THE DB IMAGE DOES NOT SUPPORT HYPERCORE
DO $$
DECLARE
    ts_version text;
    has_hypercore_policy_proc boolean;
BEGIN
    SELECT extversion
    INTO ts_version
    FROM pg_extension
    WHERE extname = 'timescaledb';

    IF ts_version IS NULL THEN
        RAISE EXCEPTION
            'Cannot enable Hypercore for %.asset_datapoint: timescaledb extension is missing. Use a DB image with TimescaleDB.',
            '${schemaName}';
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM pg_proc p
        JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'public'
          AND p.proname = 'add_columnstore_policy'
    )
    INTO has_hypercore_policy_proc;

    IF NOT has_hypercore_policy_proc THEN
        RAISE EXCEPTION
            'Cannot enable Hypercore for %.asset_datapoint: TimescaleDB version % does not provide add_columnstore_policy. Upgrade DB image.',
            '${schemaName}', ts_version;
    END IF;
END $$;

-- 1. DROP ANY EXISTING LEGACY POLICY
SELECT remove_compression_policy('asset_datapoint');

-- ENABLE COLUMNSTORE
ALTER TABLE ${schemaName}.asset_datapoint SET (
    timescaledb.enable_columnstore = true,
    timescaledb.orderby = 'timestamp DESC',
    timescaledb.segmentby = 'entity_id,attribute_name',
    timescaledb.chunk_interval = '7 days');

-- ACTIVATE THE AUTOMATED POLICY
CALL public.add_columnstore_policy('asset_datapoint', after => INTERVAL '7 days');

