package com.prep.perf.code;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * P11 - Mot khoa toan cuc tren duong nong.
 *
 * <p>Bo dem metric/rate-limit/cache viet bang {@code synchronized} + HashMap: dung, nhung moi request
 * cua moi thread xep hang qua MOT cua. Them core khong tang throughput - doi khi con giam (cache line
 * cua lock nhay qua lai giua cac core).
 */
public final class P11Contention {

    private P11Contention() {}

    public interface Counter {
        void inc(String key);

        long get(String key);
    }

    /** XAU: mot monitor cho ca map. */
    public static final class SyncCounter implements Counter {
        private final Map<String, Long> m = new HashMap<>();

        @Override
        public synchronized void inc(String key) {
            m.merge(key, 1L, Long::sum);
        }

        @Override
        public synchronized long get(String key) {
            return m.getOrDefault(key, 0L);
        }
    }

    /** SUA: CHM khoa theo bin + LongAdder chia nho bo dem theo thread khi tranh chap. */
    public static final class AdderCounter implements Counter {
        private final ConcurrentHashMap<String, LongAdder> m = new ConcurrentHashMap<>();

        @Override
        public void inc(String key) {
            LongAdder a = m.get(key);           // duong nhanh khong khoa khi key da co
            if (a == null) {
                a = m.computeIfAbsent(key, k -> new LongAdder());
            }
            a.increment();
        }

        @Override
        public long get(String key) {
            LongAdder a = m.get(key);
            return a == null ? 0 : a.sum();
        }
    }
}
