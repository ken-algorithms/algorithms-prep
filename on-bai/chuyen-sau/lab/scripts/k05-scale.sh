#!/usr/bin/env bash
# Bai 05 — scale: them broker thu 4, Kafka KHONG tu chuyen partition; tu lap ke hoach, chuyen co throttle,
# kiem tra, roi bau lai leader uu tien. Kem do key->partition khi tang partition va do song song consumer.
# Can cum: docker compose -f docker/cluster.yml up -d --wait   (script tu bat kafka-4 qua profile "scale")
#   scripts/k05-scale.sh 2>&1 | tee results/k05-scale.txt
set -uo pipefail
cd "$(dirname "$0")/.."
BIN=/opt/kafka/bin
BS=kafka-1:9092
k() { docker exec kafka-1 "$BIN/$@"; }
step() { printf '\n=== %s\n' "$*"; }
say() { printf '%s  %s\n' "$(date +%T)" "$*"; }
sizes() {  # tong byte cua topic tren tung broker (log-dirs tra JSON)
  k kafka-log-dirs.sh --bootstrap-server $BS --describe --broker-list "$2" --topic-list "$1" 2>/dev/null | grep '^{' | python3 -c '
import json,sys
d=json.load(sys.stdin)
for b in d["brokers"]:
    tot=sum(p["size"] for ld in b["logDirs"] for p in ld["partitions"])
    n=sum(1 for ld in b["logDirs"] for p in ld["partitions"])
    print("  broker %d: %2d replica, %7.1f MB" % (b["broker"], n, tot / 1e6))'; }

step "1. Topic 'grow': 6 partition, RF 3, tren 3 broker; ghi ~120 MB"
k kafka-topics.sh --bootstrap-server $BS --create --topic grow --partitions 6 --replication-factor 3
k kafka-producer-perf-test.sh --topic grow --num-records 1200000 --record-size 100 --throughput -1 \
  --producer-props bootstrap.servers=$BS acks=all linger.ms=20 batch.size=262144 | tail -1
k kafka-topics.sh --bootstrap-server $BS --describe --topic grow | awk '$3=="Partition:" {print "  p"$4, $5, $6, $7, $8, $9, $10}'
sizes grow 1,2,3

step "2. Bat broker thu 4"
docker compose -f docker/cluster.yml --profile scale up -d --wait kafka-4 2>&1 | tail -1
k kafka-broker-api-versions.sh --bootstrap-server $BS 2>/dev/null | grep -oE '^[a-z0-9-]+:[0-9]+ \(id: [0-9]+' | sort
echo "--- broker 4 da vao cum, nhung topic cu KHONG tu chuyen sang:"
k kafka-topics.sh --bootstrap-server $BS --describe --topic grow | awk '$3=="Partition:" {print "  p"$4, $5, $6, $7, $8, $9, $10}'
sizes grow 1,2,3,4

step "3. Lap ke hoach chuyen partition sang 4 broker (--generate chi de xuat, chua lam gi)"
docker exec kafka-1 sh -c 'echo "{\"topics\":[{\"topic\":\"grow\"}],\"version\":1}" > /tmp/move.json'
docker exec kafka-1 sh -c "$BIN/kafka-reassign-partitions.sh --bootstrap-server $BS --generate \
  --topics-to-move-json-file /tmp/move.json --broker-list 1,2,3,4 | sed -n '/Proposed/{n;p}' > /tmp/plan.json"
docker exec kafka-1 cat /tmp/plan.json | python3 -c '
import json,sys
for p in json.load(sys.stdin)["partitions"]: print("  p%d -> replicas %s" % (p["partition"], p["replicas"]))'

step "4. Thuc hien co throttle 10 MB/s (gioi han bang thong chep, de khong lam cham traffic that)"
T0=$(date +%s)
k kafka-reassign-partitions.sh --bootstrap-server $BS --execute --reassignment-json-file /tmp/plan.json \
  --throttle 10000000 2>&1 | grep -vE '^\{|^$' | head -4
echo "--- trong luc chuyen: throttle dat thanh dynamic config tren broker"
k kafka-configs.sh --bootstrap-server $BS --describe --entity-type brokers --entity-name 4 2>/dev/null | grep -i throttle | sed 's/^/  /'
k kafka-topics.sh --bootstrap-server $BS --describe --topic grow | awk '$3=="Partition:" && $8 ~ /,.*,.*,/ {print "  dang chuyen p"$4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14}' | head -6
for i in $(seq 1 120); do
  out=$(k kafka-reassign-partitions.sh --bootstrap-server $BS --verify --reassignment-json-file /tmp/plan.json 2>/dev/null)
  if ! grep -q 'still in progress' <<<"$out"; then break; fi
  sleep 2
done
say "chuyen xong sau $(( $(date +%s) - T0 )) giay"
echo "$out" | grep -E 'Throttle|complete' | head -3
k kafka-topics.sh --bootstrap-server $BS --describe --topic grow | awk '$3=="Partition:" {print "  p"$4, $5, $6, $7, $8, $9, $10}'
sizes grow 1,2,3,4

step "5. Bau leader uu tien (replica dau tien trong danh sach) de chia deu leader"
k kafka-leader-election.sh --bootstrap-server $BS --election-type preferred --all-topic-partitions 2>&1 | grep -E 'grow|Valid|elected' | head -8
k kafka-topics.sh --bootstrap-server $BS --describe --topic grow | awk '$3=="Partition:" {print $6}' | sort | uniq -c | awk '{print "  broker "$2": lam leader "$1" partition"}'

step "6. Tang so partition: key doi partition (tinh bang dung ham cua producer)"
scripts/run.sh com.prep.cs.kafka.PartitionMapping 2>&1 | grep -v JAVA_TOOL_OPTIONS

step "7. Song song phia doc toi da = so partition"
scripts/run.sh com.prep.cs.kafka.ScaleConsumers localhost:19092 6000 2>&1 | grep -v JAVA_TOOL_OPTIONS
true
