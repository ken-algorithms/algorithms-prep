#!/usr/bin/env bash
# Bai 01 — bay cau hinh tren Kafka 4.x: dat min.insync.replicas=2 o BROKER nhung khong dat o CONTROLLER.
# Controller (ELR bat san) ghi min.insync.replicas cua NO vao metadata thanh mac dinh cap cum, de len broker.
#   scripts/k01b-min-isr-elr.sh 2>&1 | tee results/k01b-min-isr-elr.txt
set -uo pipefail
IMG=apache/kafka:4.1.2
NET=elr-demo
CID=5lPyZ8SYSUmhz8-9WqbKOw
cleanup() { docker rm -f elr-controller elr-broker >/dev/null 2>&1; docker network rm $NET >/dev/null 2>&1; }
cleanup
docker network create $NET >/dev/null
run_cluster() {  # $1 = gia tri min.insync.replicas cho controller (rong = khong dat)
  local extra=()
  [[ -n "$1" ]] && extra=(-e KAFKA_MIN_INSYNC_REPLICAS="$1")
  docker run -d --name elr-controller --network $NET --hostname elr-controller \
    -e KAFKA_NODE_ID=100 -e KAFKA_PROCESS_ROLES=controller -e KAFKA_LISTENERS=CONTROLLER://:9093 \
    -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT \
    -e KAFKA_CONTROLLER_QUORUM_VOTERS=100@elr-controller:9093 -e CLUSTER_ID=$CID "${extra[@]}" $IMG >/dev/null
  docker run -d --name elr-broker --network $NET --hostname elr-broker \
    -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker -e KAFKA_LISTENERS=PLAINTEXT://:9092 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://elr-broker:9092 -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
    -e KAFKA_CONTROLLER_QUORUM_VOTERS=100@elr-controller:9093 -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
    -e KAFKA_MIN_INSYNC_REPLICAS=2 -e CLUSTER_ID=$CID $IMG >/dev/null
  for _ in $(seq 1 60); do
    docker exec elr-broker /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 >/dev/null 2>&1 && break
    sleep 1
  done
  docker exec elr-broker /opt/kafka/bin/kafka-configs.sh --bootstrap-server localhost:9092 --describe --all \
    --entity-type brokers --entity-name 1 | grep -E '^\s+min.insync.replicas=' | sed 's/^ *//'
}
echo "=== 1. Chi broker dat KAFKA_MIN_INSYNC_REPLICAS=2, controller de mac dinh"
run_cluster ""
docker exec elr-broker /opt/kafka/bin/kafka-features.sh --bootstrap-server localhost:9092 describe | grep eligible
cleanup; docker network create $NET >/dev/null
echo
echo "=== 2. Dat them KAFKA_MIN_INSYNC_REPLICAS=2 cho controller"
run_cluster 2
cleanup
