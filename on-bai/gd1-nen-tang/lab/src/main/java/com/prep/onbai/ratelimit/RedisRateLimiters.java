package com.prep.onbai.ratelimit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.SetParams;

/**
 * Rate limiter dung chung cho NHIEU instance app: trang thai nam o Redis.
 * Ba ban, tu sai toi dung — moi ban la mot cau tra loi hay gap trong phong van.
 */
public final class RedisRateLimiters {

    private final JedisPooled redis;
    private final String tokenBucketSha;

    public RedisRateLimiters(JedisPooled redis) {
        this.redis = redis;
        this.tokenBucketSha = redis.scriptLoad(resource("ratelimit/token_bucket.lua"));
    }

    /**
     * SAI: GET roi SET la HAI lenh. Hai instance cung GET 99 -> cung SET 100 -> limit 100 cho qua 101.
     * gap = cho test chen instance khac vao giua hai lenh.
     */
    public boolean naiveGetThenSet(String key, int limit, Runnable gap) {
        String v = redis.get(key);
        int count = v == null ? 0 : Integer.parseInt(v);
        if (count >= limit) {
            return false;
        }
        gap.run();
        redis.set(key, String.valueOf(count + 1), SetParams.setParams().px(60_000));
        return true;
    }

    /**
     * DUNG nhung co gioi han: INCR nguyen tu -> khong bao gio dem sai. Cua so co dinh (lo hong o bien).
     * SET NX PX tao key KEM TTL trong mot lenh: tranh bay "INCR roi EXPIRE", chet giua hai lenh = key khong bao gio het han.
     */
    public boolean fixedWindowIncr(String key, int limit, long windowMillis) {
        String k = key + ":" + (System.currentTimeMillis() / windowMillis);
        redis.set(k, "0", SetParams.setParams().nx().px(windowMillis * 2));
        return redis.incr(k) <= limit;
    }

    /** DUNG: token bucket trong Lua -> doc-tinh-ghi chay nguyen tu tren Redis, dung dong ho CUA Redis. */
    public Decision tokenBucket(String key, int capacity, double refillPerSecond) {
        @SuppressWarnings("unchecked")
        List<Object> r = (List<Object>) redis.evalsha(tokenBucketSha, List.of(key),
                List.of(String.valueOf(capacity), String.valueOf(refillPerSecond), "1"));
        return new Decision(((Long) r.get(0)) == 1L, ((Long) r.get(1)).intValue());
    }

    /** remaining -> header X-RateLimit-Remaining; bi tu choi -> 429 + Retry-After. */
    public record Decision(boolean allowed, int remaining) {}

    private static String resource(String path) {
        try (InputStream in = RedisRateLimiters.class.getClassLoader().getResourceAsStream(path)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
