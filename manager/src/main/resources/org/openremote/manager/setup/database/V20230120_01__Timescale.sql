create extension if not exists timescaledb;
SELECT create_hypertable('asset_datapoint', 'timestamp');
SELECT create_hypertable('asset_predicted_datapoint', 'timestamp');
