#!/bin/bash

# Configure letsencrypt
export LE_WEB_ROOT="/deployment/acme-webroot"
mkdir -p $LE_WEB_ROOT
if [ -n "${LE_EMAIL}" ]; then
  LE_EXTRA_ARGS+=" --email ${LE_EMAIL}"
fi
if [ -n "${LE_RSA_KEY_SIZE}" ]; then
  LE_EXTRA_ARGS+=" --rsa-key-size ${LE_RSA_KEY_SIZE}"
fi
LE_WORK_DIR="/deployment/letsencrypt"
export LE_CERT_ROOT="${LE_WORK_DIR}/live"
LE_ARCHIVE_ROOT="${LE_WORK_DIR}/archive"
LE_RENEWAL_CONFIG_ROOT="${LE_WORK_DIR}/renewal"
LE_CMD="/usr/bin/certbot certonly --config-dir ${LE_WORK_DIR} -w ${LE_WEB_ROOT} ${LE_EXTRA_ARGS}"
mkdir -p $LE_CERT_ROOT

# Configure haproxy
export CERT_FILE="/opt/selfsigned/localhost.pem"
if [ -n "${DOMAINNAME}" ] && [ "${DOMAINNAME}" != "localhost" ]; then
  export CERT_FILE="${LE_CERT_ROOT}/${DOMAINNAME}/haproxy.pem"
fi
if [ ! -f ${CERT_FILE} ]; then
  INIT=true
  HAPROXY_CONFIG="/etc/haproxy/haproxy-init.cfg"
else
  HAPROXY_CONFIG="/etc/haproxy/haproxy.cfg"
  INIT=false
fi
HAPROXY_PID_FILE="/var/run/haproxy.pid"
HAPROXY_CMD="haproxy -f ${HAPROXY_CONFIG} ${HAPROXY_USER_PARAMS} -D -p ${HAPROXY_PID_FILE}"
HAPROXY_CHECK_CONFIG_CMD="haproxy -f ${HAPROXY_CONFIG} -c"

function print_help {
  echo "Available commands:"
  echo ""
  echo "help                    - Show this help"
  echo "run                     - Run proxy in foreground and monitor config changes, executes check and cron-auto-renewal-init"
  echo "check                   - Check proxy configuration only"
  echo "list                    - List configured domains and their certificate's status"
  echo "add                     - Add a new domain and create a certificate for it"
  echo "renew                   - Renew the certificate for an existing domain. Allows to add additional domain names."
  echo "remove                  - Remove and existing domain and its certificate"
  echo "cron-auto-renewal       - Run the cron job automatically renewing all certificates"
  echo "cron-auto-renewal-init  - Obtain missing certificates and automatically renew afterwards"
  echo "auto-renew              - Try to automatically renew all installed certificates"
  echo "print-pin               - Print the public key pin for a given domain for usage with HPKP"
}

function check_proxy {
    log_info "Checking HAProxy configuration: $HAPROXY_CONFIG"
    $HAPROXY_CHECK_CONFIG_CMD
}

function run_proxy {
    # Start rsyslogd
    service rsyslog start

    check_proxy

    $HAPROXY_CMD
    if [[ $? != 0 ]] || test -t 0; then exit $?; fi
    log_info "HAProxy started with $HAPROXY_CONFIG config, pid $(cat $HAPROXY_PID_FILE)."

    cron_auto_renewal_init

    # Cert file should now exist
    if [ "$INIT" = true ]; then
        restart
    fi

    log_info "Monitoring config file $HAPROXY_CONFIG and certs in $LE_CERT_ROOT for changes..."

    # Wait if config or certificates were changed, block this execution
    while inotifywait -q -r --exclude '\.git/' -e modify,create,delete,move,move_self $HAPROXY_CONFIG $LE_CERT_ROOT; do
        log_info "Change detected..."
        restart
        log_info "Monitoring config file $HAPROXY_CONFIG and certs in $LE_CERT_ROOT for changes..."
    done
}

