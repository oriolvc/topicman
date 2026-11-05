#!/bin/bash
# Starts Kafka as the image does, then creates the demo user's SCRAM credentials, which only exist
# once the broker runs, and adds sample messages. The healthcheck waits for /tmp/ready, so the broker
# counts as healthy only when every mechanism works and the samples are there.
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

# Sample messages for the topics of the example collections, as `key|value`.
produce() {
  /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic "$1" \
    --reader-property parse.key=true --reader-property 'key.separator=|' > /dev/null
}
produce orders <<'MESSAGES'
customer-1001|{"orderId":"sample-1","customerId":"customer-1001","quantity":2,"createdAt":"2026-01-01T00:00:00Z"}
customer-2002|{"orderId":"sample-2","customerId":"customer-2002","quantity":5,"createdAt":"2026-01-01T01:00:00Z"}
MESSAGES
produce order-notes <<'MESSAGES'
customer-1001|Customer customer-1001 asked to deliver after 18:00 (local).
customer-2002|Customer customer-2002 asked for a gift wrap (eu-west).
MESSAGES
touch /tmp/ready

wait "$broker"
