#!/bin/bash

# Configuration
CONTAINER_NAME="or-postgresql-1"
DB_USER="postgres"
DB_NAME="openremote"
CSV_FILE="results.csv"
ITERATIONS=5

function run_psql() {
    docker exec "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c "$1"
}

echo "Starting Benchmark Initialization..."

# 1. Find the top 10 entity_id & attribute_name values
TOP_ENTITIES_QUERY="
SELECT entity_id, attribute_name
FROM asset_datapoint
WHERE timestamp > now() - INTERVAL '3 days'
  AND jsonb_typeof(value) = 'number'
GROUP BY entity_id, attribute_name
ORDER BY count(*) DESC
LIMIT 10;
"

echo "Fetching top 10 entities..."
ENTITIES=$(run_psql "$TOP_ENTITIES_QUERY")

if [ -z "$ENTITIES" ]; then
    echo "Error: No numeric entities found in the last 3 days. Exiting."
    exit 1
fi

# 2. Define the Query Templates
QUERY_TEMPLATES=(
    "SELECT time_bucket('1 hour', timestamp) as bucket, avg((value::text)::numeric) FROM asset_datapoint WHERE entity_id = '%s' AND attribute_name = '%s' AND timestamp > now() - INTERVAL '7 days' GROUP BY bucket;"
    "SELECT time, value FROM unnest((SELECT lttb(timestamp, (value::text)::numeric, 500) FROM asset_datapoint WHERE entity_id = '%s' AND attribute_name = '%s' AND timestamp > now() - INTERVAL '14 days'));"
    "SELECT time_bucket('1 day', timestamp) as bucket, min((value::text)::numeric), max((value::text)::numeric), avg((value::text)::numeric) FROM asset_datapoint WHERE entity_id = '%s' AND attribute_name = '%s' AND timestamp > now() - INTERVAL '30 days' GROUP BY bucket;"
)
QUERY_NAMES=("Hourly_Avg_7d" "LTTB_500_14d" "Daily_MinMaxAvg_30d")

# 3. Build the descriptive CSV headers
echo "Generating headers..."
CSV_HEADER="Timestamp"

declare -a ENTITY_IDS
declare -a ATTRIBUTE_NAMES

while IFS='|' read -r entity_id attribute_name; do
    ENTITY_IDS+=("$entity_id")
    ATTRIBUTE_NAMES+=("$attribute_name")
done <<< "$ENTITIES"

# Create descriptive column headers
for i in "${!ENTITY_IDS[@]}"; do
    E_ID="${ENTITY_IDS[$i]}"
    A_NAME="${ATTRIBUTE_NAMES[$i]}"

    # SAFER PARSING: Grab first 4 chars of ID using 'cut' instead of bash string slicing
    SHORT_ID="$(echo "$E_ID" | cut -c 1-4)-${A_NAME}"

    for q in "${!QUERY_NAMES[@]}"; do
        Q_NAME="${QUERY_NAMES[$q]}"
        CSV_HEADER="${CSV_HEADER},${SHORT_ID}_${Q_NAME}_ms"
    done
done

# Initialize the CSV file
echo "$CSV_HEADER" > "$CSV_FILE"
echo "Headers created successfully."

# 4. Execute the load loop
echo "Starting load generation ($ITERATIONS iterations)..."

for (( iter=1; iter<=ITERATIONS; iter++ )); do
    START_TIME=$(date -Iseconds)
    CSV_ROW="$START_TIME"

    echo "Iteration $iter / $ITERATIONS starting at $START_TIME"

    for i in "${!ENTITY_IDS[@]}"; do
        E_ID="${ENTITY_IDS[$i]}"
        A_NAME="${ATTRIBUTE_NAMES[$i]}"

        for q in "${!QUERY_TEMPLATES[@]}"; do
            TEMPLATE="${QUERY_TEMPLATES[$q]}"

            RAW_QUERY=$(printf "$TEMPLATE" "$E_ID" "$A_NAME")
            ANALYZE_QUERY="EXPLAIN ANALYZE $RAW_QUERY"

            # SAFER PARSING: Execute and extract execution time.
            # Replaced 'awk' with 'tr' and 'cut' to avoid curly braces inside the subshell!
            OUT=$(run_psql "$ANALYZE_QUERY" 2>&1)
            EXEC_TIME=$(echo "$OUT" | grep "Execution Time" | tr -s ' ' | cut -d' ' -f3)

            if [ -z "$EXEC_TIME" ]; then
                EXEC_TIME="ERROR"
                echo "  [!] Error running $A_NAME (Iter $iter)"
            fi

            CSV_ROW="${CSV_ROW},${EXEC_TIME}"
        done
    done

    # Write the row to the CSV
    echo "$CSV_ROW" >> "$CSV_FILE"
    echo "  Completed iteration $iter."
done

echo "Benchmark complete! Results saved to $CSV_FILE"
