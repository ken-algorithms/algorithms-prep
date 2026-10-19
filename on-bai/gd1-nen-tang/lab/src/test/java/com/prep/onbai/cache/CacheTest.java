package com.prep.onbai.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.prep.onbai.testsupport.Pg;
import com.prep.onbai.testsupport.RedisServer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import redis.clients.jedis.JedisPooled;

/** Bai 08 — stampede va race xoa cache, tren Postgres that; quy tac version kiem lai tren redis-server that. */
class CacheTest {

    DataSource ds;
    ProductStore db;

    @BeforeEach
    void fresh() {
        ds = Pg.freshDatabase("b08_cache", "cache.sql");
        new JdbcTemplate(ds).update("INSERT INTO product (id, name, price) VALUES (1, 'Katalon Studio Enterprise', 100)");
        db = new ProductStore(ds);
    }

    @Test
    void hundredConcurrentMissesHitTheDatabaseOnceWithSingleFlight() throws Exception {
        db.readDelayMs(50);
        CacheAside naive = new CacheAside(db, true, 60_000, System::currentTimeMillis);
        int naiveReads = stampede(naive, 100);
        CacheAside coalesced = new CacheAside(db, true, 60_000, System::currentTimeMillis);
        coalesced.singleFlight(true);
        int before = db.reads();
        stampede(coalesced, 100);
        int coalescedReads = db.reads() - before;
        System.out.printf("[LAB] 100 luong cung miss key nong: khong gop -> %d lan doc DB, single-flight -> %d lan%n",
                naiveReads, coalescedReads);
        assertThat(naiveReads).isGreaterThan(50);
        assertThat(coalescedReads).isEqualTo(1);
    }

    private int stampede(CacheAside cache, int n) throws Exception {
        int before = db.reads();
        CyclicBarrier go = new CyclicBarrier(n);
        List<Future<Long>> fs = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(n)) {
            for (int i = 0; i < n; i++) {
                fs.add(pool.submit(() -> {
                    go.await();
                    return cache.get(1).price();
                }));
            }
        }
        for (Future<Long> f : fs) {
            assertThat(f.get()).isEqualTo(100);
        }
        return db.reads() - before;
    }

    @Test
    void deleteOnWriteLeavesAStaleValueVersionedTombstoneDoesNot() throws Exception {
        long naiveStale = raceReadAgainstWrite(false);
        long versionedAfter = raceReadAgainstWrite(true);
        System.out.printf("[LAB] doc cham + ghi chen vao giua: xoa-khi-ghi -> cache tra %d (CU, toi het TTL); version+tombstone -> %d%n",
                naiveStale, versionedAfter);
        assertThat(naiveStale).isEqualTo(100);
        assertThat(versionedAfter).isEqualTo(120);
    }

    /** R: miss -> doc DB (100, v1) ... W: UPDATE 120 (v2) + xoa/tombstone ... R: SET 100. Roi doc lai. */
    private long raceReadAgainstWrite(boolean versioned) throws Exception {
        new JdbcTemplate(ds).update("UPDATE product SET price = 100, version = 1 WHERE id = 1");
        CacheAside cache = new CacheAside(db, versioned, 60_000, System::currentTimeMillis);
        CountDownLatch readDone = new CountDownLatch(1), writeDone = new CountDownLatch(1);
        cache.afterDbRead(() -> {
            readDone.countDown();
            await(writeDone); // R da cam gia cu, dung lai truoc khi SET
        });
        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            Future<?> reader = pool.submit(() -> cache.get(1));
            assertThat(readDone.await(5, TimeUnit.SECONDS)).isTrue();
            cache.afterDbRead(() -> {});
            cache.updatePrice(1, 120); // W chen vao
            writeDone.countDown();
            reader.get(5, TimeUnit.SECONDS);
        }
        return cache.get(1).price();
    }

    @Test
    void theSameVersionRulesHoldAtomicallyInRedisLua() {
        try (RedisServer redis = RedisServer.startOrSkip(); JedisPooled r = redis.client()) {
            String sha = r.scriptLoad(resource("cache/put_if_newer.lua"));
            // reader nap ban v1 cham; writer da ghi tombstone v2
            assertThat(put(r, sha, 2, "", "T")).isEqualTo(1L);
            assertThat(put(r, sha, 1, "price=100", "V")).isZero();  // gia tri cu bi tu choi
            assertThat(put(r, sha, 2, "price=120", "V")).isEqualTo(1L);
            assertThat(put(r, sha, 2, "price=120", "V")).isZero();  // da co gia tri v2
            assertThat(put(r, sha, 3, "", "T")).isEqualTo(1L);
            assertThat(r.hget("product:1", "k")).isEqualTo("T");
        }
    }

    private static Long put(JedisPooled r, String sha, long version, String data, String kind) {
        return (Long) r.evalsha(sha, List.of("product:1"), List.of(String.valueOf(version), data, kind, "60000"));
    }

    private static String resource(String path) {
        try (InputStream in = CacheTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
