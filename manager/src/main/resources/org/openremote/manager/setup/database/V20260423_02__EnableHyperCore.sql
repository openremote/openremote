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

