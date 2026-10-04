package com.prep.perf;

import com.prep.perf.demo.D12Pinning;
import com.prep.perf.demo.D13UnboundedQueue;
import com.prep.perf.demo.D15NPlusOne;
import com.prep.perf.demo.D18UnboundedCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cac hieu ung DEM duoc (so query, so entry, so request bi tu choi) - khong phu thuoc toc do may.
 */
class CountedEffectsTest {

    @Test
    void p15_nPlusOneIssuesOnePlusNQueries_batchedIssuesTwo() {
        var bad = new D15NPlusOne.FakeDb(50, 3, 0);
        var good = new D15NPlusOne.FakeDb(50, 3, 0);
        var r1 = D15NPlusOne.loadBad(bad, 20);
        var r2 = D15NPlusOne.loadGood(good, 20);
        assertEquals(r1, r2);
        assertEquals(1 + 20, bad.queries());
        assertEquals(2, good.queries());
    }

    @Test
    void p18_unboundedCacheGrowsWithRequests_lruStaysBounded() {
        Map<String, byte[]> unbounded = new HashMap<>();
        Map<String, byte[]> lru = D18UnboundedCache.lru(1_000);
        for (long n = 1; n <= 20_000; n++) {
            D18UnboundedCache.serve(unbounded, n);
            D18UnboundedCache.serve(lru, n);
        }
        assertEquals(20_000, unbounded.size());   // moi request mot khoa moi
        assertEquals(1_000, lru.size());
    }

    @Test
    void p13_boundedQueueShedsLoadInsteadOfQueueingForever() {
        // nang luc 2 x 10ms = 200 req/s, tai 400 req/s trong 1 s
        var unbounded = D13UnboundedQueue.run(null, 400, 1, 2, 10);
        var bounded = D13UnboundedQueue.run(10, 400, 1, 2, 10);
        assertEquals(0, unbounded.rejected());
        assertEquals(400, unbounded.accepted());
        assertTrue(bounded.rejected() > 0, "qua tai thi phai tu choi bot");
        assertTrue(bounded.maxQueueDepth() <= 10);
        assertTrue(unbounded.maxQueueDepth() > 10 * 5, "hang doi vo han phinh to: " + unbounded.maxQueueDepth());
    }

    /**
     * Pinning chi ton tai o JDK 21-23. Tu JDK 24 (JEP 491) test nay bi tat thay vi fail - mot test
     * "chung minh pinning" chay xanh tren JDK 25 la test noi doi.
     */
    @Test
    @EnabledForJreRange(min = JRE.JAVA_21, max = JRE.JAVA_23)
    void p12_synchronizedAroundIoPinsCarrierThreadsOnJdk21() {
        D12Pinning.run(false, 100, 1);
        var pinned = D12Pinning.run(true, 200, 20);
        var free = D12Pinning.run(false, 200, 20);
        int cores = Runtime.getRuntime().availableProcessors();
        // 200 task x 20ms / so core: pinned >= ~ (200/cores) * 20ms; free ~ 20ms + overhead
        assertTrue(pinned.wallMs() >= (200 / Math.max(cores, 1)) * 20L / 2,
                "pinned=" + pinned.wallMs() + "ms");
        assertTrue(pinned.wallMs() > free.wallMs() * 3, "pinned=" + pinned.wallMs() + " free=" + free.wallMs());
    }
}
