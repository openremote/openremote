#!/bin/sh
# Rename any heap dump left from a previous run (in case OnOutOfMemoryError script didn't run)
if [ -f /storage/dump.hprof ]; then
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    mv /storage/dump.hprof "/storage/dump_${TIMESTAMP}.hprof"
fi
# Warn if any heap dumps are present
HEAP_DUMPS=$(ls /storage/dump_*.hprof 2>/dev/null)
if [ -n "$HEAP_DUMPS" ]; then
    echo "WARNING: Heap dump(s) found in /storage - remove them to free up disk space:"
    ls -lh /storage/dump_*.hprof
fi

OTEL_JAVA_AGENT_OPTION=
case "${OTEL_JAVAAGENT_ENABLED:-false}" in
    [Tt][Rr][Uu][Ee])
        OTEL_JAVA_AGENT_OPTION="-javaagent:/opt/opentelemetry/opentelemetry-javaagent.jar"
        ;;
esac

exec java ${JAVA_OPTS} ${JAVA_OPTS_APPEND} ${OTEL_JAVA_AGENT_OPTION} -cp "/opt/app/lib/*:/deployment/manager/extensions/*:/extensions/*" org.openremote.manager.Main
