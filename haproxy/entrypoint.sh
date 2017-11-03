#!/bin/bash

set -u

CERT_FILE="/opt/selfsigned/localhost.pem"

if [ -n "${DOMAINNAME}" ] && [ "${DOMAINNAME}" != "localhost" ]; then
  CERT_FILE="/etc/letsencrypt/live/${DOMAINNAME}/haproxy.pem"
fi

export CERT_FILE=${CERT_FILE}

if [ ! -f ${CERT_FILE} ]; then
  HAPROXY_CONFIG="/etc/haproxy/haproxy-init.cfg"
else
  HAPROXY_CONFIG="/etc/haproxy/haproxy.cfg"
fi

HAPROXY_USER_PARAMS=$@
HAPROXY_PID_FILE="/var/run/haproxy.pid"
HAPROXY_CMD="haproxy -f ${HAPROXY_CONFIG} ${HAPROXY_USER_PARAMS} -D -p ${HAPROXY_PID_FILE}"
HAPROXY_CHECK_CONFIG_CMD="haproxy -f ${HAPROXY_CONFIG} -c"

log() {
  if [[ "$@" ]]; then echo "[`date +'%Y-%m-%d %T'`] $@";
  else echo; fi
}

log "DOMAINNAME: ${DOMAINNAME}"
log "CERT_FILE: ${CERT_FILE}"
log "CONFIG: ${HAPROXY_CONFIG}"

rsyslogd

$HAPROXY_CHECK_CONFIG_CMD
$HAPROXY_CMD
if [[ $? != 0 ]] || test -t 0; then exit $?; fi
log "HAProxy started with $HAPROXY_CONFIG config, pid $(cat $HAPROXY_PID_FILE)."

# Check if config or certificates were changed
while inotifywait -q -r --exclude '\.git/' -e modify,create,delete,move,move_self $HAPROXY_CONFIG /etc/letsencrypt; do
  if [ -f $HAPROXY_PID_FILE ]; then
    log "Restarting HAProxy due to config changes..."

    # Wait for the certificate to be present before restarting or might interrupt cert generation/renewal
    log "Waiting for certificate '${CERT_FILE}' to exist..."
    while ! ls -l "${CERT_FILE}" >/dev/null 2>&1; do sleep 0.1; done
    log "Certificate '${CERT_FILE}' now exists!"
    HAPROXY_CONFIG="/etc/haproxy/haproxy.cfg"
    HAPROXY_CMD="haproxy -f ${HAPROXY_CONFIG} ${HAPROXY_USER_PARAMS} -D -p ${HAPROXY_PID_FILE}"
    HAPROXY_CHECK_CONFIG_CMD="haproxy -f ${HAPROXY_CONFIG} -c"

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
