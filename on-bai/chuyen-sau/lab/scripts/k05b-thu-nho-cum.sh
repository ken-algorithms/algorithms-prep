#!/usr/bin/env bash
# Bai 05 — thu nho cum (scale-in): rut broker 4 SAI (tat thang) va DUNG (chuyen het replica ra co throttle,
# go throttle, tat, go dang ky). Chay sau k05-scale.sh (luc do kafka-4 dang giu replica).
#   scripts/k05b-thu-nho-cum.sh 2>&1 | tee results/k05b-thu-nho-cum.txt
set -uo pipefail
cd "$(dirname "$0")/.."
BIN=/opt/kafka/bin
BS=kafka-1:9092
k() { docker exec kafka-1 "$BIN/$@"; }
step() { printf '\n=== %s\n' "$*"; }
urp() { k kafka-topics.sh --bootstrap-server $BS --describe --under-replicated-partitions 2>/dev/null | grep -c Partition; }
urp_by_topic() {
  k kafka-topics.sh --bootstrap-server $BS --describe --under-replicated-partitions 2>/dev/null \
    | awk '{print $2}' | sort | uniq -c | awk '{printf "  %-16s %d partition thieu ban sao\n", $2, $1}'; }

step "1. SAI: tat thang broker 4 khi no con giu replica"
docker compose -f docker/cluster.yml --profile scale stop kafka-4 2>&1 | tail -1
sleep 12   # broker.session.timeout.ms = 9 s: controller rao broker 4, bo khoi ISR
echo "partition thieu ban sao (URP): $(urp)"
urp_by_topic
k kafka-topics.sh --bootstrap-server $BS --describe --topic grow 2>/dev/null | awk '$3=="Partition:" {print "  p"$4, $5, $6, $7, $8, $9, $10}'
echo "--- cum van ghi duoc (con 2 ban sao >= min.insync.replicas 2), nhung hong them 1 broker la partition dung ghi"

step "2. Bat lai broker 4: no tu doc tiep tu cho da dung, tu vao lai ISR"
T0=$(date +%s)
docker compose -f docker/cluster.yml --profile scale up -d --wait kafka-4 2>&1 | tail -1
while [ "$(urp)" -gt 0 ]; do sleep 1; done
echo "URP ve 0 sau $(( $(date +%s) - T0 )) giay (tinh ca thoi gian khoi dong JVM cua broker)"

step "3. DUNG: chuyen het replica khoi broker 4, throttle 5 MB/s, go throttle khi xong"
scripts/run.sh com.prep.cs.kafka.Decommission localhost:19092 4 5000000 2>&1 | grep -vE 'JAVA_TOOL_OPTIONS|WARN'
echo "--- throttle con sot tren broker nao khong:"
for b in 1 2 3 4; do k kafka-configs.sh --bootstrap-server $BS --describe --entity-type brokers --entity-name $b 2>/dev/null | grep -c throttled | sed "s/^/  broker $b: /"; done

step "4. Tat broker 4 va go dang ky khoi metadata"
docker compose -f docker/cluster.yml --profile scale stop kafka-4 2>&1 | tail -1
sleep 12
echo "URP sau khi tat: $(urp)"
k kafka-cluster.sh unregister --bootstrap-server $BS --id 4 2>&1 | grep -v WARN
k kafka-broker-api-versions.sh --bootstrap-server $BS 2>/dev/null | grep -oE '^[a-z0-9-]+:[0-9]+ \(id: [0-9]+' | sort

step "5. Chia lai leader (replica dau = leader uu tien)"
k kafka-leader-election.sh --bootstrap-server $BS --election-type preferred --all-topic-partitions 2>&1 \
  | grep Successfully | tr ',' '\n' | wc -l | sed 's/^/  so partition doi leader: /'
k kafka-topics.sh --bootstrap-server $BS --describe 2>/dev/null | awk '$3=="Partition:" {print $6}' | sort | uniq -c | awk '{print "  broker "$2": leader cua "$1" partition"}'
true
