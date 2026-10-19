#!/usr/bin/env bash
# Bai 02 — nhin thang vao o dia cua broker: segment, index, record batch, offset cua consumer, metadata log,
# retention, compaction. Can cum dang chay: docker compose -f docker/cluster.yml up -d --wait
#   scripts/k02-ben-trong-broker.sh 2>&1 | tee results/k02-ben-trong-broker.txt
set -uo pipefail
BIN=/opt/kafka/bin
BS=kafka-1:9092
k() { docker exec kafka-1 "$BIN/$@"; }
step() { printf '\n=== %s\n' "$*"; }
leader_of() { k kafka-topics.sh --bootstrap-server $BS --describe --topic "$1" | awk -v p="$2" '$3=="Partition:" && $4==p {print $6}'; }
# Kafka: partitionFor(group) = Utils.abs(group.hashCode()) % offsets.topic.num.partitions (mac dinh 50)
java_hash_partition() { python3 -c "
h=0
for ch in '$1': h=(31*h+ord(ch)) & 0xFFFFFFFF
h = h - 2**32 if h >= 2**31 else h
print(0 if h == -2**31 else abs(h) % $2)"; }

step "1. Topic 'inside': 1 partition, RF 3, segment.bytes = 1 MiB (gia tri nho nhat Kafka 4.x cho phep)"
k kafka-topics.sh --bootstrap-server $BS --create --topic inside --partitions 1 --replication-factor 3 --config segment.bytes=1048576
k kafka-topics.sh --bootstrap-server $BS --describe --topic inside
L=$(leader_of inside 0); D=/var/lib/kafka/data/inside-0
echo "leader cua inside-0 la broker $L -> xem o dia cua kafka-$L"

step "2. Ghi 3 record co key (console producer: idempotent la mac dinh tu Kafka 3.0)"
printf 'k1:v1\nk2:v2\nk1:v3\n' | docker exec -i kafka-1 $BIN/kafka-console-producer.sh --bootstrap-server $BS --topic inside \
  --property parse.key=true --property key.separator=:
docker exec kafka-$L ls -l $D

step "3. File .log: record batch voi producerId, epoch, sequence"
docker exec kafka-$L $BIN/kafka-dump-log.sh --files $D/00000000000000000000.log --print-data-log

step "4. Ghi 30.000 record x 100 byte (~3 MB) de vuot 1 MiB -> segment moi"
k kafka-producer-perf-test.sh --topic inside --num-records 30000 --record-size 100 --throughput -1 \
  --producer-props bootstrap.servers=$BS acks=all linger.ms=5 batch.size=16384 | tail -1
docker exec kafka-$L ls -l $D

step "5. Index thua (.index): offset -> vi tri byte. Them mot muc khi da ghi >= 4 KB (index.interval.bytes), chi o ranh gioi batch"
docker exec kafka-$L $BIN/kafka-dump-log.sh --files $D/00000000000000000000.index | head -8
echo "..."
echo "so muc index trong segment dau: $(docker exec kafka-$L $BIN/kafka-dump-log.sh --files $D/00000000000000000000.index | grep -c '^offset:')"

step "6. Time index (.timeindex): timestamp -> offset"
docker exec kafka-$L $BIN/kafka-dump-log.sh --files $D/00000000000000000000.timeindex | head -5

step "7. Hai file nho: leader-epoch-checkpoint (epoch -> offset bat dau) va partition.metadata (topic id)"
docker exec kafka-$L sh -c "echo '--- leader-epoch-checkpoint'; cat $D/leader-epoch-checkpoint; echo '--- partition.metadata'; cat $D/partition.metadata"

step "7b. Producer state snapshot (.snapshot): broker nho sequence cuoi cua moi producer de chan ban ghi trung"
SNAP=$(docker exec kafka-$L sh -c "ls $D/*.snapshot | head -1")
docker exec kafka-$L $BIN/kafka-dump-log.sh --files "$SNAP" | head -3 | cut -c1-260

step "8. Dau-cuoi cua log: kafka-get-offsets (earliest = -2, latest = -1)"
k kafka-get-offsets.sh --bootstrap-server $BS --topic inside --time -2
k kafka-get-offsets.sh --bootstrap-server $BS --topic inside --time -1

step "9. Offset da commit cua consumer cung la record: topic __consumer_offsets"
k kafka-console-consumer.sh --bootstrap-server $BS --topic inside --group g-inside --from-beginning \
  --max-messages 5 --timeout-ms 10000 >/dev/null 2>&1
P=$(java_hash_partition g-inside 50)
echo "group 'g-inside' -> partition abs(hashCode) % 50 = $P cua __consumer_offsets"
LO=$(leader_of __consumer_offsets $P)
echo "leader cua __consumer_offsets-$P (group coordinator) la broker $LO"
docker exec kafka-$LO $BIN/kafka-dump-log.sh --offsets-decoder \
  --files /var/lib/kafka/data/__consumer_offsets-$P/00000000000000000000.log | grep '^| offset' | head -4 | cut -c1-330
k kafka-consumer-groups.sh --bootstrap-server $BS --describe --group g-inside 2>/dev/null

step "10. Metadata cua ca cum cung la mot log: __cluster_metadata tren controller (KRaft)"
docker exec controller-1 $BIN/kafka-dump-log.sh --cluster-metadata-decoder \
  --files /var/lib/kafka/data/__cluster_metadata-0/00000000000000000000.log \
  | grep -oE '"type":"(TOPIC_RECORD|PARTITION_RECORD|REGISTER_BROKER_RECORD|FEATURE_LEVEL_RECORD|CONFIG_RECORD|PRODUCER_IDS_RECORD|BROKER_REGISTRATION_CHANGE_RECORD|PARTITION_CHANGE_RECORD)"' \
  | sort | uniq -c | sort -rn
echo "--- record TopicRecord + PartitionRecord cua topic 'inside':"
docker exec controller-1 $BIN/kafka-dump-log.sh --cluster-metadata-decoder \
  --files /var/lib/kafka/data/__cluster_metadata-0/00000000000000000000.log \
  | grep -oE '\{"type":"(TOPIC|PARTITION)_RECORD","version":[0-9]+,"data":\{"(name":"inside|partitionId":0,"topicId":"'"$(k kafka-topics.sh --bootstrap-server $BS --describe --topic inside | awk '/TopicId/{print $4; exit}')"')[^}]*\}' | head -2

step "11. Retention: topic 'short' giu 15 giay, segment.ms 5 giay; broker quet moi 5 giay"
k kafka-topics.sh --bootstrap-server $BS --create --topic short --partitions 1 --replication-factor 3 \
  --config retention.ms=15000 --config segment.ms=5000
for i in 1 2 3 4; do
  k kafka-producer-perf-test.sh --topic short --num-records 1000 --record-size 100 --throughput -1 \
    --producer-props bootstrap.servers=$BS >/dev/null 2>&1
  sleep 6
done
LS=$(leader_of short 0)
echo "--- sau 4 lan ghi cach nhau 6 giay (segment da roll):"
docker exec kafka-$LS sh -c 'ls -1 /var/lib/kafka/data/short-0 | grep -E "\.log(\.deleted)?$"'
k kafka-get-offsets.sh --bootstrap-server $BS --topic short --time -2
sleep 25
echo "--- 25 giay sau: segment cu het han bi xoa CA FILE, log-start-offset nhay len"
docker exec kafka-$LS sh -c 'ls -1 /var/lib/kafka/data/short-0 | grep -E "\.log(\.deleted)?$"'
k kafka-get-offsets.sh --bootstrap-server $BS --topic short --time -2
k kafka-get-offsets.sh --bootstrap-server $BS --topic short --time -1
docker logs kafka-$LS 2>&1 | grep -E 'short-0.*(Deleting segment|retention time)' | head -3 | cut -c1-260

step "12. Compaction: topic 'registry' (cleanup.policy=compact) giu ban moi nhat cua moi key"
k kafka-topics.sh --bootstrap-server $BS --create --topic registry --partitions 1 --replication-factor 3 \
  --config cleanup.policy=compact --config segment.ms=10000 --config min.cleanable.dirty.ratio=0.01 \
  --config delete.retention.ms=5000
produce_registry() { docker exec -i kafka-1 $BIN/kafka-console-producer.sh --bootstrap-server $BS --topic registry \
  --property parse.key=true --property key.separator=: --property null.marker=NULL; }
read_registry() { k kafka-console-consumer.sh --bootstrap-server $BS --topic registry --from-beginning --timeout-ms 5000 \
  --property print.key=true --property print.offset=true 2>/dev/null; }
printf 'checkout_v2:valid@v1\nsearch:valid@v1\ncheckout_v2:valid@v2\nlegacy_api:valid@v1\nsearch:valid@v2\n' | produce_registry
printf 'legacy_api:NULL\n' | produce_registry      # tombstone: value = null nghia la xoa key
echo "--- (a) vua ghi xong: moi phien ban con nguyen, offset 5 la tombstone"
read_registry
sleep 11; printf 'roll1:x\n' | produce_registry  # ghi sau segment.ms -> segment [0..5] dong lai, cleaner moi duoc dong vao
sleep 25
echo "--- (b) cleaner chay xong lan 1: con ban moi nhat moi key, tombstone van giu (delete.retention.ms)"
read_registry
sleep 11; printf 'roll2:x\n' | produce_registry  # them du lieu ban de cleaner co ly do chay lan 2
sleep 25
echo "--- (c) lan 2, qua delete.retention.ms: tombstone bien mat, key legacy_api khong con dau vet"
read_registry
LR=$(leader_of registry 0)
docker exec kafka-$LR sh -c 'grep -h "cleaned log registry-0" /opt/kafka/logs/log-cleaner.log 2>/dev/null' | sed 's/^[[:space:]]*//'
true
