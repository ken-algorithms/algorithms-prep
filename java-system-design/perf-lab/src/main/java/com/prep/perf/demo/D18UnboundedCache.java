package com.prep.perf.demo;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * P18 - Cache khong gioi han = memory leak co ten dep.
 *
 * <pre>
 * XAU   private static final Map&lt;String, Quote&gt; CACHE = new HashMap&lt;&gt;();   // khoa co userId + tham so
 * SUA   Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(5, MINUTES)  // o day: LRU LinkedHashMap
 * </pre>
 *
 * Khoa cache chua thanh phan co cardinality cao (userId, timestamp, query string) -> moi request mot
 * khoa moi -> map lon mai. Trieu chung tren production: heap dang rang cua cua tang dan, GC chay
 * day dac hon, p99 tang theo ngay, cuoi cung OOM vao gio cao diem - va restart "chua" duoc vai ngay.
 *
 * <p>Chay rieng voi heap nho de thay OOM that: {@code java -Xmx128m -cp target/benchmarks.jar
 * com.prep.perf.demo.D18UnboundedCache bad}
 */
public final class D18UnboundedCache {

    private D18UnboundedCache() {}

    /** LRU toi gian: dung de demo khong phu thuoc. Production dung Caffeine (W-TinyLFU, co stats). */
    public static <K, V> Map<K, V> lru(int maxEntries) {
        return new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    /** Mo phong request "bao gia ty gia" co khoa gom userId + phut hien tai. */
    public static void serve(Map<String, byte[]> cache, long requestNo) {
        String key = "quote:user-" + (requestNo % 200_000) + ":min-" + (requestNo / 1000);
        cache.computeIfAbsent(key, k -> new byte[1024]);   // ~1 KB moi entry
    }

    public static void main(String[] args) {
        boolean bad = args.length == 0 || args[0].equals("bad");
        Map<String, byte[]> cache = bad ? new HashMap<>() : lru(10_000);
        var rt = Runtime.getRuntime();
        System.out.printf("%s cache, -Xmx=%dMB%n", bad ? "UNBOUNDED" : "LRU(10k)", rt.maxMemory() >> 20);
        long n = 0;
        try {
            for (n = 1; n <= 1_000_000; n++) {
                serve(cache, n);
                if (n % 50_000 == 0) {
                    long gcMs = ManagementFactory.getGarbageCollectorMXBeans().stream()
                            .mapToLong(g -> Math.max(0, g.getCollectionTime())).sum();
                    System.out.printf("  requests=%,9d  entries=%,9d  heapUsed=%4dMB  gcTimeTotal=%dms%n",
                            n, cache.size(), (rt.totalMemory() - rt.freeMemory()) >> 20, gcMs);
                }
            }
            System.out.println("  done: 1,000,000 requests served");
        } catch (OutOfMemoryError oom) {
            cache = null;   // nha bo nho de in duoc
            System.out.printf("  OutOfMemoryError after %,d requests: %s%n", n, oom.getMessage());
        }
    }
}
