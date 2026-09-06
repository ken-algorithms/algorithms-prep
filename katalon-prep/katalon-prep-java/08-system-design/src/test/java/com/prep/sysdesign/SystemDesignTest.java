package com.prep.sysdesign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.prep.sysdesign.Resilience.CircuitBreaker;
import com.prep.sysdesign.Resilience.CircuitOpenException;
import com.prep.sysdesign.Resilience.IdempotencyStore;
import com.prep.sysdesign.Resilience.IdempotentConsumer;
import com.prep.sysdesign.Resilience.PerTenantRateLimiter;
import com.prep.sysdesign.Resilience.State;
import com.prep.sysdesign.Resilience.TokenBucket;
import com.prep.sysdesign.Scheduling.TestCase;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * System design nhung CHAY DUOC.
 *
 * <p>Muc dich: khi interviewer hoi "ban se rate-limit the nao / chia test cho worker the nao", ban
 * khong tra loi bang danh tu ("token bucket", "bin packing"). Ban tra loi bang co che, bang trade-off,
 * va bang so lieu — vi ban da tu viet va tu do.
 */
class SystemDesignTest {

    /** Dong ho gia — cho phep test logic thoi gian ma KHONG can Thread.sleep. */
    private static final class FakeClock {
        private final AtomicLong now = new AtomicLong(1_000_000L);

        long millis() {
            return now.get();
        }

        void advance(Duration d) {
            now.addAndGet(d.toMillis());
        }
    }

    // ==================================================================
    // BIN PACKING
    // ==================================================================

    @Nested
    @DisplayName("Bin-packing: chia test cho worker (bai toan makespan)")
    class BinPacking {

        /**
         * 4 test e2e nang xen giua cac test nhanh — va CO Y dat o vi tri 0, 4, 8, 12.
         *
         * <p>Vi sao thu tu nay khong phai bay dat: file test thuong duoc liet ke theo thu muc, nen
         * cac test e2e nang hay nam canh nhau hoac cach nhau deu. Voi 4 shard, chia round-robin se
         * don TAT CA test nang vao shard 0 — va do la diem yeu that su cua cach chia theo so luong:
         * ket qua phu thuoc vao THU TU dau vao, tuc la khong du doan duoc.
         */
        private static List<TestCase> skewedSuite() {
            var tests = new ArrayList<TestCase>();
            for (int block = 0; block < 4; block++) {
                tests.add(new TestCase("e2e-" + block, "t1", Duration.ofSeconds(300)));
                for (int i = 0; i < 3; i++) {
                    tests.add(new TestCase("unit-" + block + "-" + i, "t1", Duration.ofSeconds(5)));
                }
            }
            return List.copyOf(tests); // 16 test: 4 x 300s + 12 x 5s
        }

        @Test
        @DisplayName("chia deu theo SO LUONG -> makespan te vi test cham don vao mot shard")
        void naiveCountBasedPackingIsBad() {
            var naive = Scheduling.packByCountNaive(skewedSuite(), 4);
            var lpt = Scheduling.packByLongestFirst(skewedSuite(), 4);

            Duration naiveMakespan = Scheduling.makespan(naive);
            Duration lptMakespan = Scheduling.makespan(lpt);

            System.out.printf(
                    "=== makespan: chia-theo-so-luong=%ds  LPT=%ds  (nhanh hon %.2fx)%n",
                    naiveMakespan.toSeconds(),
                    lptMakespan.toSeconds(),
                    (double) naiveMakespan.toSeconds() / lptMakespan.toSeconds());

            // Ca hai deu chia dung so test, nhung makespan khac han — va makespan chinh la thoi gian
            // ma nguoi dung phai cho.
            // Chia theo so luong: shard 0 nhan het 4 test nang = 1200s.
            // LPT: moi shard mot test nang = ~315s. Chenh gan 4 lan.
            assertTrue(
                    lptMakespan.multipliedBy(2).compareTo(naiveMakespan) < 0,
                    "LPT phai tot hon it nhat 2x: naive=%s lpt=%s".formatted(naiveMakespan, lptMakespan));
        }

