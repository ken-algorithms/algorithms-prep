#!/usr/bin/env bash
# Bai 03 — replication duoi tai that: tat follower, mat min.insync.replicas, tat leader em va kill leader,
# roi tai hien MAT DU LIEU DA XAC NHAN voi acks=1 va doi chung voi acks=all.
# Can cum: docker compose -f docker/cluster.yml up -d --wait
#   scripts/k03-replication-va-su-co.sh 2>&1 | tee results/k03-replication-va-su-co.txt
set -uo pipefail
cd "$(dirname "$0")/.."
BIN=/opt/kafka/bin
HOSTBS=localhost:19092,localhost:29092,localhost:39092
step() { printf '\n=== %s\n' "$*"; }
say() { printf '%s  %s\n' "$(date +%T)" "$*"; }
alive() { docker exec kafka-$1 true 2>/dev/null && [[ "$(docker inspect -f '{{.State.Paused}}' kafka-$1)" == false ]]; }
any_alive() { local b; for b in 1 2 3; do alive $b && { echo $b; return; }; done; }
desc() {  # dong partition 0 cua topic, hoi tu mot broker con song
  local b; b=$(any_alive)
  docker exec kafka-$b $BIN/kafka-topics.sh --bootstrap-server kafka-$b:9092 --describe --topic "$1" 2>/dev/null \
    | awk -v t="$(date +%T)" '$3=="Partition:" && $4=="0" {printf "%s  describe: Leader=%s Replicas=%s Isr=%s Elr=%s\n", t, $6, $8, $10, ($11=="Elr:" ? $12 : "")}'
}
field() { desc "$1" | sed -E "s/.*$2=([^ ]*).*/\1/"; }
wait_until() { local i; for i in $(seq 1 "$1"); do eval "$2" && return 0; sleep 1; done; return 1; }
isr_full() { [[ "$(field "$1" Isr)" == *,*,* ]]; }
steady() { grep -qE "xac-nhan= *([3-9][0-9]{2}|[0-9]{4,})" "$1" 2>/dev/null || \
           [[ $(grep -cE 'xac-nhan= *(9[0-9]|[1-9][0-9]{2,})' "$1" 2>/dev/null) -ge 3 ]]; }
start_producer() {  # $1 topic, $2 acks, $3 rate -> chay nen, ghi log vao results/k03-$1-producer.txt
  rm -f "results/stop-$1"
  scripts/run.sh com.prep.cs.kafka.SteadyProducer "$HOSTBS" "$1" "$2" "$3" 600 "results/acked-$1.txt" "results/stop-$1" \
    > "results/k03-$1-producer.txt" 2>&1 &
  PROD=$!
  wait_until 60 "steady results/k03-$1-producer.txt" && say "producer da on dinh (co xac nhan deu)"
}
stop_producer() { touch "results/stop-$1"; wait $PROD; rm -f "results/stop-$1"; }
producer_report() {  # cac dong moi giay + vai loi dau tien moi loai + tong
  echo "--- producer ($1): dong moi giay va loi"
  grep -E '^[0-9:]{8} t=' "results/k03-$1-producer.txt"
  grep -oE 'Error: [A-Z_]+|could not be established|Disconnected from node [0-9]' "results/k03-$1-producer.txt" | sort | uniq -c
  grep -E '^TONG' "results/k03-$1-producer.txt"
}
check_loss() { scripts/run.sh com.prep.cs.kafka.AckLossCheck "$HOSTBS" "$1" "results/acked-$1.txt" 2>&1 | grep -v JAVA_TOOL_OPTIONS; }

mvn -q compile 2>&1 | grep -v JAVA_TOOL_OPTIONS

# ---------------------------------------------------------------------------------------------------------
step "A. acks=all, RF 3, min.insync.replicas 2: tat dan follower, tat leader em, roi kill leader"
docker exec kafka-1 $BIN/kafka-topics.sh --bootstrap-server kafka-1:9092 --create --topic pay --partitions 1 --replication-factor 3
desc pay
start_producer pay all 100
L=$(field pay Leader); F=( $(echo 1 2 3 | tr ' ' '\n' | grep -v "^$L$") )
say "leader=$L, follower=${F[*]}"
sleep 3
say "docker stop kafka-${F[0]}   (follower thu nhat, tat em)"; docker stop kafka-${F[0]} >/dev/null; desc pay
sleep 5
say "docker stop kafka-${F[1]}   (follower thu hai -> ISR chi con leader, duoi min.insync.replicas = 2)"; docker stop kafka-${F[1]} >/dev/null; desc pay
sleep 8
say "docker start kafka-${F[0]} kafka-${F[1]}"; docker start kafka-${F[0]} kafka-${F[1]} >/dev/null
wait_until 90 "isr_full pay"; say "ISR du 3 lai"; desc pay
sleep 5
L=$(field pay Leader)
say "docker stop kafka-$L   (LEADER, tat em: controlled shutdown, bao controller chuyen leader truoc khi tat)"; docker stop kafka-$L >/dev/null; desc pay
sleep 5
say "docker start kafka-$L"; docker start kafka-$L >/dev/null; wait_until 90 "isr_full pay"; say "ISR du 3 lai"; desc pay
sleep 5
L=$(field pay Leader)
say "docker kill kafka-$L   (LEADER, SIGKILL: khong kip bao ai)"; docker kill kafka-$L >/dev/null; desc pay
wait_until 30 "[[ \$(field pay Leader) != $L ]]"; say "da co leader moi"; desc pay
sleep 5
stop_producer pay
docker start kafka-$L >/dev/null; wait_until 90 "isr_full pay"; desc pay
producer_report pay
echo "--- kafka-$L vua bi SIGKILL, khoi dong lai: log recovery cua pay-0"
docker logs kafka-$L 2>&1 | grep 'pay-0' | grep -iE 'recover|truncat' | tail -3 | cut -c1-230
check_loss pay

