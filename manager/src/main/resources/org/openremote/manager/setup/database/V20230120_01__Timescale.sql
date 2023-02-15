SELECT public.create_hypertable('asset_datapoint', 'timestamp', if_not_exists := true, migrate_data := true);
SELECT public.create_hypertable('asset_predicted_datapoint', 'timestamp', if_not_exists := true, migrate_data := true);
