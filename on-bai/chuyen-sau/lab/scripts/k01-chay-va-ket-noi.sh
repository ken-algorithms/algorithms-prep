#!/usr/bin/env bash
# Bai 01 — chay Kafka mot node bang Docker, doc config sinh ra, vong doi khoi dong, CLI co ban,
# va bon cach ket noi (hai dung, hai sai) de thay advertised.listeners hoat dong the nao.
#   scripts/k01-chay-va-ket-noi.sh 2>&1 | tee results/k01-chay-va-ket-noi.txt
set -uo pipefail
cd "$(dirname "$0")/.."
BIN=/opt/kafka/bin
IMG=apache/kafka:4.1.2
step() { printf '\n=== %s\n' "$*"; }
k() { docker exec kafka "$BIN/$@"; }

step "1. docker compose up: mot node vua la broker vua la controller"
T0=$(date +%s.%N)
docker compose -f docker/kafka-1node.yml up -d --wait 2>&1 | tail -2
printf 'san sang sau %.1f giay\n' "$(echo "$(date +%s.%N) - $T0" | bc)"
docker compose -f docker/kafka-1node.yml ps --format 'table {{.Name}}\t{{.Status}}\t{{.Ports}}'

step "2. Image doi bien moi truong KAFKA_* thanh server.properties"
docker exec kafka sh -c 'sort /opt/kafka/config/server.properties'

step "3. Vong doi khoi dong trong log: bau leader KRaft -> dang ky broker -> bat kip metadata -> mo cong cho client"
docker logs kafka 2>&1 | grep -E 'Completed transition to Leader|BrokerLifecycleManager|Awaiting socket connections|Kafka Server started' \
  | sed -E 's/\(kafka\.[^)]*\)$//; s/\(org\.apache[^)]*\)$//' | sed -E 's/Leader\(localVoterNode=.*epoch=([0-9]+).*/Leader(epoch=\1 ...)/' | cut -c1-200

step "4. CLI: tao topic, ghi co key, doc kem partition/offset, xem lag cua group"
k kafka-topics.sh --bootstrap-server localhost:9092 --create --topic orders --partitions 3 --replication-factor 1
k kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic orders
printf 'alice:order-1\nbob:order-2\nalice:order-3\ncarol:order-4\nbob:order-5\nalice:order-6\n' | \
  docker exec -i kafka $BIN/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic orders \
  --property parse.key=true --property key.separator=:
k kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic orders --from-beginning --group demo \
  --property print.key=true --property print.partition=true --property print.offset=true --timeout-ms 5000 2>/dev/null
k kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group demo 2>/dev/null

NET=$(docker inspect kafka -f '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')
IP=$(docker inspect kafka -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
FAST='echo "default.api.timeout.ms=15000" > /tmp/c.properties; echo "request.timeout.ms=10000" >> /tmp/c.properties;'
probe() {  # $1 mo ta, $2 network, $3 bootstrap
  echo "--- $1  (network=$2, bootstrap=$3)"
  local out
  out=$(docker run --rm --network "$2" $IMG sh -c "$FAST $BIN/kafka-topics.sh --bootstrap-server $3 --list --command-config /tmp/c.properties" 2>&1 \
    | grep -vE '^\s+at |^$' | awk '!seen[$0]++' | sed -E 's/^\[[^]]*\] //; s/ \(org\.apache[^)]*\)$//' | cut -c1-170)
  echo "$out" | grep -vE 'Error while executing|TimeoutException' | head -3
  echo "$out" | grep -E 'Error while executing' | head -1
}
step "5. Bon cach noi vao cung mot broker"
probe "A. tu may host -> localhost:9092 (listener EXTERNAL)" host localhost:9092
probe "B. tu container cung network -> kafka:29092 (listener INTERNAL)" "$NET" kafka:29092
probe "C. tu container cung network -> kafka:9092 (EXTERNAL, quang ba localhost:9092)" "$NET" kafka:9092
probe "D. tu may host -> $IP:29092 (INTERNAL, quang ba kafka:29092)" host "$IP:29092"

step "6. Client Java tu may host"
scripts/run.sh com.prep.cs.kafka.ConnectCheck localhost:9092 2>&1 | grep -v JAVA_TOOL_OPTIONS

step "7. Don dep"
docker compose -f docker/kafka-1node.yml down -v 2>&1 | tail -2
true
