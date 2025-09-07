#!/usr/bin/env bash
set -euo pipefail

# =========================
# Config (override via env)
# =========================
: "${OUT_DIR:=mtls}"
: "${PW:=secret}"                                   # password for all PKCS#12 stores
: "${CA_CN:=OpenRemote Root CA}"
: "${SERVER_CN:=auth.local}"                        # server certificate CN
: "${SERVER_DNS_1:=localhost}"                      # SANs
: "${SERVER_DNS_2:=auth.local}"
: "${CLIENT_CN:=openremote-service-client}"         # client certificate CN
: "${CA_DAYS:=3650}"                                # ~10 years
: "${SERVER_DAYS:=825}"                             # ~27 months
: "${CLIENT_DAYS:=365}"                             # 1 year
: "${KEY_BITS:=2048}"                               # RSA key size for leaf certs
: "${FORCE:=0}"                                     # set FORCE=1 to overwrite existing files

# =========================
# Helpers
# =========================
need() { command -v "$1" >/dev/null 2>&1 || { echo "Missing dependency: $1" >&2; exit 1; }; }
write_once() {
  local path="$1"; shift
  if [[ -e "$path" && "$FORCE" != "1" ]]; then
    echo "[SKIP] $path exists (set FORCE=1 to overwrite)"
  else
    mkdir -p "$(dirname "$path")"
    cat > "$path"
    echo "[WRITE] $path"
  fi
}

fail_if_empty() {
  local f="$1"
  [[ -s "$f" ]] || { echo "File $f is missing or empty"; exit 1; }
}

# =========================
# Pre-flight
# =========================
need openssl
need keytool

mkdir -p "$OUT_DIR"
cd "$OUT_DIR"

echo "===> Output dir: $PWD"

# =========================
# 0) OpenSSL configs
# =========================

# Root CA config (CA:TRUE, keyCertSign, cRLSign)
write_once ca-openssl.cnf <<'EOF'
[ req ]
distinguished_name = dn
x509_extensions = v3_ca
prompt = no

[ dn ]
CN = __CA_CN__

[ v3_ca ]
subjectKeyIdentifier   = hash
authorityKeyIdentifier = keyid:always,issuer
basicConstraints       = critical,CA:true
keyUsage               = critical,keyCertSign,cRLSign
EOF
sed -i '' -e "s#__CA_CN__#${CA_CN}#g" ca-openssl.cnf 2>/dev/null || sed -i -e "s#__CA_CN__#${CA_CN}#g" ca-openssl.cnf

# Server extensions (SANs + serverAuth)
write_once server-ext.cnf <<EOF
basicConstraints = CA:FALSE
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names
[alt_names]
DNS.1 = ${SERVER_DNS_1}
DNS.2 = ${SERVER_DNS_2}
EOF

# Client extensions (clientAuth)
write_once client-ext.cnf <<'EOF'
basicConstraints = CA:FALSE
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = clientAuth
EOF

# =========================
# 1) Root CA (rootca.key + rootca.crt)
# =========================
if [[ ! -s rootca.key || ! -s rootca.crt || "$FORCE" == "1" ]]; then
  echo "===> Creating Root CA"
  openssl genrsa -out rootca.key 4096
  chmod 600 rootca.key
  openssl req -x509 -new -nodes \
    -key rootca.key \
    -sha256 -days "$CA_DAYS" \
    -out rootca.crt \
    -config ca-openssl.cnf
else
  echo "[SKIP] Root CA already exists (rootca.key, rootca.crt)"
fi
fail_if_empty rootca.crt

# =========================
# 2) Server key + CSR + CA-signed cert
# =========================
if [[ ! -s server.key || "$FORCE" == "1" ]]; then
  echo "===> Generating server key"
  openssl genrsa -out server.key "$KEY_BITS"
fi
openssl req -new -key server.key -out server.csr -subj "/CN=${SERVER_CN}"

echo "===> Signing server certificate with Root CA"
openssl x509 -req -in server.csr \
  -CA rootca.crt -CAkey rootca.key -CAcreateserial \
  -out server.crt -days "$SERVER_DAYS" -sha256 \
  -extfile server-ext.cnf

# Basic sanity
s1=$(openssl x509 -noout -modulus -in server.crt | openssl md5)
s2=$(openssl rsa  -noout -modulus -in server.key  | openssl md5)
[[ "$s1" == "$s2" ]] || { echo "Server key and cert do NOT match"; exit 1; }

# =========================
# 3) Client key + CSR + CA-signed cert (clientAuth)
# =========================
if [[ ! -s client.key || "$FORCE" == "1" ]]; then
  echo "===> Generating client key"
  openssl genrsa -out client.key "$KEY_BITS"
