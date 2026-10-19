package com.prep.onbai.cache;

import com.prep.onbai.cache.ProductStore.Product;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/**
 * Hai ban cache-aside tren cung mot "Redis" trong bo nho (ConcurrentHashMap + TTL):
 * <pre>
 *  NAIVE    doc: miss -> DB -> SET           ghi: UPDATE DB -> DEL key
 *  VERSIONED doc: miss -> DB -> SET neu MOI HON  ghi: UPDATE DB -> SET tombstone(version moi)
 * </pre>
 * Race kinh dien cua ban naive: doc lay gia CU tu DB, ghi chen vao (UPDATE + DEL), doc moi SET gia CU
 * -> cache sai toi het TTL. Ban versioned tu choi SET do vi version cua no nho hon tombstone.
 */
public final class CacheAside {

    /** tombstone = "DB da o version nay, chua co gia tri": chi gia tri cung/moi hon moi duoc ghi de. */
    record Entry(Product value, long version, boolean tombstone, long expiresAtMs) {}

    private final ProductStore db;
    private final boolean versioned;
    private final long ttlMs;
    private final LongSupplier clockMs;
    private final ConcurrentHashMap<Long, Entry> cache = new ConcurrentHashMap<>();
    private final SingleFlight<Long, Product> flights = new SingleFlight<>();
    private volatile boolean singleFlight;
    private volatile Runnable afterDbRead = () -> {};

    public CacheAside(ProductStore db, boolean versioned, long ttlMs, LongSupplier clockMs) {
        this.db = db;
        this.versioned = versioned;
        this.ttlMs = ttlMs;
        this.clockMs = clockMs;
    }

    public Product get(long id) {
        Entry e = cache.get(id);
        if (e != null && !e.tombstone() && e.expiresAtMs() > clockMs.getAsLong()) {
            return e.value(); // hit
        }
        return singleFlight ? flights.load(id, this::loadAndFill) : loadAndFill(id);
    }

    private Product loadAndFill(long id) {
        Product p = db.find(id);
        afterDbRead.run(); // test chen mot lan ghi vao dung khe nay
        long expires = clockMs.getAsLong() + jitter(ttlMs);
        if (!versioned) {
            cache.put(id, new Entry(p, p.version(), false, expires));
        } else {
            cache.compute(id, (k, cur) -> acceptsValue(cur, p.version()) ? new Entry(p, p.version(), false, expires) : cur);
        }
        return p;
    }

    public void updatePrice(long id, long price) {
        long v = db.updatePrice(id, price);
        if (!versioned) {
            cache.remove(id);
        } else {
            long expires = clockMs.getAsLong() + ttlMs;
            cache.compute(id, (k, cur) -> cur == null || v >= cur.version() ? new Entry(null, v, true, expires) : cur);
        }
    }

    static boolean acceptsValue(Entry cur, long version) {
        return cur == null || version > cur.version() || (version == cur.version() && cur.tombstone());
    }

    /** TTL +-10%: hang nghin key nap cung luc khong het han cung luc. */
    static long jitter(long ttlMs) {
        return (long) (ttlMs * (0.9 + 0.2 * ThreadLocalRandom.current().nextDouble()));
    }

    public void singleFlight(boolean on) {
        this.singleFlight = on;
    }

    public void afterDbRead(Runnable hook) {
        this.afterDbRead = hook;
    }

    public void clear() {
        cache.clear();
    }
}
