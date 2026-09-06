package com.prep.sysdesign;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SYSTEM DESIGN — chia test cho worker, va chia CONG BANG giua cac tenant.
 *
 * <p>Day la trai tim cua bai design "Distributed Test Execution Platform". Hai bai toan doc lap:
 *
 * <ol>
 *   <li><b>Bin-packing:</b> chia N test vao K worker sao cho worker cham nhat xong som nhat. Day
 *       chinh la ho bai toan cua LeetCode #253 (Meeting Rooms II) va bai toan makespan.
 *   <li><b>Fairness:</b> tenant free khong duoc lam nghen tenant enterprise, va nguoc lai tenant
 *       enterprise khong duoc chiem 100% khien tenant nho cho mai mai (starvation).
 * </ol>
 */
public final class Scheduling {

    private Scheduling() {}

    /**
     * Test can chay, kem THOI LUONG LICH SU (p50 cua cac lan chay truoc).
     *
     * <p>Diem quan trong ve thiet ke: khong co truong nay thi khong the chia viec tot. Do la ly do
     * he thong phai luu lai duration cua moi lan chay — mot yeu cau xuat phat tu SCHEDULER, khong
     * phai tu bao cao.
     */
    public record TestCase(String id, String tenantId, Duration historicalDuration) {
        public TestCase {
            if (historicalDuration == null || historicalDuration.isNegative()) {
                throw new IllegalArgumentException("historicalDuration must be >= 0");
            }
        }
    }

    public record Shard(int index, List<TestCase> tests, Duration estimatedDuration) {
        public Shard {
            tests = List.copyOf(tests);
        }
    }

    /**
     * LPT (Longest Processing Time first) — greedy: sap xep giam dan roi lan luot nem vao shard
     * dang nhe nhat.
     *
     * <p><b>Vi sao khong chia deu theo SO LUONG test?</b> Vi 100 test nhanh xong truoc 3 test cham.
     * Chia theo so luong lam mot worker xong trong 10 giay, worker khac chay 20 phut — tong thoi
     * gian run bang worker cham nhat, nen ban tra tien cho K worker ma chi duoc toc do cua 1.
     *
     * <p><b>Chat luong:</b> LPT dam bao makespan <= (4/3 - 1/(3K)) x toi uu. Bai toan toi uu la
     * NP-hard, nen 4/3 la rat tot cho mot vong lap 5 dong. Neu interviewer hoi "co toi uu khong",
     * cau tra loi dung la: "khong, va khong can — day la xap xi 4/3 cua NP-hard, doi lai O(n log n)".
     *
     * <p><b>Gioi han da biet:</b> duration lich su co the sai (test moi khong co lich su, hoac test
     * vua bi doi). Nen phai co timeout per-shard va co che re-balance, chu khong tin tuyet doi.
     */
    public static List<Shard> packByLongestFirst(List<TestCase> tests, int shardCount) {
        if (shardCount < 1) {
            throw new IllegalArgumentException("shardCount must be >= 1, got " + shardCount);
        }
        var buckets = new ArrayList<List<TestCase>>(shardCount);
        var loads = new long[shardCount];
        for (int i = 0; i < shardCount; i++) {
            buckets.add(new ArrayList<>());
        }

        // Sap xep giam dan theo thoi luong. Test dai nhat duoc dat truoc — do la toan bo bi mat
        // cua LPT: dat cai kho truoc, cai de dung de "lap khe".
        var sorted = new ArrayList<>(tests);
        sorted.sort(Comparator.comparing(TestCase::historicalDuration).reversed()
                .thenComparing(TestCase::id)); // tie-break on dinh -> ket qua deterministic

        for (TestCase test : sorted) {
            int lightest = 0;
            for (int i = 1; i < shardCount; i++) {
                if (loads[i] < loads[lightest]) {
                    lightest = i;
                }
            }
            buckets.get(lightest).add(test);
            loads[lightest] += test.historicalDuration().toMillis();
        }

        var shards = new ArrayList<Shard>(shardCount);
        for (int i = 0; i < shardCount; i++) {
            shards.add(new Shard(i, buckets.get(i), Duration.ofMillis(loads[i])));
        }
        return List.copyOf(shards);
    }

