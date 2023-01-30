create extension if not exists timescaledb;
create extension if not exists timescaledb_toolkit; /*Not sure whether this is required*/
SELECT create_hypertable('ASSET_DATAPOINT', 'timestamp');
