#!/bin/sh
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
mv /storage/dump.hprof "/storage/dump_${TIMESTAMP}.hprof" 2>/dev/null || true
