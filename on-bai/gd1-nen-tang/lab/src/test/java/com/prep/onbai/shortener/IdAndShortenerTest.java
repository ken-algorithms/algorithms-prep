package com.prep.onbai.shortener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prep.onbai.testsupport.Pg;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

/** Bai 07 — Snowflake, base62, rut gon URL tren Postgres that. */
class IdAndShortenerTest {

    @Test
    void snowflakeIsUniqueAndIncreasingAcrossEightThreads() throws Exception {
        Snowflake gen = new Snowflake(7, System::currentTimeMillis);
        Set<Long> all = ConcurrentHashMap.newKeySet();
        List<Future<Boolean>> fs = new ArrayList<>();
        long t0 = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (int t = 0; t < 8; t++) {
                fs.add(pool.submit(() -> {
                    long prev = -1;
                    boolean increasing = true;
                    for (int i = 0; i < 50_000; i++) {
                        long id = gen.next();
                        increasing &= id > prev;
                        prev = id;
                        all.add(id);
                    }
                    return increasing;
                }));
            }
        }
        for (Future<Boolean> f : fs) {
            assertThat(f.get()).isTrue(); // trong moi luong: tang dan
        }
        long ms = Math.max(1, (System.nanoTime() - t0) / 1_000_000);
        long sample = all.iterator().next();
        System.out.printf("[LAB] Snowflake: 400.000 id tu 8 luong, %d trung, ~%d id/ms; id %d -> worker %d, %d ky tu base62%n",
                400_000 - all.size(), 400_000 / ms, sample, Snowflake.workerOf(sample), Base62.encode(sample).length());
        assertThat(all).hasSize(400_000);
        assertThat(Snowflake.workerOf(sample)).isEqualTo(7);
    }

    @Test
    void twoPodsWithTheSameWorkerIdProduceDuplicates() {
        long fixed = Snowflake.EPOCH + 1_000;
        Snowflake podA = new Snowflake(3, () -> fixed), podB = new Snowflake(3, () -> fixed);
        assertThat(podA.next()).isEqualTo(podB.next()); // cung ms, cung worker, cung seq -> TRUNG
        Snowflake podC = new Snowflake(4, () -> fixed);
        assertThat(podC.next()).isNotEqualTo(new Snowflake(3, () -> fixed).next());
    }

    @Test
    void sequenceOverflowMovesToTheNextMillisecond() {
        AtomicLong calls = new AtomicLong();
        long base = Snowflake.EPOCH + 5_000;
        Snowflake gen = new Snowflake(1, () -> base + calls.getAndIncrement() / 5_000); // dong ho nhich 1 ms moi 5.000 lan hoi
        long last = 0;
        for (int i = 0; i < 4_096; i++) {
            last = gen.next();
        }
        long overflow = gen.next();
        assertThat(Snowflake.sequenceOf(last)).isEqualTo(4_095);
        assertThat(Snowflake.timestampOf(overflow)).isEqualTo(Snowflake.timestampOf(last) + 1);
        assertThat(Snowflake.sequenceOf(overflow)).isZero();
    }

    @Test
    void aSmallClockStepBackWaitsALargeOneRefuses() {
        long t = Snowflake.EPOCH + 10_000;
        long[] readings = {t, t - 3, t - 2, t - 1, t, t - 500};
        int[] i = {0};
        Snowflake gen = new Snowflake(2, () -> readings[Math.min(i[0]++, readings.length - 1)]);
        long first = gen.next();
        long second = gen.next(); // doc t-3 -> cho toi khi dong ho ve lai t
        assertThat(second).isGreaterThan(first);
        assertThatThrownBy(gen::next).hasMessageContaining("dong ho lui 500 ms");
    }

    @Test
    void base62RoundTripsAndSevenCharsCoverTheSpace() {
        for (int i = 0; i < 10_000; i++) {
            long v = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
            assertThat(Base62.decode(Base62.encode(v))).isEqualTo(v);
        }
        long maxSeven = UrlShortener.SPACE.longValueExact() - 1;
        assertThat(Base62.encode(maxSeven)).isEqualTo("zzzzzzz");
        assertThat(Base62.encode(Long.MAX_VALUE)).hasSize(11);
    }

    @Test
    void shortCodesAreUniqueFixedLengthAndNotGuessable() {
        DataSource ds = Pg.freshDatabase("b07_shortener", "shortener.sql");
        UrlShortener s = new UrlShortener(ds);
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            codes.add(s.shorten("https://katalon.com/docs/page/" + i));
        }
        Set<String> unique = new HashSet<>(codes);
        List<String> sorted = new ArrayList<>(codes);
        sorted.sort(null);
        System.out.println("[LAB] 5 ma lien tiep tu sequence 1..5: " + codes.subList(0, 5));
        assertThat(unique).hasSize(5_000);
        assertThat(codes).allSatisfy(c -> assertThat(c).hasSize(7));
        assertThat(sorted).isNotEqualTo(codes); // thu tu tao != thu tu ma: khong doan duoc ma ke tiep
        assertThat(s.resolve(codes.get(42))).contains("https://katalon.com/docs/page/42");
        assertThat(s.resolve("nope123")).isEmpty();
    }

    @Test
    void customAliasesLiveInTheirOwnNamespace() {
        DataSource ds = Pg.freshDatabase("b07_alias", "shortener.sql");
        UrlShortener s = new UrlShortener(ds);
        assertThat(s.shortenWithAlias("https://katalon.com/careers", "katalon-jobs")).isEqualTo("katalon-jobs");
        assertThatThrownBy(() -> s.shortenWithAlias("https://evil.example", "katalon-jobs"))
                .isInstanceOf(UrlShortener.AliasTaken.class);
        assertThatThrownBy(() -> s.shortenWithAlias("https://x.example", "abcDEF1")) // dung dang ma tu dong
                .isInstanceOf(IllegalArgumentException.class);
    }
}
