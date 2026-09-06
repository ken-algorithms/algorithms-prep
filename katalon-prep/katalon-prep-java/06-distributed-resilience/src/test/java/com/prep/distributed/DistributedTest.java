package com.prep.distributed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.prep.distributed.KafkaSimulator.ConsumerGroup;
import com.prep.distributed.KafkaSimulator.CountingHandler;
import com.prep.distributed.KafkaSimulator.DeadLetterConsumer;
import com.prep.distributed.KafkaSimulator.Message;
import com.prep.distributed.KafkaSimulator.Topic;
import com.prep.distributed.Retries.Backoff;
import com.prep.distributed.Retries.NonRetryableException;
import com.prep.distributed.Retries.Policy;
import com.prep.distributed.Retries.TransientException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongUnaryOperator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DistributedTest {

    /** Nguon "ngau nhien" deterministic: luon tra 30% cua bound. */
    private static final LongUnaryOperator FIXED_RANDOM = bound -> (long) (bound * 0.3);

    // ==================================================================
    // RETRY + BACKOFF + JITTER
    // ==================================================================

    @Nested
    @DisplayName("Retry / backoff / jitter")
    class RetryPolicies {

        @Test
        @DisplayName("EXPONENTIAL: 0, 100, 200, 400, 800 ms - va bi chan boi cap")
        void exponentialBackoffIsCapped() {
            var policy = new Policy(
                    8, Duration.ofMillis(100), Duration.ofMillis(500), Backoff.EXPONENTIAL, FIXED_RANDOM);

            assertEquals(Duration.ZERO, policy.delayBefore(1), "lan dau khong cho");
            assertEquals(Duration.ofMillis(100), policy.delayBefore(2));
            assertEquals(Duration.ofMillis(200), policy.delayBefore(3));
            assertEquals(Duration.ofMillis(400), policy.delayBefore(4));
            // Bi CAP chan lai - neu khong, lan thu 20 se cho hang ngay.
            assertEquals(Duration.ofMillis(500), policy.delayBefore(5));
            assertEquals(Duration.ofMillis(500), policy.delayBefore(8));
        }

        @Test
        @DisplayName("THUNDERING HERD: khong jitter -> 1000 client cho GIONG HET nhau")
        void withoutJitterEveryClientRetriesAtTheSameInstant() {
            var noJitter = new Policy(
                    5, Duration.ofMillis(100), Duration.ofSeconds(10), Backoff.EXPONENTIAL, FIXED_RANDOM);

            // Mo phong 1000 client cung gap loi cung luc.
            var delays = new HashSet<Long>();
            for (int client = 0; client < 1000; client++) {
                delays.add(noJitter.delayBefore(3).toMillis());
            }

            // TAT CA cho dung 200ms -> dap vao downstream cung mot thoi diem.
            assertEquals(1, delays.size(), "khong jitter -> moi client dong bo hoa");
            assertEquals(200L, delays.iterator().next());
        }

        @Test
        @DisplayName("FULL JITTER: cung tham so nhung delay RAI DEU tren [0, bound]")
        void fullJitterSpreadsRetriesAcrossTheWindow() {
            // Random that (khac nhau moi client) - dung LCG deterministic de test on dinh.
            var seed = new java.util.concurrent.atomic.AtomicLong(42);
            LongUnaryOperator pseudoRandom = bound -> {
                long next = seed.updateAndGet(s -> (s * 6364136223846793005L + 1442695040888963407L));
                return Math.floorMod(next, Math.max(1, bound));
            };
            var jittered = new Policy(
                    5, Duration.ofMillis(100), Duration.ofSeconds(10),
                    Backoff.EXPONENTIAL_FULL_JITTER, pseudoRandom);

            var delays = new ArrayList<Long>();
            for (int client = 0; client < 1000; client++) {
                delays.add(jittered.delayBefore(3).toMillis());
            }

            long distinct = delays.stream().distinct().count();
            long max = delays.stream().mapToLong(Long::longValue).max().orElseThrow();
            long min = delays.stream().mapToLong(Long::longValue).min().orElseThrow();

            // Rai ra hang tram gia tri khac nhau thay vi don ve 1 diem.
            assertTrue(distinct > 100, "chi co %d gia tri khac nhau".formatted(distinct));
            assertTrue(max <= 200, "khong vuot bound");
            assertTrue(min < 50, "co ca nhung client retry rat som");
        }

        @Test
        @DisplayName("EQUAL JITTER: luon co san sau toi thieu (khong retry ngay tuc thi)")
        void equalJitterKeepsAMinimumFloor() {
            var policy = new Policy(
                    5, Duration.ofMillis(100), Duration.ofSeconds(10),
                    Backoff.EXPONENTIAL_EQUAL_JITTER, FIXED_RANDOM);

            // bound = 200 -> half = 100, jitter = 30% cua 101 = 30 -> 130ms
            long delay = policy.delayBefore(3).toMillis();
            assertTrue(delay >= 100, "equal jitter luon >= nua bound: " + delay);
            assertTrue(delay <= 200, "va khong vuot bound: " + delay);
        }

        @Test
        @DisplayName("KHONG retry loi cua chinh minh (400/validation) - retry vo nghia")
        void nonRetryableErrorsStopImmediately() {
            var policy = new Policy(
                    5, Duration.ofMillis(10), Duration.ofSeconds(1), Backoff.EXPONENTIAL, FIXED_RANDOM);
            var calls = new AtomicInteger();

            var outcome = Retries.execute(policy, () -> {
                calls.incrementAndGet();
                throw new NonRetryableException("field 'email' is invalid");
            });

            assertFalse(outcome.succeeded());
            assertEquals(1, calls.get(), "loi cua phia goi -> KHONG duoc retry");
            assertEquals(1, outcome.attemptCount());
        }

        @Test
        @DisplayName("retry loi tam thoi cho den khi thanh cong, va ghi lai lich cho")
        void transientErrorsAreRetriedUntilSuccess() {
            var policy = new Policy(
                    5, Duration.ofMillis(100), Duration.ofSeconds(10), Backoff.EXPONENTIAL, FIXED_RANDOM);
            var calls = new AtomicInteger();

            var outcome = Retries.execute(policy, () -> {
                if (calls.incrementAndGet() < 3) {
                    throw new TransientException("connection reset");
                }
                return "ok";
            });

            assertTrue(outcome.succeeded());
            assertEquals("ok", outcome.value());
            assertEquals(3, outcome.attemptCount());
            // 0 + 100 + 200 = 300ms tong thoi gian cho.
            assertEquals(Duration.ofMillis(300), outcome.totalDelay());
        }

        @Test
        @DisplayName("het luot van fail -> tra ve that bai kem DAY DU lich su cac lan thu")
        void exhaustedRetriesReportEveryAttempt() {
            var policy = new Policy(
                    3, Duration.ofMillis(50), Duration.ofSeconds(1), Backoff.EXPONENTIAL, FIXED_RANDOM);

            var outcome = Retries.execute(policy, () -> {
                throw new TransientException("still down");
            });

            assertFalse(outcome.succeeded());
            assertEquals(3, outcome.attemptCount());
            assertTrue(outcome.attempts().stream().noneMatch(Retries.Attempt::succeeded));
            // Lich su nay la thu ban can trong log de chan doan, khong phai chi "failed after 3 tries".
            assertEquals("TransientException", outcome.attempts().getFirst().error());
        }

        @Test
        @DisplayName("cau hinh vo ly bi chan tai constructor (cap < base, maxAttempts < 1)")
        void invalidPolicyRejected() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Policy(0, Duration.ofMillis(1), Duration.ofSeconds(1), Backoff.FIXED, FIXED_RANDOM));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Policy(3, Duration.ofSeconds(5), Duration.ofSeconds(1), Backoff.FIXED, FIXED_RANDOM));
        }
    }

    // ==================================================================
    // PARTITION & ORDERING
    // ==================================================================

    @Nested
    @DisplayName("Kafka: partition, ordering, key")
    class Partitioning {

        @Test
        @DisplayName("CUNG KEY -> CUNG partition -> DAM BAO thu tu")
        void sameKeyGoesToSamePartitionInOrder() {
            var topic = new Topic("test-events", 4);

            // 1 session gui 5 event. Key = sessionId.
            for (int i = 1; i <= 5; i++) {
                topic.publish(new Message("session-42", "event-" + i, "e" + i));
            }
            // Xen ke session khac de chung minh chung khong lam lan thu tu.
            for (int i = 1; i <= 3; i++) {
                topic.publish(new Message("session-99", "other-" + i, "o" + i));
            }

            int partition = topic.partitionFor("session-42");
            List<String> values = topic.partition(partition).stream()
                    .map(r -> r.message().value())
                    .filter(v -> v.startsWith("event-"))
                    .toList();

            assertEquals(List.of("event-1", "event-2", "event-3", "event-4", "event-5"), values);
        }

        @Test
        @DisplayName("hashCode AM van ra partition hop le (Math.floorMod, khong dung %)")
        void negativeHashCodeIsHandled() {
            var topic = new Topic("t", 3);
            // Tim mot key co hashCode am - bug '%' se lam IndexOutOfBounds.
            String negativeKey = null;
            for (int i = 0; i < 10_000 && negativeKey == null; i++) {
                String candidate = "key-" + i;
                if (candidate.hashCode() < 0) {
                    negativeKey = candidate;
                }
            }
            assertTrue(negativeKey != null, "phai tim duoc key co hashCode am");

            int partition = topic.partitionFor(negativeKey);
            assertTrue(partition >= 0 && partition < 3, "partition=" + partition);
            topic.publish(new Message(negativeKey, "v", "id"));
        }

        @Test
        @DisplayName("KHONG co key -> rai deu -> MAT dam bao thu tu (danh doi phai biet)")
        void withoutKeyOrderingIsLost() {
            var topic = new Topic("t", 4);
            for (int i = 0; i < 8; i++) {
                topic.publish(new Message(null, "v" + i, "id" + i));
            }

            // 8 message rai deu 4 partition -> khong con dam bao thu tu toan cuc.
            var sizes = new ArrayList<Integer>();
            for (int p = 0; p < 4; p++) {
                sizes.add(topic.partition(p).size());
            }
            assertEquals(List.of(2, 2, 2, 2), sizes);
        }

        @Test
        @DisplayName("key qua it gia tri -> HOT PARTITION (mot tenant lam nghen ca topic)")
        void lowCardinalityKeyCausesHotPartition() {
            var topic = new Topic("t", 4);

            // Key chi la tenantId: mot tenant lon dam het vao 1 partition.
            for (int i = 0; i < 1000; i++) {
                topic.publish(new Message("tenant-big", "e" + i, "id" + i));
            }
            for (int i = 0; i < 10; i++) {
                topic.publish(new Message("tenant-small", "e" + i, "s" + i));
            }

            int hot = topic.partitionFor("tenant-big");
            assertEquals(1000, topic.partition(hot).size(), "1 partition om het");

            // Sua: key = (tenantId, sessionId) -> phan tan deu MA VAN giu thu tu trong session.
            // Day dung la thiet ke trong bai design TrueTest.
            var better = new Topic("t2", 4);
            for (int i = 0; i < 1000; i++) {
                better.publish(new Message("tenant-big:session-" + (i % 100), "e" + i, "id" + i));
            }
            var spread = new ArrayList<Integer>();
            for (int p = 0; p < 4; p++) {
                spread.add(better.partition(p).size());
            }
            assertTrue(
                    spread.stream().allMatch(size -> size > 100),
                    "phai rai deu, thuc te: " + spread);
        }
    }

    // ==================================================================
    // CONSUMER GROUP, REBALANCE, AT-LEAST-ONCE
    // ==================================================================

    @Nested
    @DisplayName("Consumer group / rebalance / at-least-once")
    class ConsumerGroups {

        private static Topic topicWith(int partitions, int messages) {
            var topic = new Topic("events", partitions);
            for (int i = 0; i < messages; i++) {
                topic.publish(new Message("k" + i, "v" + i, "e" + i));
            }
            return topic;
        }

        @Test
        @DisplayName("mot partition chi thuoc MOT consumer; consumer thua ngoi khong")
        void partitionsAreExclusivelyAssigned() {
            var group = new ConsumerGroup(topicWith(2, 0));
            group.join("c1");
            group.join("c2");
            group.join("c3"); // thua: chi co 2 partition

            var all = new ArrayList<Integer>();
            all.addAll(group.partitionsOf("c1"));
            all.addAll(group.partitionsOf("c2"));
            all.addAll(group.partitionsOf("c3"));

            assertEquals(2, all.size(), "khong partition nao bi giao 2 lan");
            assertEquals(2, new HashSet<>(all).size());
            assertTrue(group.partitionsOf("c3").isEmpty(), "consumer thua khong co viec");
        }

        @Test
        @DisplayName("AT-LEAST-ONCE: xu ly xong ma CHUA COMMIT roi chet -> message duoc giao LAI")
        void uncommittedWorkIsRedelivered() {
            var topic = topicWith(1, 5);
            var group = new ConsumerGroup(topic);
            group.join("c1");

            var handler = new CountingHandler();
            int processed = group.poll("c1", handler, false); // CHET truoc khi commit
            assertEquals(5, processed);
            assertEquals(0, group.committedOffset(0), "chua commit gi");

            // Consumer khac tiep quan -> doc lai TU DAU.
            group.poll("c1", handler, true);

            // Moi message da duoc xu ly HAI lan. Day chinh la at-least-once.
            assertEquals(2, handler.timesProcessed("e0"));
            assertEquals(2, handler.timesProcessed("e4"));

            // => day la LY DO BAT BUOC phai co idempotent consumer.
            // Xem IdempotencyStore o module 08 va test ben duoi.
        }

        @Test
        @DisplayName("IDEMPOTENT CONSUMER: message giao lai 2 lan -> side effect chi 1 lan")
        void idempotencyMakesRedeliveryHarmless() {
            var topic = topicWith(1, 5);
            var group = new ConsumerGroup(topic);
            group.join("c1");

            // Bo nho cac eventId da xu ly (trong that: INSERT ... ON CONFLICT DO NOTHING).
            var seen = new HashSet<String>();
            var sideEffects = new AtomicInteger();
            KafkaSimulator.Handler idempotent = record -> {
                if (seen.add(record.message().eventId())) {
                    sideEffects.incrementAndGet();
                }
            };

            group.poll("c1", idempotent, false); // chet truoc commit
            group.poll("c1", idempotent, true);  // doc lai

            assertEquals(5, sideEffects.get(), "moi event chi gay side effect MOT lan");
        }

        @Test
        @DisplayName("REBALANCE khi consumer roi nhom: partition duoc chia lai, doc tiep tu offset DA COMMIT")
        void rebalanceReassignsPartitionsAndResumesFromCommittedOffset() {
            var topic = topicWith(4, 0);
            for (int i = 0; i < 8; i++) {
                topic.publish(new Message("k" + i, "v" + i, "e" + i));
            }
            var group = new ConsumerGroup(topic);
            group.join("c1");
            group.join("c2");

            int before = group.rebalanceCount();
            var handler = new CountingHandler();
            group.poll("c1", handler, true); // c1 xu ly xong phan cua no va commit

            var c1Partitions = new ArrayList<>(group.partitionsOf("c1"));
            assertFalse(c1Partitions.isEmpty());

            group.leave("c1"); // c1 chet
            assertTrue(group.rebalanceCount() > before, "phai co rebalance");

            // c2 gio giu TAT CA partition.
            assertEquals(4, group.partitionsOf("c2").size());

            // c2 doc tiep: phan c1 da commit thi KHONG bi xu ly lai.
            var handler2 = new CountingHandler();
            group.poll("c2", handler2, true);
            for (int p : c1Partitions) {
                assertEquals(
                        topic.partition(p).size(),
                        group.committedOffset(p),
                        "partition %d da duoc commit het truoc rebalance".formatted(p));
            }
            assertNotEquals(0, handler2.order().size(), "c2 van con viec cua chinh no de lam");
        }
    }

    // ==================================================================
    // DEAD LETTER QUEUE
    // ==================================================================

    @Nested
    @DisplayName("Dead letter queue - chan poison message lam ket ca partition")
    class DeadLetterQueue {

        @Test
        @DisplayName("poison message sau N lan that bai -> sang DLQ, luong KHONG bi ket")
        void poisonMessageGoesToDlqInsteadOfBlockingThePartition() {
            var topic = new Topic("t", 1);
            topic.publish(new Message("k1", "good-1", "e1"));
            topic.publish(new Message("k2", "POISON", "e2"));
            topic.publish(new Message("k3", "good-2", "e3"));

            var consumer = new DeadLetterConsumer(3);
            var processedValues = new ArrayList<String>();

            for (var record : topic.partition(0)) {
                boolean canCommit = consumer.consume(record, r -> {
                    if ("POISON".equals(r.message().value())) {
                        throw new TransientException("cannot parse");
                    }
                    processedValues.add(r.message().value());
                });
                assertTrue(canCommit, "luon commit duoc -> khong bao gio ket");
            }

            // Message sau poison VAN duoc xu ly - do la diem quan trong nhat.
            assertEquals(List.of("good-1", "good-2"), processedValues);
            assertEquals(1, consumer.deadLettered().size());
            assertEquals("POISON", consumer.deadLettered().getFirst().message().value());
            assertEquals(3, consumer.attemptsFor("e2"), "da thu du 3 lan truoc khi bo");
        }

        @Test
        @DisplayName("loi VINH VIEN -> sang DLQ NGAY, khong ton 3 lan thu")
        void permanentErrorSkipsRetries() {
            var topic = new Topic("t", 1);
            topic.publish(new Message("k", "bad-schema", "e1"));

            var consumer = new DeadLetterConsumer(5);
            consumer.consume(topic.partition(0).getFirst(), r -> {
                throw new NonRetryableException("schema version 99 unsupported");
            });

            assertEquals(1, consumer.deadLettered().size());
            assertEquals(1, consumer.attemptsFor("e1"), "khong retry loi vinh vien");
        }

        @Test
        @DisplayName("KHONG co DLQ: consumer ket vinh vien tren poison message (mo phong)")
        void withoutDlqTheConsumerLoopsForever() {
            var topic = new Topic("t", 1);
            topic.publish(new Message("k", "POISON", "e1"));
            topic.publish(new Message("k", "never-reached", "e2"));

            var group = new ConsumerGroup(topic);
            group.join("c1");

            // Retry mai ma khong commit -> offset khong tien.
            for (int round = 0; round < 5; round++) {
                assertThrows(
                        TransientException.class,
                        () -> group.poll("c1", record -> {
                            if ("POISON".equals(record.message().value())) {
                                throw new TransientException("cannot parse");
                            }
                        }, true));
            }

            assertEquals(0, group.committedOffset(0), "offset KHONG BAO GIO tien");
            // Message "never-reached" khong bao gio duoc xu ly. Su co nay IM LANG:
            // khong crash, chi la consumer lag tang dan cho den khi co nguoi de y.
        }
    }
}