    /** Makespan = thoi gian cua shard cham nhat = thoi gian ca run. */
    public static Duration makespan(List<Shard> shards) {
        return shards.stream()
                .map(Shard::estimatedDuration)
                .max(Duration::compareTo)
                .orElse(Duration.ZERO);
    }

    /** Chia deu theo SO LUONG — cach nai ngay, de o day de SO SANH bang test. */
    public static List<Shard> packByCountNaive(List<TestCase> tests, int shardCount) {
        var buckets = new ArrayList<List<TestCase>>(shardCount);
        for (int i = 0; i < shardCount; i++) {
            buckets.add(new ArrayList<>());
        }
        for (int i = 0; i < tests.size(); i++) {
            buckets.get(i % shardCount).add(tests.get(i));
        }
        var shards = new ArrayList<Shard>(shardCount);
        for (int i = 0; i < shardCount; i++) {
            long total = buckets.get(i).stream()
                    .mapToLong(t -> t.historicalDuration().toMillis())
                    .sum();
            shards.add(new Shard(i, buckets.get(i), Duration.ofMillis(total)));
        }
        return List.copyOf(shards);
    }

    // ==================================================================
    // WEIGHTED FAIR QUEUEING — chong noisy neighbour trong SaaS multi-tenant
    // ==================================================================

    /**
     * Queue cong bang co trong so theo tenant.
     *
     * <p><b>Van de:</b> FIFO thuan la sai o SaaS. Mot tenant submit 100.000 test luc 9 gio sang se
     * chiem het slot; moi tenant khac cho hang gio du ho chi submit 5 test. Do la "noisy neighbour",
     * va la loai su co lam mat khach hang nhanh nhat.
     *
     * <p><b>Giai phap:</b> mot queue rieng cho moi tenant, va rut theo VONG (round-robin) co trong
     * so. Tenant enterprise weight 5, free weight 1 -> enterprise duoc 5 slot moi vong, nhung
     * tenant free VAN LUON duoc 1 -> khong bao gio starvation.
     *
     * <p><b>Danh doi phai noi ra:</b> throughput tong THAP hon FIFO thuan (vi mat cache locality,
     * va vi phai chuyen qua lai giua cac tenant). Ta doi throughput lay tinh du doan duoc cho tung
     * khach hang. Voi SaaS ban theo SLA thi doi nay luon dung.
     */
    public static final class WeightedFairQueue {

        private final Map<String, Deque<TestCase>> perTenant = new LinkedHashMap<>();
        private final Map<String, Integer> weights = new LinkedHashMap<>();

        /** @param weight so slot tenant nay duoc nhan trong mot vong. Phai >= 1 de tranh starvation. */
        public void register(String tenantId, int weight) {
            if (weight < 1) {
                throw new IllegalArgumentException(
                        "weight must be >= 1 (weight 0 = starvation), got " + weight);
            }
            weights.put(tenantId, weight);
            perTenant.computeIfAbsent(tenantId, k -> new ArrayDeque<>());
        }

        public void submit(TestCase test) {
            Deque<TestCase> queue = perTenant.get(test.tenantId());
            if (queue == null) {
                throw new IllegalStateException("unknown tenant: " + test.tenantId());
            }
            queue.addLast(test);
        }

        /**
         * Rut toi da {@code limit} test theo vong co trong so.
         *
         * <p>Moi vong: di qua tung tenant, moi tenant duoc rut toi da {@code weight} test. Lap lai
         * cho den khi du {@code limit} hoac het viec.
         */
        public List<TestCase> drain(int limit) {
            var out = new ArrayList<TestCase>(Math.min(limit, 1024));
            boolean progress = true;

            while (out.size() < limit && progress) {
                progress = false;
                for (var entry : perTenant.entrySet()) {
                    Deque<TestCase> queue = entry.getValue();
                    int quota = weights.get(entry.getKey());
                    for (int i = 0; i < quota && out.size() < limit && !queue.isEmpty(); i++) {
                        out.add(queue.pollFirst());
                        progress = true;
                    }
                }
            }
            return List.copyOf(out);
        }

        public int pending(String tenantId) {
            Deque<TestCase> queue = perTenant.get(tenantId);
            return queue == null ? 0 : queue.size();
        }

        public int totalPending() {
            return perTenant.values().stream().mapToInt(Deque::size).sum();
        }
    }
}