function restart {
      if [ -f $HAPROXY_PID_FILE ]; then
        log_info "Restarting HAProxy..."

        # Wait for the certificate to be present before restarting or might interrupt cert generation/renewal
        log_info "Waiting for certificate '${CERT_FILE}' to exist..."
        while ! ls -l "${CERT_FILE}" >/dev/null 2>&1; do sleep 0.1; done
        log_info "Certificate '${CERT_FILE}' now exists!"
        HAPROXY_CONFIG="/etc/haproxy/haproxy.cfg"
        HAPROXY_CMD="haproxy -f ${HAPROXY_CONFIG} ${HAPROXY_USER_PARAMS} -D -p ${HAPROXY_PID_FILE}"
        HAPROXY_CHECK_CONFIG_CMD="haproxy -f ${HAPROXY_CONFIG} -c"

        log_info "HAPROXY_CONFIG: ${HAPROXY_CONFIG}"
        log_info "HAPROXY_CMD: ${HAPROXY_CMD}"

        if $HAPROXY_CHECK_CONFIG_CMD; then
          $HAPROXY_CMD -sf $(cat $HAPROXY_PID_FILE)
          log_info "HAProxy restarted, pid $(cat $HAPROXY_PID_FILE)."
        else
          log_info "HAProxy config invalid, not restarting..."
        fi
      else
        log_error"Error: no $HAPROXY_PID_FILE present, HAProxy exited."
        break
      fi
}

