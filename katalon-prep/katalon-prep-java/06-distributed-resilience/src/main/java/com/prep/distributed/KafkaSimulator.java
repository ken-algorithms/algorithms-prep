package com.prep.distributed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * ============================================================================
 * MO PHONG KAFKA — de HOC PATTERN, khong phai de thay Kafka.
 * ============================================================================
 *
 * <p>May nay khong co Docker, ma nhung thu phong van HOI ve Kafka lai la <b>ngu nghia</b>, khong
 * phai thao tac: ordering nam o dau, key quyet dinh gi, rebalance lam mat gi, DLQ de lam gi,
 * exactly-once co that khong. Toan bo nhung cai do mo phong duoc trung thuc bang code thuan.
 *
 * <p><b>Cai mo phong nay TAI TAO trung thuc:</b> partition theo hash cua key, ordering trong tung
 * partition, consumer group + assignment, offset commit, rebalance khi consumer roi nhom,
 * at-least-once (message da xu ly nhung chua commit se duoc giao lai).
 *
 * <p><b>Cai KHONG tai tao</b> (can broker that, ghi ro de khong ao tuong): do tre mang va
 * backpressure that, replication/ISR va do ben khi broker chet, log compaction, transaction
 * (exactly-once) cua Kafka, rebalance protocol thuc te (cooperative sticky).
 */
public final class KafkaSimulator {

    private KafkaSimulator() {}

    public record Message(String key, String value, String eventId) {
        public Message {
            Objects.requireNonNull(value, "value");
            eventId = eventId == null ? key + ":" + value : eventId;
        }
    }

    public record Record(int partition, long offset, Message message) {}

    /**
     * Topic co N partition.
     *
     * <p><b>Diem cot loi ma interviewer luon dao:</b> Kafka chi dam bao THU TU TRONG MOT PARTITION.
     * Message cung key luon vao cung partition, nen "cung key thi dung thu tu". Khong co key ->
     * round-robin -> khong con dam bao gi ve thu tu.
     */
    public static final class Topic {

        private final String name;
        private final int partitionCount;
        private final Map<Integer, List<Record>> partitions = new TreeMap<>();

        public Topic(String name, int partitionCount) {
            if (partitionCount < 1) {
                throw new IllegalArgumentException("partitionCount must be >= 1");
            }
            this.name = name;
            this.partitionCount = partitionCount;
            for (int i = 0; i < partitionCount; i++) {
                partitions.put(i, new ArrayList<>());
            }
        }

        /**
         * Partition duoc chon bang {@code hash(key) % partitionCount}.
         *
         * <p>Dung {@code Math.floorMod} chu KHONG dung {@code %}: hashCode co the AM, va
         * {@code -7 % 3 = -1} -> IndexOutOfBounds. Bug nay that va rat hay gap khi tu viet partitioner.
         */
        public int partitionFor(String key) {
            if (key == null) {
                // Khong co key: round-robin. Danh doi la MAT dam bao thu tu.
                return (int) (totalMessages() % partitionCount);
            }
            return Math.floorMod(key.hashCode(), partitionCount);
        }

        public Record publish(Message message) {
            int partition = partitionFor(message.key());
            List<Record> log = partitions.get(partition);
            var record = new Record(partition, log.size(), message);
            log.add(record);
            return record;
        }

        public List<Record> partition(int index) {
            return List.copyOf(partitions.get(index));
        }

        public long totalMessages() {
            return partitions.values().stream().mapToLong(List::size).sum();
        }

        public int partitionCount() {
            return partitionCount;
        }

        public String name() {
            return name;
        }
    }

    /** Ket qua xu ly mot record cua consumer. */
    public interface Handler {
        /** Nem = xu ly that bai. Khong nem = thanh cong. */
        void handle(Record record);
    }

    /**
     * Consumer group: chia partition cho cac consumer, giu offset da commit.
     *
     * <p>Mo phong dung ba dieu quan trong nhat:
     * <ol>
     *   <li><b>Mot partition chi thuoc ve MOT consumer</b> trong nhom. Do la ly do so consumer
     *       nhieu hon so partition la lang phi — cac consumer thua ngoi khong.
     *   <li><b>Offset commit</b> quyet dinh "da xu ly toi dau". Xu ly xong ma chua commit ma chet
     *       -> message do duoc giao LAI (at-least-once).
     *   <li><b>Rebalance</b> khi consumer roi nhom: partition cua no duoc chia lai cho nhung
     *       consumer con lai, va viec doc tiep tuc tu OFFSET DA COMMIT, khong phai tu cho da xu ly.
     * </ol>
     */
    public static final class ConsumerGroup {

        private final Topic topic;
        private final List<String> members = new ArrayList<>();
        private final Map<Integer, Long> committedOffsets = new TreeMap<>();
        private final Map<Integer, String> assignment = new TreeMap<>();
        private int rebalanceCount;

