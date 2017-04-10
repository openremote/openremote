#!/bin/bash

set -u

if [ -z "${HAPROXY_CONFIG-}" ]; then
    if [ -f /etc/haproxy/haproxy.cfg ]
    then
        HAPROXY_CONFIG="/etc/haproxy/haproxy.cfg"
    else
        HAPROXY_CONFIG="/etc/haproxy/haproxy-init.cfg"
    fi
fi

HAPROXY_USER_PARAMS=$@

HAPROXY_PID_FILE="/var/run/haproxy.pid"
HAPROXY_CMD="haproxy -f ${HAPROXY_CONFIG} ${HAPROXY_USER_PARAMS} -D -p ${HAPROXY_PID_FILE}"
HAPROXY_CHECK_CONFIG_CMD="haproxy -f ${HAPROXY_CONFIG} -c"

log() {
  if [[ "$@" ]]; then echo "[`date +'%Y-%m-%d %T'`] $@";
  else echo; fi
}

rsyslogd

$HAPROXY_CHECK_CONFIG_CMD
$HAPROXY_CMD
if [[ $? != 0 ]] || test -t 0; then exit $?; fi
log "HAProxy started with $HAPROXY_CONFIG config, pid $(cat $HAPROXY_PID_FILE)."

# Check if config or certificates were changed
while inotifywait -q -r --exclude '\.git/' -e modify,create,delete,move,move_self $HAPROXY_CONFIG /etc/letsencrypt; do
  if [ -f $HAPROXY_PID_FILE ]; then
    log "Restarting HAProxy due to config changes..."
    if $HAPROXY_CHECK_CONFIG_CMD; then
      $HAPROXY_CMD -sf $(cat $HAPROXY_PID_FILE)
      log "HAProxy restarted, pid $(cat $HAPROXY_PID_FILE)." && log
    else
      log "HAProxy config invalid, not restarting..."
    fi
  else
    log "Error: no $HAPROXY_PID_FILE present, HAProxy exited."
    break
  fi
done
