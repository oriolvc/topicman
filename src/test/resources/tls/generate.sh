#!/usr/bin/env bash
# Generates the TEST-ONLY certificates in this directory (ADR 0008). Never use them outside tests:
# their private keys are public, in this repository.
#
#   ca.pem                 test CA certificate, which connections under test trust
#   server.pem             server certificate for localhost and 127.0.0.1, issued by the test CA
#   server-key.pem         its private key (unencrypted PKCS#8, as Pulsar and Kafka expect)
#   other-ca.pem           an unrelated CA, to test that a broker with an untrusted certificate fails
#   wrong-host.pem         a certificate issued by the test CA for another host, to test hostname checks
#   wrong-host-key.pem     its private key
#
# Valid for 100 years. Run from anywhere: ./src/test/resources/tls/generate.sh
set -euo pipefail
cd "$(dirname "$0")"
days=36500
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

openssl req -x509 -newkey rsa:2048 -nodes -days "$days" -subj '/CN=TopicMan Test CA' \
    -addext 'basicConstraints=critical,CA:TRUE' -addext 'keyUsage=critical,keyCertSign,cRLSign' \
    -keyout "$work/ca-key.pem" -out ca.pem 2>/dev/null

issue() { # name, common name, subjectAltName
    openssl req -newkey rsa:2048 -nodes -subj "/CN=$2" -keyout "$work/$1-key.pem" -out "$work/$1.csr" 2>/dev/null
    printf 'subjectAltName=%s\nbasicConstraints=CA:FALSE\nkeyUsage=digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\n' "$3" > "$work/$1.ext"
    openssl x509 -req -in "$work/$1.csr" -CA ca.pem -CAkey "$work/ca-key.pem" -CAcreateserial -CAserial "$work/ca.srl" \
        -days "$days" -extfile "$work/$1.ext" -out "$1.pem" 2>/dev/null
    openssl pkcs8 -topk8 -nocrypt -in "$work/$1-key.pem" -out "$1-key.pem"
}
issue server localhost 'DNS:localhost,IP:127.0.0.1'
issue wrong-host broker.example.com 'DNS:broker.example.com'

openssl req -x509 -newkey rsa:2048 -nodes -days "$days" -subj '/CN=Unrelated Test CA' \
    -addext 'basicConstraints=critical,CA:TRUE' -keyout "$work/other-ca-key.pem" -out other-ca.pem 2>/dev/null
# Readable by the broker processes inside the containers, which do not run as this user.
chmod 644 ./*.pem
