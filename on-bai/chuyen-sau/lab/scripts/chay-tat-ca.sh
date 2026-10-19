#!/usr/bin/env bash
# Chay lai TOAN BO lab tu dau tren cum Kafka moi tinh, ghi moi ket qua vao results/. Khoang 50 phut.
#   scripts/chay-tat-ca.sh
# Moi buoc cung chay rieng duoc (xem README). Thu tu nay de so lieu cac bai khop nhau.
set -uo pipefail
cd "$(dirname "$0")/.."
log() { printf '\n##### %s  %s\n' "$(date +%T)" "$*"; }
BS=localhost:19092,localhost:29092,localhost:39092

log "build"
mvn -q compile && mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt || exit 1

log "bai 01: mot node, ket noi"
docker compose -f docker/cluster.yml --profile scale down -v >/dev/null 2>&1
scripts/k01-chay-va-ket-noi.sh > results/k01-chay-va-ket-noi.txt 2>&1
scripts/k01b-min-isr-elr.sh > results/k01b-min-isr-elr.txt 2>&1

log "dung cum 3 controller + 3 broker + Postgres"
docker compose -f docker/cluster.yml up -d --wait 2>&1 | tail -1

log "bai 02: ben trong broker"
scripts/k02-ben-trong-broker.sh > results/k02-ben-trong-broker.txt 2>&1
log "bai 03: replication, su co, quorum"
scripts/k03-replication-va-su-co.sh > results/k03-replication-va-su-co.txt 2>&1
log "bai 04: producer, consumer, transaction"
scripts/k04-producer-consumer.sh > results/k04-producer-consumer.txt 2>&1

log "bai 06-10: toan bo test JUnit (dem su kien + so cai) tren Kafka + Postgres that"
mvn test > results/test-output-full.txt 2>&1
grep -E '\[LAB\]|^\s{2,}\S|Tests run:|FAIL|BUILD' results/test-output-full.txt | grep -vE 'WARN|^\s+at ' > results/test-output.txt

log "bai 07-09: do ingest, aggregator, HTTP"
scripts/run.sh com.prep.cs.counting.bench.IngestBench "$BS" 1 10 2>&1 | grep -vE 'JAVA_TOOL_OPTIONS|WARN' > results/e-ingest-bench.txt
scripts/run.sh com.prep.cs.counting.bench.IngestBench "$BS" 3 10 2>&1 | grep -vE 'JAVA_TOOL_OPTIONS|WARN' >> results/e-ingest-bench.txt
scripts/run.sh com.prep.cs.counting.bench.AggregatorBench localhost:19092 20000 500 2>&1 | grep -vE 'JAVA_TOOL_OPTIONS|WARN' > results/e-aggregator-bench.txt
scripts/e-http-bench.sh > results/e-http-bench.txt 2>&1
scripts/e-curl-demo.sh > results/e-curl-demo.txt 2>&1
scripts/e-ingest-fanout.sh > results/e-ingest-fanout.txt 2>&1

log "bai 01: trang thai quorum sau khi cum da chay cac bai tren"
{ echo "=== kafka-metadata-quorum describe --status"
  docker exec kafka-1 /opt/kafka/bin/kafka-metadata-quorum.sh --bootstrap-server kafka-1:9092 describe --status
  echo; echo "=== kafka-metadata-quorum describe --replication"
  docker exec kafka-1 /opt/kafka/bin/kafka-metadata-quorum.sh --bootstrap-server kafka-1:9092 describe --replication
} 2>&1 | grep -v WARN > results/k01c-quorum.txt

log "bai 05: them broker, chuyen partition, scale consumer"
scripts/k05-scale.sh > results/k05-scale.txt 2>&1
log "bai 05: rut broker 4 (sai roi dung)"
scripts/k05b-thu-nho-cum.sh > results/k05b-thu-nho-cum.txt 2>&1

log "JMH: dung cum Kafka cho may ranh roi do"
docker compose -f docker/cluster.yml --profile scale stop >/dev/null 2>&1
java -cp "target/classes:$(cat target/cp.txt)" org.openjdk.jmh.Main CountingBench -prof gc 2>&1 \
  | sed -n '/^Benchmark /,$p' | grep -vE 'gc\.(count|time|alloc\.rate )|churn' > results/e-countingbench.txt
docker compose -f docker/cluster.yml up -d --wait 2>&1 | tail -1
log "xong"
