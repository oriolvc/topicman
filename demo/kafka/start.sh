#!/bin/bash
# Starts Kafka as the image does, then creates the demo user's SCRAM credentials, which only exist
# once the broker runs. The healthcheck waits for /tmp/ready, so the broker counts as healthy only
# when every mechanism works.
set -euo pipefail

# A PEM keystore holds the private key and the certificate in one file.
cat /etc/kafka/tls/server-key.pem /etc/kafka/tls/server.pem > /tmp/server-keystore.pem

/etc/kafka/docker/run &
broker=$!
trap 'kill -TERM "$broker"; wait "$broker"' TERM INT

until /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1; do
  sleep 1
done
# Kafka changes a user's credentials at most once per request, so one call per mechanism.
for mechanism in SCRAM-SHA-256 SCRAM-SHA-512; do
  /opt/kafka/bin/kafka-configs.sh --bootstrap-server localhost:9092 --alter \
    --entity-type users --entity-name demo --add-config "$mechanism=[password=demo-secret]" > /dev/null
done
touch /tmp/ready

wait "$broker"