# ---------------------------------------------------------------------------------------------------------
lost_scenario() {  # $1 acks, $2 topic, $3 tieu de
  step "$3"
  docker exec kafka-1 $BIN/kafka-topics.sh --bootstrap-server kafka-1:9092 --create --topic $2 --replica-assignment 1:2:3
  desc $2
  start_producer $2 $1 500
  sleep 2
  say "docker pause kafka-2 kafka-3   (follower dung fetch; trong ISR toi khi qua replica.lag.time.max.ms = 10s)"
  docker pause kafka-2 kafka-3 >/dev/null
  sleep 3
  say "docker kill kafka-1   (leader chet)"; docker kill kafka-1 >/dev/null
  sleep 1
  say "docker unpause kafka-2 kafka-3"; docker unpause kafka-2 kafka-3 >/dev/null
  wait_until 30 "[[ \$(field $2 Leader) != 1 ]]"; say "da co leader moi"; desc $2
  sleep 5
  stop_producer $2
  docker start kafka-1 >/dev/null; wait_until 90 "isr_full $2"; desc $2
  producer_report $2
  echo "--- kafka-1 quay lai: cat phan log ma leader moi khong co"
  docker logs kafka-1 2>&1 | grep "$2-0" | grep -iE 'truncat' | tail -3 | cut -c1-260
  check_loss $2
}
lost_scenario 1   pay1 "B. acks=1: leader xac nhan ngay khi ghi vao log cua NO; follower dung, leader chet"
lost_scenario all pay2 "C. Doi chung: dung kich ban B nhung acks=all"

# ---------------------------------------------------------------------------------------------------------
step "D. Quorum controller KRaft: mat leader cua quorum, roi mat ca quorum"
quorum() { timeout 20 docker exec kafka-1 $BIN/kafka-metadata-quorum.sh --bootstrap-server kafka-1:9092 describe --status 2>/dev/null \
  | awk '/LeaderId|LeaderEpoch|HighWatermark/ {printf "%s=%s ", $1, $2} END {print ""}'; }
probe() { scripts/run.sh com.prep.cs.kafka.QuorumProbe localhost:19092 pay 2>&1 | grep -v JAVA_TOOL_OPTIONS; }
say "quorum: $(quorum)"
LC=$(quorum | sed -E 's/.*LeaderId:=([0-9]+).*/\1/')
say "docker stop controller-$((LC - 100))   (leader cua quorum)"; docker stop controller-$((LC - 100)) >/dev/null
wait_until 30 "[[ -n \"\$(quorum)\" ]]"; sleep 2; say "quorum: $(quorum)"
probe
LC2=$(quorum | sed -E 's/.*LeaderId:=([0-9]+).*/\1/')
say "docker stop controller-$((LC2 - 100))   (con 1/3 controller: MAT QUORUM)"; docker stop controller-$((LC2 - 100)) >/dev/null
sleep 3
probe
say "producer acks=1 (khong idempotent, khong can producer id) ghi vao topic co san:"
scripts/run.sh com.prep.cs.kafka.SteadyProducer "$HOSTBS" pay 1 100 5 results/acked-quorum1.txt 2>&1 | grep -E '^TONG'
say "producer acks=all (idempotent: phai xin producer id) ghi vao topic co san:"
scripts/run.sh com.prep.cs.kafka.SteadyProducer "$HOSTBS" pay all 100 8 results/acked-quorum2.txt 2>&1 | grep -E '^TONG'
for b in 1 2 3; do docker logs kafka-$b 2>&1 | grep 'AllocateProducerIds' | tail -1 | cut -c1-200; done
say "docker start controller-$((LC - 100)) controller-$((LC2 - 100))"
docker start controller-$((LC - 100)) controller-$((LC2 - 100)) >/dev/null
wait_until 60 "[[ -n \"\$(quorum)\" ]]"; sleep 2; say "quorum: $(quorum)"
probe
true