        @Test
        @DisplayName("LPT: khong mat test nao, khong nhan doi test nao")
        void packingPreservesEveryTest() {
            var suite = skewedSuite();
            var shards = Scheduling.packByLongestFirst(suite, 4);

            var packed = shards.stream().flatMap(s -> s.tests().stream()).map(TestCase::id).toList();
            assertEquals(suite.size(), packed.size(), "khong duoc mat hay nhan doi test");
            assertEquals(suite.size(), packed.stream().distinct().count());
        }

        @Test
        @DisplayName("LPT rai deu 4 test nang ra 4 shard, moi shard dung 1 cai")
        void lptSpreadsHeavyTestsAcrossShards() {
            var shards = Scheduling.packByLongestFirst(skewedSuite(), 4);

            for (var shard : shards) {
                long heavy = shard.tests().stream()
                        .filter(t -> t.historicalDuration().toSeconds() >= 300)
                        .count();
                assertEquals(1, heavy, "shard " + shard.index() + " phai co dung 1 test nang");
            }

            // Va cac shard can bang nhau — do la muc tieu that cua bin-packing.
            long min = shards.stream().mapToLong(sh -> sh.estimatedDuration().toSeconds()).min().orElseThrow();
            long max = shards.stream().mapToLong(sh -> sh.estimatedDuration().toSeconds()).max().orElseThrow();
            assertTrue(max - min <= 10, "lech giua shard nang nhat va nhe nhat: %ds".formatted(max - min));
        }

        @Test
        @DisplayName("deterministic: cung input -> cung ket qua (quan trong cho debug va cache)")
        void packingIsDeterministic() {
            var a = Scheduling.packByLongestFirst(skewedSuite(), 3);
            var b = Scheduling.packByLongestFirst(skewedSuite(), 3);
            assertEquals(a.toString(), b.toString());
        }

        @Test
        @DisplayName("shardCount = 1 -> tat ca vao mot shard; shardCount < 1 -> loi cau hinh")
        void edgeCases() {
            assertEquals(16, Scheduling.packByLongestFirst(skewedSuite(), 1).getFirst().tests().size());
            assertThrows(
                    IllegalArgumentException.class, () -> Scheduling.packByLongestFirst(skewedSuite(), 0));
            // Nhieu shard hon test -> co shard rong, khong duoc nem.
            var many = Scheduling.packByLongestFirst(List.of(new TestCase("t", "t1", Duration.ofSeconds(1))), 5);
            assertEquals(5, many.size());
            assertEquals(4, many.stream().filter(s -> s.tests().isEmpty()).count());
        }
    }

    // ==================================================================
    // WEIGHTED FAIR QUEUEING
    // ==================================================================

    @Nested
    @DisplayName("Weighted fair queueing: chong noisy neighbour")
    class Fairness {

        @Test
        @DisplayName("tenant lon submit 1000 test KHONG lam tenant nho phai cho")
        void smallTenantIsNotStarvedByALargeOne() {
            var queue = new Scheduling.WeightedFairQueue();
            queue.register("enterprise", 5);
            queue.register("free", 1);

            for (int i = 0; i < 1000; i++) {
                queue.submit(new TestCase("big-" + i, "enterprise", Duration.ofSeconds(1)));
            }
            // Tenant nho submit SAU, va chi 3 test.
            for (int i = 0; i < 3; i++) {
                queue.submit(new TestCase("small-" + i, "free", Duration.ofSeconds(1)));
            }

            var firstBatch = queue.drain(12);

            long freeInFirstBatch = firstBatch.stream()
                    .filter(t -> "free".equals(t.tenantId()))
                    .count();

            System.out.println("=== 12 slot dau: " + firstBatch.stream().map(TestCase::id).toList());

            // Voi FIFO thuan, ca 12 slot dau se la cua enterprise va tenant free cho sau 1000 test.
            // Voi weighted fair queue: moi VONG cap 5 slot cho enterprise + 1 cho free.
            // 12 slot = 2 vong = 10 enterprise + 2 free -> free duoc phuc vu ngay tu vong DAU TIEN.
            assertEquals(2, freeInFirstBatch, "tenant nho phai duoc phuc vu ngay, khong bi starvation");
            assertEquals(10, firstBatch.size() - freeInFirstBatch, "ty le 5:1 dung theo weight");

            // Ca 3 test cua tenant free xong trong 18 slot dau, chu khong phai cho het 1000 test.
            var moreSlots = queue.drain(6);
            assertEquals(1, moreSlots.stream().filter(t -> "free".equals(t.tenantId())).count());
            assertEquals(0, queue.pending("free"), "tenant nho da duoc phuc vu xong tu rat som");
        }

