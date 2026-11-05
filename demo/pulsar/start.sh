#!/bin/bash
# Starts Pulsar standalone, then adds sample messages to the topics of the example collections. The
# healthcheck waits for /tmp/ready, so the broker counts as healthy only once they are there.
set -euo pipefail

# Writes the PULSAR_PREFIX_ settings in compose.demo.yaml into the configuration.
bin/apply-config-from-env.py conf/standalone.conf

bin/pulsar standalone &
broker=$!
trap 'kill -TERM "$broker"; wait "$broker"' TERM INT

until bin/pulsar-admin brokers healthcheck > /dev/null 2>&1; do
  sleep 1
done

# A separator of its own, as the default comma would split the JSON. It is a regular expression, so
# not `|`, which would split between all characters.
produce() {
  bin/pulsar-client produce "$1" --key "$2" --separator '~' --messages "$3" > /dev/null
}
produce user-events customer-1001 '{"userId":"customer-1001","email":"dev@example.com","at":1767225600000}'
produce user-events customer-2002 '{"userId":"customer-2002","email":"qa@example.com","at":1767229200000}'
produce audit-log customer-1001 '2026-01-01T00:00:00Z local user customer-1001 logged in'
produce audit-log customer-2002 '2026-01-01T01:00:00Z eu-west user customer-2002 logged in'
touch /tmp/ready

wait "$broker"