function add {
  if [ $# -lt 1 ]
  then
    echo 'Usage: add <domain name> <alternative domain name>...'
    exit -1
  fi

  DOMAINNAME="${1}"
  DOMAIN_FOLDER="${LE_CERT_ROOT}/${DOMAINNAME}"

  if [ -e "${DOMAIN_FOLDER}" ]; then
    log_error "Domain ${1} already exists."
    exit 3
  fi

  log_info "Adding domain \"${DOMAINNAME}\"..."

  DOMAIN_ARGS="-d ${DOMAINNAME}"
  for name in ${@}; do
    if [ "${name}" != "${DOMAINNAME}" ]; then
      DOMAIN_ARGS="${DOMAIN_ARGS} -d ${name}"
    fi
  done

  ${LE_CMD} ${DOMAIN_ARGS} || die "Failed to issue certificate "

  # Concat the certificate chain and private key to a PEM file suitable for HAProxy
  cat "${DOMAIN_FOLDER}/privkey.pem" \
   "${DOMAIN_FOLDER}/fullchain.pem" \
   > "/tmp/haproxy.pem"
   mv "/tmp/haproxy.pem" "${DOMAIN_FOLDER}/haproxy.pem"

  if [ $? -ne 0 ]; then
   >&2 log_error "failed to create haproxy.pem file!"
   exit 2
  fi

  log_info "Added domain \"${DOMAINNAME}\"..."
}

function renew {
  if [ $# -lt 1 ]
  then
    echo 'Usage: renew <domain name> <alternative domain name>...'
    exit -1
  fi

  DOMAINNAME="${1}"
  DOMAIN_FOLDER="${LE_CERT_ROOT}/${DOMAINNAME}"

  if [ ! -d "${DOMAIN_FOLDER}" ]; then
    log_error "Domain ${DOMAINNAME} does not exist! Cannot renew it."
    exit 6
  fi

  log_info "Renewing domain \"${DOMAINNAME}\"..."

  DOMAIN_ARGS="-d ${DOMAINNAME}"
  for name in ${@}; do
    if [ "${name}" != "${DOMAINNAME}" ]; then
      DOMAIN_ARGS="${DOMAIN_ARGS} -d ${name}"
    fi
  done

  ${LE_CMD} --renew-by-default --expand ${DOMAIN_ARGS}

  LE_RESULT=$?

  if [ ${LE_RESULT} -ne 0 ]; then
   >&2 log_error "letsencrypt returned error code ${LE_RESULT}"
   return ${LE_RESULT}
  fi

  # Concat the certificate chain and private key to a PEM file suitable for HAProxy
  cat "${DOMAIN_FOLDER}/privkey.pem" \
   "${DOMAIN_FOLDER}/fullchain.pem" \
   > "/tmp/haproxy.pem"
   mv "/tmp/haproxy.pem" "${DOMAIN_FOLDER}/haproxy.pem"

  if [ $? -ne 0 ]; then
   >&2 log_error "failed to create haproxy.pem file!"
   return 2
  fi

  log_info "Renewed domain \"${DOMAINNAME}\"..."
}

# check certificate expiration and run certificate issue requests
# for those that expire in under 4 weeks
function auto_renew {
  log_info "Executing auto renew at $(date -R)"
  renewed_certs=()
  exitcode=0
  while IFS= read -r -d '' cert; do

    CERT_DIR_PATH=$(dirname ${cert})
    DOMAINNAME=$(basename ${CERT_DIR_PATH})

    if ! openssl x509 -noout -checkend $((4*7*86400)) -in "${cert}"; then
      subject="$(openssl x509 -noout -subject -in "${cert}" | grep -o -E 'CN=[^ ,]+' | tr -d 'CN=')"
      subjectaltnames="$(openssl x509 -noout -text -in "${cert}" | sed -n '/X509v3 Subject Alternative Name/{n;p}' | sed 's/\s//g' | tr -d 'DNS:' | sed 's/,/ /g')"
      domains="${subject}"
      for name in ${subjectaltnames}; do
        if [ "${name}" != "${subject}" ]; then
          domains="${domains} ${name}"
        fi
      done
      renew ${domains}
      if [ $? -ne 0 ]
      then
        log_error "failed to renew certificate for ${subject}!"
        exitcode=1
      else
        renewed_certs+=("$subject")
        log_info "renewed certificate for ${subject}"
      fi
    else
      log_info "Certificate for ${DOMAINNAME} does not require renewal."
    fi
  done < <(find ${LE_CERT_ROOT} -name cert.pem -print0)
}

function list {
  OUTPUT="DOMAINNAME@ALTERNATIVE DOMAINNAMES@VALID UNTIL@REMAINING DAYS\n"
  while IFS= read -r -d '' cert; do
    enddate_str=$(openssl x509 -enddate -noout -in "$cert" | sed 's/.*=//g')
    enddate=$(date --date="$enddate_str" +%s)
    now=$(date +%s)
    remaining_days=$(( ($enddate - $now) / 60 / 60 / 24 ))
    subject="$(openssl x509 -noout -subject -in "${cert}" | grep -o -E 'CN=[^ ,]+' | tr -d 'CN=')"
    subjectaltnames="$(openssl x509 -noout -text -in "${cert}" | sed -n '/X509v3 Subject Alternative Name/{n;p}' | sed 's/\s//g' | tr -d 'DNS:' | sed 's/,/ /g')"
    altnames=$(echo $subjectaltnames | sed "s/\([^\.]\)\($subject\)\([^\.]\)/\3/g")
    OUTPUT+="${subject}@${altnames}@${enddate_str}@${remaining_days}\n"
  done < <(find ${LE_CERT_ROOT} -name cert.pem -print0)

  echo -e $OUTPUT | column -t -s '@'
}

function print_pin {
  if [ $# -lt 1 ]
  then
    echo 'Usage: print-pin <domain name>'
    exit -1
  fi

  DOMAINNAME="${1}"
  DOMAIN_FOLDER="${LE_CERT_ROOT}/${DOMAINNAME}"

  if [ ! -d "${DOMAIN_FOLDER}" ]; then
    log_error "Domain ${DOMAINNAME} does not exist!"
    exit 6
  fi

  pin_sha256=$(openssl rsa -in "${DOMAIN_FOLDER}/privkey.pem" -outform der -pubout 2> /dev/null | openssl dgst -sha256 -binary | openssl enc -base64)

  echo
  echo "pin-sha256: ${pin_sha256}"
  echo
  echo "Example usage in HTTP header:"
  echo "Public-Key-Pins: pin-sha256="${pin_sha256}"; max-age=5184000; includeSubdomains;"
  echo
  echo "CAUTION: Make sure to also add another pin for a backup key!"
}

function remove {
  if [ $# -lt 1 ]
  then
    echo 'Usage: remove <domain name>'
    exit -1
  fi

  log_info "Removing domain \"${DOMAINNAME}\"..."

  DOMAINNAME=$1
  DOMAIN_LIVE_FOLDER="${LE_CERT_ROOT}/${DOMAINNAME}"
  DOMAIN_ARCHIVE_FOLDER="${LE_ARCHIVE_ROOT}/${DOMAINNAME}"
  DOMAIN_RENEWAL_CONFIG="${LE_RENEWAL_CONFIG_ROOT}/${DOMAINNAME}.conf"

  if [ ! -d "${DOMAIN_LIVE_FOLDER}" ]; then
    log_error "Domain ${1} does not exist! Cannot remove it."
    exit 5
  fi

  rm -rf ${DOMAIN_LIVE_FOLDER} || die "Failed to remove domain live directory ${DOMAIN_FOLDER}"
  rm -rf ${DOMAIN_ARCHIVE_FOLDER} || die "Failed to remove domain archive directory ${DOMAIN_ARCHIVE_FOLDER}"
  rm -f ${DOMAIN_RENEWAL_CONFIG} || die "Failed to remove domain renewal config ${DOMAIN_RENEWAL_CONFIG}"

  log_info "Removed domain \"${DOMAINNAME}\"..."
}

function log_error {
  if [ -n "${LOGFILE}" ]
  then
    if [[ "$@" ]]; then echo "[ERROR][`date +'%Y-%m-%d %T'`] $@\n" >> ${LOGFILE};
    else echo; fi
    >&2 echo "[ERROR][`date +'%Y-%m-%d %T'`] $@"
  else
    echo "[ERROR][`date +'%Y-%m-%d %T'`] $@"
  fi

}

function log_info {
  if [ -n "${LOGFILE}" ]
  then
    if [[ "$@" ]]; then echo "[INFO][`date +'%Y-%m-%d %T'`] $@\n" >> ${LOGFILE};
    else echo; fi
    >&2 echo "[INFO][`date +'%Y-%m-%d %T'`] $@"
  else
    echo "[INFO][`date +'%Y-%m-%d %T'`] $@"
  fi
}

function die {
    echo >&2 "$@"
    exit 1
}

function cron_auto_renewal_init {
  log_info "Executing cron_auto_renewal_init at $(date -R)"

  if [ -n "${DOMAINNAME}" ] && [ "${DOMAINNAME}" != "localhost" ]; then
    if [ ! -d "${LE_CERT_ROOT}/${DOMAINNAME}" ]; then
      log_info "Initialising certificate for '${DOMAINNAME}'..."
      rm -rf "${LE_CERT_ROOT}/${DOMAINNAME}"
      add ${DOMAINNAME}
    fi

    # TODO Multiple domains
    #  while IFS= read -r -d '' domain_dir; do
    #    DOMAINNAME=$(basename ${domain_dir})
    #    log_info "Checking domain '${DOMAINNAME}'..."
    #
    #    if [ -f "${domain_dir}/cert.pem" ]; then
    #      log_info "Certificate exists"
    #    else
    #      log_info "Certificate doesn't exist so attempting to add it..."
    #      log_info "Removing domain dir otherwise add will fail..."
    #      log_info "CALL ADD ${DOMAINNAME}"
    #      rm -rf ${domain_dir}
    #      add ${DOMAINNAME}
    #    fi
    #
    #  done < <(find ${LE_CERT_ROOT} -mindepth 1 -maxdepth 1 -type d -print0)

    auto_renew
    cron_auto_renewal

  else
    log_info "Domain is 'localhost', dummy SSL certificate does not have to be renewed"
  fi
}

function cron_auto_renewal {
    # CRON_TIME can be set via environment
    # If not defined, the default is daily
    CRON_TIME=${CRON_TIME:-@daily}
    log_info "Scheduling cron job with execution time ${CRON_TIME}"
    echo "${CRON_TIME} root /entrypoint.sh auto-renew >> /var/log/cron.log 2>&1" > /etc/cron.d/letsencrypt

    # Start crond if not running
    if ! [ -f /var/run/crond.pid ]; then
        service cron start
    fi
}

log_info "DOMAINNAME: ${DOMAINNAME}"
log_info "HAPROXY_CERT_FILE: ${CERT_FILE}"
log_info "HAPROXY_CONFIG: ${HAPROXY_CONFIG}"
log_info "HAPROXY_CMD: ${HAPROXY_CMD}"
log_info "HAPROXY_USER_PARAMS: ${HAPROXY_USER_PARAMS}"
log_info "PROXY_LOGLEVEL: ${PROXY_LOGLEVEL}"
log_info "LE_CERT_ROOT: ${LE_CERT_ROOT}"
log_info "LE_ARCHIVE_ROOT: ${LE_ARCHIVE_ROOT}"
log_info "LE_RENEWAL_CONFIG_ROOT: ${LE_RENEWAL_CONFIG_ROOT}"
log_info "LE_CMD: ${LE_CMD}"

if [ $# -eq 0 ]
then
  print_help
  exit 0
fi

CMD="${1}"
shift

if [ "${CMD}" = "run"  ]; then
  run_proxy "${@}"
elif [ "${CMD}" = "check" ]; then
  check_proxy "${@}"
elif [ "${CMD}" = "add" ]; then
  add "${@}"
elif [ "${CMD}" = "list" ]; then
  list "${@}"
elif [ "${CMD}" = "remove" ]; then
  remove "${@}"
elif [ "${CMD}" = "renew" ]; then
  renew "${@}"
elif [ "${CMD}" = "auto-renew" ]; then
  auto_renew "${@}"
elif [ "${CMD}" = "help" ]; then
  print_help "${@}"
elif [ "${CMD}" = "cron-auto-renewal" ]; then
  cron_auto_renewal
elif [ "${CMD}" = "print-pin" ]; then
  print_pin "${@}"
elif [ "${CMD}" = "cron-auto-renewal-init" ]; then
  cron_auto_renewal_init
else
  die "Unknown command: ${CMD}"
fi