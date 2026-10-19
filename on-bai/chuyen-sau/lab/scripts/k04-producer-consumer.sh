#!/usr/bin/env bash
# Bai 04 — producer va consumer tu code: mac dinh that cua client, batch/linger/nen do bang metric cua client,
# rebalance theo 3 protocol, commit truoc/sau voi kill -9 that, transaction (abort, LSO, zombie) va marker trong log.
# Can cum: docker compose -f docker/cluster.yml up -d --wait
#   scripts/k04-producer-consumer.sh 2>&1 | tee results/k04-producer-consumer.txt
set -uo pipefail
cd "$(dirname "$0")/.."
BIN=/opt/kafka/bin
BS=kafka-1:9092
HOSTBS=localhost:19092,localhost:29092,localhost:39092
step() { printf '\n=== %s\n' "$*"; }
run() { scripts/run.sh "$@" 2>&1 | grep -vE 'JAVA_TOOL_OPTIONS|OffsetFetcherUtils'; }
k() { docker exec kafka-1 "$BIN/$@"; }

step "1. Mac dinh that cua kafka-clients"
run com.prep.cs.kafka.ClientDefaults

step "2. Producer: cung 300.000 event, doi linger/batch/nen/acks, doc metric cua client"
k kafka-topics.sh --bootstrap-server $BS --create --if-not-exists --topic perf --partitions 6 --replication-factor 3
run com.prep.cs.kafka.ProducerTuning $HOSTBS perf 300000

step "3. Consumer group: c1 vao, c2 vao, c3 vao, c2 roi nhom — ba cach chia lai"
k kafka-topics.sh --bootstrap-server $BS --create --if-not-exists --topic orders6 --partitions 6 --replication-factor 3
for m in eager cooperative consumer; do run com.prep.cs.kafka.GroupRebalanceDemo localhost:19092 orders6 $m; echo; done

step "4. Commit truoc hay xu ly truoc: kill -9 tien trinh giua mot batch"
run com.prep.cs.kafka.CommitSemantics localhost:19092 at-least-once 350
run com.prep.cs.kafka.CommitSemantics localhost:19092 at-most-once 350
run com.prep.cs.kafka.CommitSemantics localhost:19092 auto 750

step "5. Transaction: abort/commit, LSO, zombie fencing"
run com.prep.cs.kafka.TxnDemo localhost:19092 | tee /tmp/txn-out.txt
T=$(awk '/^topic /{print $2; exit}' /tmp/txn-out.txt)
L=$(k kafka-topics.sh --bootstrap-server $BS --describe --topic "$T" | awk '$3=="Partition:" {print $6}')
echo "--- trong file log cua $T-0 (leader broker $L): marker COMMIT/ABORT la record dieu khien, chiem mot offset"
docker exec kafka-$L $BIN/kafka-dump-log.sh --files /var/lib/kafka/data/$T-0/00000000000000000000.log --print-data-log \
  | grep -E '^\| offset|isControl: true' \
  | sed -E 's/CreateTime: [0-9]+ //; s/ deleteHorizonMs: [^ ]+//; s/position: [0-9]+ //; s/ crc: [0-9]+ isvalid: true//; s/ headerKeys: \[\]//; s/ size: [0-9]+ magic: 2 compresscodec: none//' \
  | cut -c1-200
echo "--- coordinator nho trang thai transaction trong topic __transaction_state"
k kafka-transactions.sh --bootstrap-server $BS list 2>/dev/null | head -6
true
