# Query Exporter Configuration for OpenRemote

This directory contains the configuration for [query-exporter](https://github.com/albertodonato/query-exporter), a Prometheus exporter that collects metrics from PostgreSQL queries.

## Overview

The query-exporter service monitors the OpenRemote PostgreSQL database and exposes metrics on port 9560 (localhost by default).

## Metrics Collected

### 1. Table and Index Bloat Metrics
- **`pg_table_bloat_count`**: Number of tables/indexes with bloat exceeding configured thresholds
- **`pg_table_bloat_ratio`**: Bloat ratio per table/index (1.0 = no bloat, 2.0 = 100% bloat)
- **`pg_table_bloat_bytes`**: Estimated bloat size in bytes per table/index
- **`pg_table_bloat_wasted_mb`**: Estimated wasted space in megabytes per table/index

These metrics help identify tables and indexes that need VACUUM FULL, REINDEX, or other maintenance. Metrics include a `bloat_type` label to distinguish between 'table' and 'index' bloat.

### 2. Autovacuum Worker Metrics
- **`pg_autovacuum_workers_active`**: Number of currently active autovacuum workers
- **`pg_autovacuum_workers_max`**: Maximum number of autovacuum workers configured
- **`pg_autovacuum_running`**: Detailed information about running autovacuum processes (with labels: database, table_schema, table_name, phase)

These metrics help monitor autovacuum activity and identify potential bottlenecks.

### 3. Datapoint Query Performance Metrics
- **`pg_datapoint_query_duration_seconds`**: Histogram of execution times for queries on the attribute with the most datapoints (samples 100 recent datapoints)
- **`pg_datapoint_count`**: Total number of datapoints for the top attribute

These metrics help identify performance issues with the most heavily used attributes. The query samples the 100 most recent datapoints to minimize performance impact.

### 4. Additional Metrics
- **`pg_database_size_megabytes`**: Total database size in megabytes
- **`pg_connections_active`**: Number of active connections
- **`pg_connections_idle`**: Number of idle connections
- **`pg_locks_count`**: Number of locks by type

## Configuration

The configuration file `config.yaml` uses environment variables for database connection and thresholds:

### Database Connection
- `POSTGRES_HOST` - Database host (default: postgresql)
- `POSTGRES_PORT` - Database port (default: 5432)
- `POSTGRES_DB` - Database name (default: openremote)
- `POSTGRES_USER` - Database user (default: postgres)
- `POSTGRES_PASSWORD` - Database password (default: postgres)

### Bloat Detection Thresholds
- `TABLE_BLOAT_THRESHOLD` - Table bloat ratio threshold (default: 1.2 = 20% bloat)
- `INDEX_BLOAT_THRESHOLD` - Index bloat ratio threshold (default: 1.5 = 50% bloat)

These are automatically set from the OpenRemote environment variables in `deploy.yml`.

**Note**: Indexes typically bloat faster than tables, so the default index threshold is higher (1.5 vs 1.2).

**Customizing thresholds**: Set these environment variables before starting the services:
```bash
export TABLE_BLOAT_THRESHOLD=1.3  # 30% table bloat
export INDEX_BLOAT_THRESHOLD=2.0  # 100% index bloat
```

## Query Intervals

- **Table bloat queries**: Every 5 minutes (300s)
- **Autovacuum queries**: Every 30 seconds
- **Datapoint performance**: Every 60 seconds
- **Database size**: Every 5 minutes
- **Connection/lock stats**: Every 30 seconds

## Accessing Metrics

Metrics are exposed at: `http://localhost:9560/metrics`

**Note**: In `dev-testing.yml`, the port is exposed as `9560:9560` for easy access. In production (`deploy.yml`), it's bound to `127.0.0.1:9560:9560` by default for security.

To expose on a private network in production, uncomment the appropriate line in `deploy.yml`:
```yaml
# - "${PRIVATE_IP:-127.0.0.1}:9560:9560"
```

## Integration with Prometheus

Add the following to your Prometheus configuration:

```yaml
scrape_configs:
  - job_name: 'openremote-postgres'
    static_configs:
      - targets: ['localhost:9560']
    scrape_interval: 30s
```

## Customization

To modify queries or add new metrics:

1. Edit `config.yaml`
2. Restart the query-exporter service:
   ```bash
   docker-compose restart query-exporter
   ```

## Troubleshooting

### Check service logs
```bash
docker-compose logs -f query-exporter
```

### Test database connectivity
```bash
docker-compose exec query-exporter sh
# Inside container:
apk add postgresql-client
psql -h $POSTGRES_HOST -U $POSTGRES_USER -d $POSTGRES_DB
```

### Verify metrics endpoint
```bash
curl http://localhost:9560/metrics
```

## Performance Considerations

The bloat detection queries are computationally expensive. If they impact database performance:
- Increase the interval (e.g., from 300s to 600s or higher)
- Limit the queries to specific schemas
- Run during off-peak hours using the `schedule` option instead of `interval`
- Reduce the datapoint query sample size (currently limited to 100 datapoints)

### Query Complexity Notes
- **Bloat detection**: Scans `pg_stats` and `pg_class` catalogs, limited to top 50 results
- **Datapoint performance**: Samples 100 most recent datapoints from the largest attribute
- All queries exclude PostgreSQL system schemas (`pg_%` and `information_schema`)

## Understanding Bloat Metrics

### Bloat Ratio Interpretation
- **1.0** = No bloat (optimal size)
- **1.2** = 20% bloat (default table threshold)
- **1.5** = 50% bloat (default index threshold)
- **2.0** = 100% bloat (object is twice the optimal size)

### When to Take Action
- **Tables > 1.2**: Consider running `VACUUM FULL` during maintenance window
- **Indexes > 1.5**: Consider running `REINDEX` on affected indexes
- **Critical bloat (> 2.0)**: Immediate maintenance recommended

### Hardcoded Constants in Queries
The bloat detection queries use PostgreSQL internal constants:
- **1048576** = Bytes per megabyte (1024 × 1024)
- **8** = Bits per byte (for null bitmap calculation)
- **20** = Page header size in bytes
- **12** = Index header overhead in bytes
- **4** = Item pointer size in bytes
- **23** = Tuple header size for PostgreSQL 14+ (Linux)
- **4** = Memory alignment for Linux containers

## References

- [Query Exporter Documentation](https://github.com/albertodonato/query-exporter)
- [Configuration Format](https://github.com/albertodonato/query-exporter/blob/main/docs/configuration.rst)
- [PostgreSQL Statistics Views](https://www.postgresql.org/docs/current/monitoring-stats.html)
- [PostgreSQL Bloat Detection](https://wiki.postgresql.org/wiki/Show_database_bloat)
