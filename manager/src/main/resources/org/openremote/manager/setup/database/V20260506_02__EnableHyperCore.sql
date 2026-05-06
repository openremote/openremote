-- FAIL FAST IF THE DB IMAGE DOES NOT SUPPORT HYPERCORE
DO $$
    DECLARE
        ts_version text;
        has_hypercore_policy_proc boolean;
    BEGIN
        -- 1. EXISTING VERSION CHECKS
        SELECT extversion INTO ts_version FROM pg_extension WHERE extname = 'timescaledb';

        IF ts_version IS NULL THEN
            RAISE EXCEPTION 'TimescaleDB extension is missing.';
        END IF;

        -- 2. SAFE REMOVAL (Sub-blocks for error handling)
        -- This "nests" the logic within your existing block
        BEGIN
            -- remove_compression_policy is a FUNCTION
            -- Use PERFORM to call a function inside PL/pgSQL
            PERFORM public.remove_compression_policy('${schemaName}.asset_datapoint');
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Could not remove legacy policy: %', SQLERRM;
        END;

        BEGIN
            -- remove_columnstore_policy is a PROCEDURE
            -- Use EXECUTE 'CALL...' for maximum compatibility
            EXECUTE 'CALL public.remove_columnstore_policy(''${schemaName}.asset_datapoint'', if_exists => true)';
        EXCEPTION WHEN OTHERS THEN
            RAISE NOTICE 'Could not remove columnstore policy: %', SQLERRM;
        END;

    END $$;

-- 3. ENABLE COLUMNSTORE (Must be outside DO block if it requires its own transaction)
ALTER TABLE ${schemaName}.asset_datapoint SET (
    timescaledb.enable_columnstore = true,
    timescaledb.orderby = 'timestamp DESC',
    timescaledb.segmentby = 'entity_id,attribute_name',
    timescaledb.chunk_interval = '7 days');

-- ACTIVATE THE AUTOMATED POLICY
CALL public.add_columnstore_policy('${schemaName}.asset_datapoint', after => INTERVAL '7 days');
