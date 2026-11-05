#!/bin/bash
# Checks a built image the way a user runs it: no configuration, a published port and a volume.
#
#   docker build -t topicman:test . && scripts/test-image.sh topicman:test
set -euo pipefail

image=${1:?Usage: test-image.sh IMAGE}
name=topicman-image-test-$$
volume=$name
port=${TEST_PORT:-18080}
api=http://127.0.0.1:$port/api/v1

cleanup() {
  docker rm -f "$name" > /dev/null 2>&1 || true
  docker volume rm "$volume" > /dev/null 2>&1 || true
}
trap cleanup EXIT

start() {
  docker run -d --name "$name" -p "127.0.0.1:$port:8080" -v "$volume:/data" "$image" > /dev/null
  # Healthy means started: it reports 503 until the example data has loaded, after the port opens.
  for _ in $(seq 1 60); do
    if [ "$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$port/actuator/health")" = 200 ]; then
      return
    fi
    if [ "$(docker inspect -f '{{.State.Running}}' "$name")" != true ]; then
      break
    fi
    sleep 1
  done
  docker logs "$name"
  echo "FAILED: the app did not start" >&2
  exit 1
}

stop() {
  docker stop "$name" > /dev/null
  docker rm "$name" > /dev/null
}

check() {
  local what=$1 expected=$2 actual=$3
  if [ "$actual" != "$expected" ]; then
    docker logs "$name" | tail -40
    echo "FAILED: $what: expected '$expected', got '$actual'" >&2
    exit 1
  fi
  echo "ok: $what"
}

connections() {
  curl -fsS "$api/connections" | grep -o '"id":' | wc -l | tr -d ' '
}

start
check 'runs as a non-root user' 1000 "$(docker exec "$name" id -u)"
check 'serves the UI' 200 "$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:$port/")"
check 'only its owner can read the generated key' '600 1000' "$(docker exec "$name" stat -c '%a %u' /data/master.key)"
check 'loads the example connections' 8 "$(connections)"

id=$(curl -fsS -X POST "$api/connections" -H 'Content-Type: application/json' -d \
  '{"name":"survives","brokerType":"PULSAR","address":"pulsar://broker:6650","credentials":{"type":"TOKEN","token":"secret-token"}}' \
  | sed -E 's/.*"id":([0-9]+).*/\1/')
key=$(docker exec "$name" sha256sum /data/master.key)
check 'never logs a credential' 0 "$(docker logs "$name" 2>&1 | grep -c 'secret-token' || true)"
curl -fsS -X DELETE "$api/connections/1"

stop
start
check 'keeps its data across a new container' 8 "$(connections)"
check 'does not bring a deleted example back' 404 \
  "$(curl -s -o /dev/null -w '%{http_code}' "$api/connections/1")"
check 'keeps a saved connection' 200 "$(curl -s -o /dev/null -w '%{http_code}' "$api/connections/$id")"
check 'keeps the master key' "$key" "$(docker exec "$name" sha256sum /data/master.key)"

echo 'The image works.'