        @Test
        @DisplayName("het viec cua tenant nay thi tenant khac dung het slot (khong bo trong)")
        void unusedQuotaIsNotWasted() {
            var queue = new Scheduling.WeightedFairQueue();
            queue.register("a", 2);
            queue.register("b", 2);

            for (int i = 0; i < 10; i++) {
                queue.submit(new TestCase("a-" + i, "a", Duration.ofSeconds(1)));
            }
            queue.submit(new TestCase("b-0", "b", Duration.ofSeconds(1)));

            var drained = queue.drain(6);

            // b chi co 1 test -> 5 slot con lai thuoc ve a. Fairness KHONG duoc bien thanh bo trong
            // tai nguyen (work-conserving).
            assertEquals(6, drained.size());
            assertEquals(1, drained.stream().filter(t -> "b".equals(t.tenantId())).count());
            assertEquals(5, drained.stream().filter(t -> "a".equals(t.tenantId())).count());
        }

        @Test
        @DisplayName("weight 0 = starvation -> chan ngay tai luc dang ky")
        void zeroWeightIsRejected() {
            var queue = new Scheduling.WeightedFairQueue();
            var ex = assertThrows(IllegalArgumentException.class, () -> queue.register("x", 0));
            assertTrue(ex.getMessage().contains("starvation"), ex.getMessage());
        }

        @Test
        @DisplayName("drain khong lam mat viec: phan chua rut van con trong queue")
        void drainLeavesRemainderQueued() {
            var queue = new Scheduling.WeightedFairQueue();
            queue.register("a", 1);
            for (int i = 0; i < 10; i++) {
                queue.submit(new TestCase("a-" + i, "a", Duration.ofSeconds(1)));
            }
            assertEquals(10, queue.totalPending());
            queue.drain(4);
            assertEquals(6, queue.pending("a"));
        }
    }

    // ==================================================================
    // TOKEN BUCKET
    // ==================================================================

    @Nested
    @DisplayName("Token bucket rate limiter")
    class RateLimiting {

        @Test
        @DisplayName("cho phep burst bang capacity, roi chan")
        void allowsBurstThenRejects() {
            var clock = new FakeClock();
            var bucket = new TokenBucket(10, 1, clock::millis);

            for (int i = 0; i < 10; i++) {
                assertTrue(bucket.tryAcquire(), "token thu " + i + " phai duoc phep (burst)");
            }
            assertFalse(bucket.tryAcquire(), "het token -> phai bi chan");
        }

        @Test
        @DisplayName("nap lai theo thoi gian, KHONG can background thread")
        void refillsLazilyOverTime() {
            var clock = new FakeClock();
            var bucket = new TokenBucket(10, 2, clock::millis); // 2 token/giay

            for (int i = 0; i < 10; i++) {
                bucket.tryAcquire();
            }
            assertFalse(bucket.tryAcquire());

            clock.advance(Duration.ofSeconds(3)); // 3s x 2 = 6 token
            assertEquals(6, bucket.availableTokens());
            for (int i = 0; i < 6; i++) {
                assertTrue(bucket.tryAcquire());
            }
            assertFalse(bucket.tryAcquire());
        }

        @Test
        @DisplayName("khong nap qua capacity du de rat lau (chong tich luy burst vo han)")
        void refillIsCappedAtCapacity() {
            var clock = new FakeClock();
            var bucket = new TokenBucket(10, 100, clock::millis);
            bucket.tryAcquire(10);

            clock.advance(Duration.ofHours(1));
            assertEquals(10, bucket.availableTokens(), "khong duoc vuot capacity");
        }

        @Test
        @DisplayName("dong ho lui (NTP dieu chinh) KHONG lam vo bo dem")
        void clockGoingBackwardsIsSafe() {
            var now = new AtomicLong(1_000_000L);
            var bucket = new TokenBucket(10, 1, now::get);
            bucket.tryAcquire(5);
            long before = bucket.availableTokens();

            now.addAndGet(-60_000); // dong ho lui 1 phut
            assertEquals(before, bucket.availableTokens(), "khong duoc tang hay giam token");
        }

