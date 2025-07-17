-- Enable compression on asset_datapoint table
ALTER TABLE openremote.asset_datapoint SET (timescaledb.compress, timescaledb.compress_segmentby = 'entity_id, attribute_name');

-- Add compression policy to compress data older than 1 month
SELECT public.add_compression_policy('openremote.asset_datapoint', INTERVAL '1 months');
