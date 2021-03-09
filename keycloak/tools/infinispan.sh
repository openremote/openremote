# How many owners / replicas should our distributed caches have. If <2 any node that is removed from the cluster will cause a data-loss!
# As it is only sensible to replicate AuthenticationSessions for certain cases, their replication factor can be configured independently

if [ -n "$CACHE_OWNERS_COUNT" ]; then
    echo "Setting cache owners to $CACHE_OWNERS_COUNT replicas"

    # Check and log the replication factor of AuthenticationSessions, otherwise this is set to 1 by default
    if [ -n "$CACHE_OWNERS_AUTH_SESSIONS_COUNT" ]; then
        echo "Enabling replication of AuthenticationSessions with ${CACHE_OWNERS_AUTH_SESSIONS_COUNT} replicas"
    else
        echo "AuthenticationSessions will NOT be replicated, set CACHE_OWNERS_AUTH_SESSIONS_COUNT to configure this"
    fi
$JBOSS_HOME/bin/jboss-cli.sh --file="/opt/jboss/tools/cli/infinispan/cache-owners.cli" >& /dev/null
fi