        @Test
        @DisplayName("quota rieng theo tenant: tenant A het quota KHONG anh huong tenant B")
        void perTenantQuotasAreIndependent() {
            var clock = new FakeClock();
            var limiter = new PerTenantRateLimiter(3, 1, clock::millis);

            for (int i = 0; i < 3; i++) {
                assertTrue(limiter.tryAcquire("tenant-a"));
            }
            assertFalse(limiter.tryAcquire("tenant-a"), "A het quota");

            // B khong bi anh huong — day chinh la yeu cau isolation trong SaaS multi-tenant.
            assertTrue(limiter.tryAcquire("tenant-b"));
            assertEquals(2, limiter.trackedTenants());
        }

        @Test
        @DisplayName("cau hinh vo ly bi chan tai constructor")
        void invalidConfigRejected() {
            var clock = new FakeClock();
            assertThrows(IllegalArgumentException.class, () -> new TokenBucket(0, 1, clock::millis));
            assertThrows(IllegalArgumentException.class, () -> new TokenBucket(10, 0, clock::millis));
        }
    }

    // ==================================================================
    // CIRCUIT BREAKER
    // ==================================================================

    @Nested
    @DisplayName("Circuit breaker: CLOSED -> OPEN -> HALF_OPEN -> CLOSED")
    class CircuitBreaking {

        private static CircuitBreaker breaker(FakeClock clock) {
            return new CircuitBreaker(3, 2, Duration.ofSeconds(30), clock::millis);
        }

        @Test
        @DisplayName("du nguong loi -> OPEN, va request sau bi chan TRUOC khi cham downstream")
        void opensAfterThresholdAndFailsFast() {
            var clock = new FakeClock();
            var cb = breaker(clock);
            var downstreamCalls = new AtomicLong();

            for (int i = 0; i < 3; i++) {
                assertThrows(IllegalStateException.class, () -> cb.call(() -> {
                    downstreamCalls.incrementAndGet();
                    throw new IllegalStateException("downstream down");
                }));
            }
            assertEquals(State.OPEN, cb.state());
            assertEquals(3, downstreamCalls.get());

            // Request tiep theo bi chan — downstream KHONG bi goi. Day la gia tri that cua breaker:
            // bao ve downstream dang qua tai, va giai phong thread cua chinh minh.
            assertThrows(CircuitOpenException.class, () -> cb.call(() -> {
                downstreamCalls.incrementAndGet();
                return "should not run";
            }));
            assertEquals(3, downstreamCalls.get(), "downstream KHONG duoc goi khi circuit OPEN");
            assertEquals(1, cb.rejectedCount());
        }

        @Test
        @DisplayName("thanh cong xen giua reset bo dem -> khong OPEN oan")
        void intermittentFailuresDoNotOpenTheCircuit() {
            var clock = new FakeClock();
            var cb = breaker(clock);

            for (int round = 0; round < 5; round++) {
                assertThrows(IllegalStateException.class, () -> cb.call(() -> {
                    throw new IllegalStateException("blip");
                }));
                assertThrows(IllegalStateException.class, () -> cb.call(() -> {
                    throw new IllegalStateException("blip");
                }));
                cb.call(() -> "ok"); // thanh cong -> reset
            }
            // 10 loi tong cong nhung khong bao gio 3 loi LIEN TIEP -> van CLOSED.
            assertEquals(State.CLOSED, cb.state());
        }

        @Test
        @DisplayName("het thoi gian cho -> HALF_OPEN; du thanh cong -> CLOSED")
        void recoversThroughHalfOpen() {
            var clock = new FakeClock();
            var cb = breaker(clock);

            for (int i = 0; i < 3; i++) {
                assertThrows(IllegalStateException.class, () -> cb.call(() -> {
                    throw new IllegalStateException("down");
                }));
            }
            assertEquals(State.OPEN, cb.state());

            clock.advance(Duration.ofSeconds(30));
            assertEquals(State.HALF_OPEN, cb.state(), "het cool-down -> tham do");

            cb.call(() -> "ok");
            assertEquals(State.HALF_OPEN, cb.state(), "can 2 lan thanh cong lien tiep");
            cb.call(() -> "ok");
            assertEquals(State.CLOSED, cb.state(), "da hoi phuc");
        }

