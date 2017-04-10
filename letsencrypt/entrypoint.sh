#!/bin/bash

crond

if [ -n "${LE_EMAIL}" ]; then
  LE_EXTRA_ARGS+=" --email ${LE_EMAIL}"
fi

if [ -n "${LE_RSA_KEY_SIZE}" ]; then
  LE_EXTRA_ARGS+=" --rsa-key-size ${LE_RSA_KEY_SIZE}"
fi

LE_CERT_ROOT="/etc/letsencrypt/live"
LE_ARCHIVE_ROOT="/etc/letsencrypt/archive"
LE_RENEWAL_CONFIG_ROOT="/etc/letsencrypt/renewal"
LE_CMD="/usr/bin/certbot certonly ${LE_EXTRA_ARGS}"

function print_help {
  echo "Available commands:"
  echo ""
  echo "help              - Show this help"
  echo "list              - List configured domains and their certificate's status"
  echo "add               - Add a new domain and create a certificate for it"
  echo "renew             - Renew the certificate for an existing domain. Allows to add additional domain names."
  echo "remove            - Remove and existing domain and its certificate"
  echo "cron-auto-renewal - Run the cron job automatically renewing all certificates"
  echo "auto-renew        - Try to automatically renew all installed certificates"
  echo "print-pin         - Print the public key pin for a given domain for usage with HPKP"
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

  echo "Adding domain \"${DOMAINNAME}\"..."

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
   >&2 echo "failed to create haproxy.pem file!"
   exit 2
  fi
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

  echo "Renewing domain \"${DOMAINNAME}\"..."

  DOMAIN_ARGS="-d ${DOMAINNAME}"
  for name in ${@}; do
    if [ "${name}" != "${DOMAINNAME}" ]; then
      DOMAIN_ARGS="${DOMAIN_ARGS} -d ${name}"
    fi
  done

  ${LE_CMD} --renew-by-default ${DOMAIN_ARGS}

  LE_RESULT=$?

  if [ ${LE_RESULT} -ne 0 ]; then
   >&2 echo "letsencrypt returned error code ${LE_RESULT}"
   return ${LE_RESULT}
  fi

  # Concat the certificate chain and private key to a PEM file suitable for HAProxy
  cat "${DOMAIN_FOLDER}/privkey.pem" \
   "${DOMAIN_FOLDER}/fullchain.pem" \
   > "/tmp/haproxy.pem"
   mv "/tmp/haproxy.pem" "${DOMAIN_FOLDER}/haproxy.pem"

  if [ $? -ne 0 ]; then
   >&2 echo "failed to create haproxy.pem file!"
   return 2
  fi
}

# check certificate expiration and run certificate issue requests
# for those that expire in under 4 weeks
function auto_renew {
  echo "Executing auto renew at $(date -R)"
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

  echo "Domain ${DOMAIN} removed successfully."
}

function log_error {
  if [ -n "${LOGFILE}" ]
  then
    echo "[error] ${1}\n" >> ${LOGFILE}
  fi
  >&2 echo "[error] ${1}"
}

function log_info {
  if [ -n "${LOGFILE}" ]
  then
    echo "[info] ${1}\n" >> ${`LOGFILE`}
  else
    echo "[info] ${1}"
  fi
}

function die {
    echo >&2 "$@"
    exit 1
}

if [ $# -eq 0 ]
then
  print_help
  exit 0
fi

CMD="${1}"
shift

if [ "${CMD}" = "add"  ]; then
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
  # CRON_TIME can be set via environment
  # If not defined, the default is daily
  CRON_TIME=${CRON_TIME:-@daily}
  echo "Running cron job with execution time ${CRON_TIME}"
  echo "${CRON_TIME} root /entrypoint.sh auto-renew >> /var/log/cron.log 2>&1" > /etc/cron.d/letsencrypt
  touch /var/log/cron.log && cron && tail -f /var/log/cron.log
elif [ "${CMD}" = "print-pin" ]; then
  print_pin "${@}"
else
  die "Unknown command ${CMD}"
fi