        public ConsumerGroup(Topic topic) {
            this.topic = topic;
            for (int i = 0; i < topic.partitionCount(); i++) {
                committedOffsets.put(i, 0L);
            }
        }

        public void join(String consumerId) {
            members.add(consumerId);
            rebalance();
        }

        public void leave(String consumerId) {
            members.remove(consumerId);
            rebalance();
        }

        /** Range assignment don gian: chia partition deu cho cac member theo thu tu. */
        private void rebalance() {
            rebalanceCount++;
            assignment.clear();
            if (members.isEmpty()) {
                return;
            }
            for (int p = 0; p < topic.partitionCount(); p++) {
                assignment.put(p, members.get(p % members.size()));
            }
        }

        public List<Integer> partitionsOf(String consumerId) {
            return assignment.entrySet().stream()
                    .filter(e -> e.getValue().equals(consumerId))
                    .map(Map.Entry::getKey)
                    .toList();
        }

        public int rebalanceCount() {
            return rebalanceCount;
        }

        public long committedOffset(int partition) {
            return committedOffsets.get(partition);
        }

        /**
         * Doc va xu ly cac record chua commit cua {@code consumerId}.
         *
         * @param commit true = commit offset sau khi xu ly xong (duong binh thuong);
         *     false = mo phong consumer CHET truoc khi commit -> lan sau se nhan LAI
         * @return so record da xu ly thanh cong
         */
        public int poll(String consumerId, Handler handler, boolean commit) {
            int processed = 0;
            for (int partition : partitionsOf(consumerId)) {
                long from = committedOffsets.get(partition);
                List<Record> log = topic.partition(partition);
                for (int offset = (int) from; offset < log.size(); offset++) {
                    handler.handle(log.get(offset));
                    processed++;
                    if (commit) {
                        committedOffsets.put(partition, offset + 1L);
                    }
                }
            }
            return processed;
        }
    }

    /**
     * DEAD LETTER QUEUE — chan poison message lam ket consumer vinh vien.
     *
     * <p><b>Poison message:</b> mot message khong bao gio xu ly duoc (JSON hong, schema sai).
     * Neu consumer retry mai va khong commit, no se doc lai dung message do vo han — <b>toan bo
     * partition dung lai</b>, cac message phia sau khong bao gio duoc xu ly. Su co nay im lang:
     * khong crash, chi la lag tang dan.
     *
     * <p>DLQ: sau N lan that bai, day message sang topic rieng, COMMIT offset, va di tiep.
     * Danh doi: message do khong duoc xu ly (can nguoi xem lai), nhung ca luong khong bi ket.
     */
    public static final class DeadLetterConsumer {

        private final int maxAttemptsPerMessage;
        private final List<Record> deadLettered = new ArrayList<>();
        private final Set<String> processed = new LinkedHashSet<>();
        private final Map<String, Integer> attemptCounts = new LinkedHashMap<>();

        public DeadLetterConsumer(int maxAttemptsPerMessage) {
            if (maxAttemptsPerMessage < 1) {
                throw new IllegalArgumentException("maxAttemptsPerMessage must be >= 1");
            }
            this.maxAttemptsPerMessage = maxAttemptsPerMessage;
        }

        /** @return true neu da xu ly xong (thanh cong hoac da day sang DLQ) -> duoc phep commit */
        public boolean consume(Record record, Handler handler) {
            String id = record.message().eventId();
            for (int attempt = 1; attempt <= maxAttemptsPerMessage; attempt++) {
                attemptCounts.merge(id, 1, Integer::sum);
                try {
                    handler.handle(record);
                    processed.add(id);
                    return true;
                } catch (RuntimeException ex) {
                    if (!Retries.isRetryable(ex)) {
                        // Loi vinh vien -> khong retry, sang DLQ ngay. Retry chi ton thoi gian.
                        deadLettered.add(record);
                        return true;
                    }
                }
            }
            deadLettered.add(record);
            return true; // van commit -> luong khong bi ket
        }

        public List<Record> deadLettered() {
            return List.copyOf(deadLettered);
        }

        public Set<String> processed() {
            return Set.copyOf(processed);
        }

        public int attemptsFor(String eventId) {
            return attemptCounts.getOrDefault(eventId, 0);
        }
    }

    /** Consumer dem so lan xu ly moi eventId — de chung minh at-least-once va idempotency. */
    public static final class CountingHandler implements Handler {
        private final Map<String, Integer> counts = new LinkedHashMap<>();
        private final List<String> order = new ArrayList<>();

        @Override
        public void handle(Record record) {
            String id = record.message().eventId();
            counts.merge(id, 1, Integer::sum);
            order.add(id);
        }

        public int timesProcessed(String eventId) {
            return counts.getOrDefault(eventId, 0);
        }

        public List<String> order() {
            return List.copyOf(order);
        }

        public Map<String, Integer> counts() {
            return new HashMap<>(counts);
        }
    }
}