        @Test
        @DisplayName("HALF_OPEN gap loi -> OPEN lai NGAY (mot loi la du)")
        void singleFailureInHalfOpenReopens() {
            var clock = new FakeClock();
            var cb = breaker(clock);

            for (int i = 0; i < 3; i++) {
                assertThrows(IllegalStateException.class, () -> cb.call(() -> {
                    throw new IllegalStateException("down");
                }));
            }
            clock.advance(Duration.ofSeconds(30));
            assertEquals(State.HALF_OPEN, cb.state());

            assertThrows(IllegalStateException.class, () -> cb.call(() -> {
                throw new IllegalStateException("still down");
            }));

            // Khong doi den 3 loi nua — downstream ro rang chua san sang.
            // Day la diem chan "thundering herd": khong de ca luu luong dap lai vao no.
            assertEquals(State.OPEN, cb.state());
        }

        @Test
        @DisplayName("exception goc duoc GIU NGUYEN, breaker khong boc lai")
        void originalExceptionIsPropagated() {
            var clock = new FakeClock();
            var cb = breaker(clock);
            var ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> cb.call(() -> {
                        throw new IllegalArgumentException("bad request from us");
                    }));
            assertEquals("bad request from us", ex.getMessage());
        }
    }

    // ==================================================================
    // IDEMPOTENCY
    // ==================================================================

    @Nested
    @DisplayName("Idempotency: at-least-once + idempotent consumer")
    class Idempotency {

        @Test
        @DisplayName("message den 3 lan -> side effect chi xay ra MOT lan")
        void duplicateDeliveryIsProcessedOnce() {
            var clock = new FakeClock();
            var consumer = new IdempotentConsumer(new IdempotencyStore(Duration.ofMinutes(10), clock::millis));

            consumer.consume("evt-1");
            consumer.consume("evt-1");
            consumer.consume("evt-1");
            consumer.consume("evt-2");

            assertEquals(2, consumer.processed().size());
            assertEquals(2, consumer.duplicatesSkipped());
        }

        @Test
        @DisplayName("BAY: TTL het truoc khi producer retry xong -> message bi xu ly LAN HAI")
        void ttlShorterThanRetryWindowCausesDoubleProcessing() {
            var clock = new FakeClock();
            // TTL 1 phut, nhung producer cua ta retry den 5 phut -> cau hinh SAI.
            var consumer = new IdempotentConsumer(new IdempotencyStore(Duration.ofMinutes(1), clock::millis));

            consumer.consume("evt-1");
            assertEquals(1, consumer.processed().size());

            clock.advance(Duration.ofMinutes(2)); // qua TTL
            consumer.consume("evt-1"); // retry muon cua producer

            // Da bi xu ly hai lan, va KHONG co dau hieu gi bao dong.
            assertEquals(0, consumer.duplicatesSkipped(), "khong con nho evt-1 nua");

            // BAI HOC: TTL cua idempotency store PHAI lon hon thoi gian retry toi da cua producer
            // (cong voi do lech dong ho). Neu khong, bug nay chi xuat hien DUNG LUC he thong dang
            // co su co — luc kho debug nhat.
        }

        @Test
        @DisplayName("TTL don entry cu -> bo nho khong phinh vo han")
        void expiredEntriesAreEvicted() {
            var clock = new FakeClock();
            var store = new IdempotencyStore(Duration.ofMinutes(5), clock::millis);

            for (int i = 0; i < 100; i++) {
                store.markIfFirstTime("evt-" + i);
            }
            assertEquals(100, store.size());

            clock.advance(Duration.ofMinutes(6));
            assertEquals(0, store.size(), "entry qua han phai bi don");
        }

        @Test
        @DisplayName("TTL <= 0 la cau hinh vo nghia")
        void invalidTtlRejected() {
            var clock = new FakeClock();
            assertThrows(
                    IllegalArgumentException.class, () -> new IdempotencyStore(Duration.ZERO, clock::millis));
        }
    }
}
