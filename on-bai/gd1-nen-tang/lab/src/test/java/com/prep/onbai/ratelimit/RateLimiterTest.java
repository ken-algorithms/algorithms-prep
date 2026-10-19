package com.prep.onbai.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.prep.onbai.ratelimit.WindowCounters.FixedWindow;
import com.prep.onbai.ratelimit.WindowCounters.SlidingCounter;
import com.prep.onbai.ratelimit.WindowCounters.SlidingLog;
import com.prep.onbai.testsupport.RedisServer;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPooled;

/** Bai 03 — rate limiter: dong ho gia cho ban trong bo nho, redis-server that cho ban phan tan. */
class RateLimiterTest {

    static RedisServer server;

    @AfterAll
    static void stopRedis() {
        if (server != null) {
            server.close();
        }
    }

    static RedisServer redis() {
        if (server == null) {
            server = RedisServer.startOrSkip();
        }
        return server;
    }

    @Test
    void tokenBucketAllowsABurstThenTheRefillRate() {
        AtomicLong nanos = new AtomicLong();
        TokenBucket bucket = new TokenBucket(5, 2, nanos::get); // burst 5, trung binh 2/s
        int burst = 0;
        for (int i = 0; i < 20; i++) {
            burst += bucket.tryAcquire("u1") ? 1 : 0;
        }
        nanos.addAndGet(1_000_000_000L); // 1 giay sau: nap lai 2 token
        int afterOneSecond = 0;
        for (int i = 0; i < 20; i++) {
            afterOneSecond += bucket.tryAcquire("u1") ? 1 : 0;
        }
        assertThat(burst).isEqualTo(5);
        assertThat(afterOneSecond).isEqualTo(2);
        assertThat(bucket.tryAcquire("u2")).isTrue(); // key khac, bucket rieng
    }

    @Test
    void tokenBucketIsExactUnderContention() throws Exception {
        TokenBucket bucket = new TokenBucket(100, 0.000001, () -> 0L); // dong ho dung yen: khong nap them
        AtomicInteger granted = new AtomicInteger();
        int threads = 16;
        CyclicBarrier go = new CyclicBarrier(threads);
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int t = 0; t < threads; t++) {
                pool.submit(() -> {
                    go.await();
                    for (int i = 0; i < 1_000; i++) {
                        if (bucket.tryAcquire("hot")) {
                            granted.incrementAndGet();
                        }
                    }
                    return null;
                });
            }
        }
        assertThat(granted.get()).isEqualTo(100);
    }

    @Test
    void fixedWindowLets2xThroughAtTheBoundarySlidingVariantsDoNot() {
        AtomicLong ms = new AtomicLong(59_999); // 1 ms truoc het phut
        FixedWindow fixed = new FixedWindow(100, 60_000, ms::get);
        SlidingLog log = new SlidingLog(100, 60_000, ms::get);
        SlidingCounter counter = new SlidingCounter(100, 60_000, ms::get);
        int[] got = new int[3];
        for (int i = 0; i < 100; i++) {
            got[0] += fixed.tryAcquire("k") ? 1 : 0;
            got[1] += log.tryAcquire("k") ? 1 : 0;
            got[2] += counter.tryAcquire("k") ? 1 : 0;
        }
        ms.set(60_001); // 2 ms sau, da sang phut moi
        for (int i = 0; i < 100; i++) {
            got[0] += fixed.tryAcquire("k") ? 1 : 0;
            got[1] += log.tryAcquire("k") ? 1 : 0;
            got[2] += counter.tryAcquire("k") ? 1 : 0;
        }
        System.out.printf("[LAB] limit 100/phut, 200 request trong 2 ms quanh bien phut: fixed %d, sliding log %d, sliding counter %d%n",
                got[0], got[1], got[2]);
        assertThat(got[0]).isEqualTo(200); // gap doi limit
        assertThat(got[1]).isEqualTo(100);
        // xap xi: 100 x (1 - 1/60000) = 99,998 < 100 -> lot them 1. Doi lai: 2 so nguyen moi key thay vi 100 moc thoi gian
        assertThat(got[2]).isBetween(100, 101);
    }

    @Test
    void redisGetThenSetOverAdmitsWhenTwoInstancesInterleave() {
        try (JedisPooled a = redis().client(); JedisPooled b = redis().client()) {
            a.set("naive", "99");
            RedisRateLimiters instanceA = new RedisRateLimiters(a), instanceB = new RedisRateLimiters(b);
            boolean[] bAllowed = new boolean[1];
            boolean aAllowed = instanceA.naiveGetThenSet("naive", 100, () ->            // A: GET 99 ...
                    bAllowed[0] = instanceB.naiveGetThenSet("naive", 100, () -> {}));  // ... B chen vao: GET 99, SET 100
            System.out.println("[LAB] GET roi SET, limit 100, dang o 99: A=" + aAllowed + " B=" + bAllowed[0] + " -> bo dem = " + a.get("naive"));
            assertThat(aAllowed).isTrue();
            assertThat(bAllowed[0]).isTrue();     // 101 request lot qua limit 100
            assertThat(a.get("naive")).isEqualTo("100");
        }
    }

    @Test
    void luaTokenBucketIsExactAcrossInstances() throws Exception {
        int instances = 4, threadsPerInstance = 8, tries = 50;
        AtomicInteger granted = new AtomicInteger();
        CyclicBarrier go = new CyclicBarrier(instances * threadsPerInstance);
        JedisPooled[] clients = new JedisPooled[instances];
        try (ExecutorService pool = Executors.newFixedThreadPool(instances * threadsPerInstance)) {
            for (int i = 0; i < instances; i++) {
                clients[i] = redis().client(); // moi "pod" mot connection pool rieng
                RedisRateLimiters limiter = new RedisRateLimiters(clients[i]);
                for (int t = 0; t < threadsPerInstance; t++) {
                    pool.submit(() -> {
                        go.await();
                        for (int k = 0; k < tries; k++) {
                            if (limiter.tokenBucket("api:user42", 100, 0.001).allowed()) {
                                granted.incrementAndGet();
                            }
                        }
                        return null;
                    });
                }
            }
        } finally {
            for (JedisPooled c : clients) {
                if (c != null) {
                    c.close();
                }
            }
        }
        System.out.println("[LAB] Lua token bucket, 4 instance x 8 luong x 50 lan, capacity 100: cho qua " + granted.get());
        assertThat(granted.get()).isEqualTo(100);
    }

    @Test
    void incrIsNeverWrongButStillHasTheBoundaryProblem() {
        try (JedisPooled c = redis().client()) {
            RedisRateLimiters limiter = new RedisRateLimiters(c);
            int ok = 0;
            for (int i = 0; i < 150; i++) {
                ok += limiter.fixedWindowIncr("incr:user7", 100, 60_000) ? 1 : 0;
            }
            assertThat(ok).isEqualTo(100);
            assertThat(c.pttl("incr:user7:" + (System.currentTimeMillis() / 60_000))).isPositive(); // co TTL
        }
    }
}
