create extension if not exists timescaledb;
SELECT create_hypertable('asset_datapoint', 'timestamp', if_not_exists := true);
SELECT create_hypertable('asset_predicted_datapoint', 'timestamp', if_not_exists := true);