fi
openssl req -new -key client.key -out client.csr -subj "/CN=${CLIENT_CN}"

echo "===> Signing client certificate with Root CA"
openssl x509 -req -in client.csr \
  -CA rootca.crt -CAkey rootca.key -CAcreateserial \
  -out client.crt -days "$CLIENT_DAYS" -sha256 \
  -extfile client-ext.cnf

# Basic sanity
c1=$(openssl x509 -noout -modulus -in client.crt | openssl md5)
c2=$(openssl rsa  -noout -modulus -in client.key  | openssl md5)
[[ "$c1" == "$c2" ]] || { echo "Client key and cert do NOT match"; exit 1; }

# =========================
# 4) Build PKCS#12 stores
# =========================

# 4a) Server PKCS#12 keystore for Keycloak HTTPS
# Chain can be just root (no intermediate). If you have intermediates, put them first.
cp -f rootca.crt chain.crt
echo "===> Creating server-keystore.p12 (PKCS#12)"
openssl pkcs12 -export \
  -inkey server.key \
  -in server.crt \
  -certfile chain.crt \
  -out server-keystore.p12 \
  -name keycloak \
  -passout pass:"$PW"

# 4b) Truststore with client-issuing CA(s) (TrustedCertEntry only)
echo "===> Creating server-truststore.p12 (PKCS#12)"
# Start fresh truststore file if FORCE=1
[[ "$FORCE" == "1" && -f server-truststore.p12 ]] && rm -f server-truststore.p12
keytool -importcert -noprompt \
  -alias client-ca \
  -file rootca.crt \
  -storetype PKCS12 \
  -keystore server-truststore.p12 \
  -storepass "$PW"

# 4c) Client PKCS#12 (handy for curl and some SDKs)
echo "===> Creating client.p12 (PKCS#12)"
openssl pkcs12 -export \
  -inkey client.key \
  -in client.crt \
  -certfile rootca.crt \
  -out client.p12 \
  -name "${CLIENT_CN}" \
  -passout pass:"$PW"

# =========================
# 5) PEM exports for curl (optional)
# =========================
echo "===> Writing PEM exports for curl"
cp -f client.crt client.pem
cat client.crt rootca.crt > client-with-chain.pem   # leaf + root (add intermediates here if you have any)
cp -f server.crt tls.crt
cp -f server.key tls.key

# Tighten perms
chmod 600 server.key client.key tls.key || true

# =========================
# 6) Quick validation
# =========================
echo "===> Validating keystores"
keytool -list -storetype PKCS12 -keystore server-keystore.p12 -storepass "$PW" >/dev/null
keytool -list -storetype PKCS12 -keystore server-truststore.p12 -storepass "$PW" >/dev/null

echo
echo "======================================"
echo " Done!"
echo " Files created in: $PWD"
echo " - Root CA:           rootca.key, rootca.crt"
echo " - Server leaf:       server.key, server.crt (SANs: ${SERVER_DNS_1}, ${SERVER_DNS_2})"
echo " - Client leaf:       client.key, client.crt (EKU=clientAuth)"
echo " - Server keystore:   server-keystore.p12   (password: $PW, alias: keycloak)"
echo " - Truststore:        server-truststore.p12 (password: $PW, alias: client-ca)"
echo " - Client bundle:     client.p12            (password: $PW, alias: ${CLIENT_CN})"
echo " - curl PEMs:         client-with-chain.pem, client.key, rootca.crt"
echo "======================================"
echo
echo "Example Keycloak env:"
cat <<EOF
  KC_HTTPS_CLIENT_AUTH: request
  KC_HTTPS_KEY_STORE_FILE: /opt/keycloak/certs/server-keystore.p12
  KC_HTTPS_KEY_STORE_PASSWORD: $PW
  KC_HTTPS_KEY_STORE_TYPE: PKCS12
  KC_HTTPS_TRUST_STORE_FILE: /opt/keycloak/certs/server-truststore.p12
  KC_HTTPS_TRUST_STORE_PASSWORD: $PW
  KC_HTTPS_TRUST_STORE_TYPE: PKCS12
EOF
echo
echo "Test curl (HTTPS, no /auth prefix):"
cat <<'EOF'
curl -v \
  --cert client-with-chain.pem \
  --key  client.key \
  --cacert rootca.crt \
  -d grant_type=client_credentials \
  -d client_id=<your-client-id> \
  https://localhost:8443/realms/<realm>/protocol/openid-connect/token
EOF
