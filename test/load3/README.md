# TimescaleDB Benchmark

This script is an automated load-generator designed to benchmark query performance on a TimescaleDB instance; it can be used to compare performance before and after a DB configuration change.

## What it does
Instead of testing generic, hardcoded queries, this script dynamically adapts to your actual data:
1. **Dynamic Target Acquisition:** It queries the `asset_datapoint` table to find the 10 entities and attributes with the highest volume of numeric data over the last 3 days.
2. **Heavy Load Generation:** For each of those 10 targets, it executes three realistic, heavy time-series queries:
   * A 7-day hourly average.
   * A 14-day Largest Triangle Three Buckets (LTTB) visual downsample (500 points).
   * A 30-day daily Min/Max/Avg calculation.
3. **Pure Execution Timing:** It wraps every query in an `EXPLAIN ANALYZE` command to extract the exact millisecond execution time directly from the PostgreSQL engine, completely eliminating Docker or network latency from the benchmark.
4. **Excel-Ready Output:** It formats the results into a self-describing CSV file (`results.csv`), making it trivial to generate charts in Excel or Grafana.

## Prerequisites
* The script must be run on the host machine running the PostgreSQL Docker container.
* The TimescaleDB instance must have the `timescaledb_toolkit` extension installed (required for the `lttb()` downsampling function).
* The default Docker container name is expected to be `or-postgresql-1`. (This can be changed at the top of the script).

## How to Run

1. Make the script executable:
    `chmod +x benchmark.sh`

2. Execute the script:
    `./benchmark.sh`

## Customization
You can easily adjust the behavior of the script by editing the configuration block at the top of `benchmark.sh`:
* `ITERATIONS`: Controls how many times the full suite of queries runs (Default is `5`).
* `CONTAINER_NAME`: Update this if your OpenRemote database container has a different name.

## Understanding the Output (`results.csv`)
The script generates a CSV file where the first column is the exact ISO8601 timestamp of when the iteration began.

The subsequent columns contain the query execution times in milliseconds. The headers are dynamically generated using a combination of the Entity ID (first 4 characters), Attribute Name, and Query Type.

**Example Header:** `4hOF-kwMax_Hourly_Avg_7d_ms`
* `4hOF`: First 4 characters of the `entity_id`
* `kwMax`: The `attribute_name`
* `Hourly_Avg_7d`: The query template executed
* `ms`: Value is measured in milliseconds

To visualize your benchmark, simply open `results.csv` in Excel, select all data, and insert a Line Chart. The headers will automatically generate a perfectly formatted legend.
