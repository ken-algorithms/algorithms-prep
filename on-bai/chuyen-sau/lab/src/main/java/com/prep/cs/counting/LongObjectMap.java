package com.prep.cs.counting;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Map long -> V dia chi mo, khong boxing. HashMap<Long, V> tao mot Long (16 byte) moi lan get/put voi khoa
 * ngoai [-128, 127]: o 10 trieu request/giay la 160 MB rac moi giay chi de tra cuu. Map nay khong tao rac.
 * Xoa dung "backward shift" (khong de lai bia mo), nen bang khong ban dan sau nhieu lan don client cu.
 */
public final class LongObjectMap<V> {

    private long[] keys;
    private Object[] vals;
    private int size, mask;

    public LongObjectMap(int expected) {
        int cap = Integer.highestOneBit(Math.max(4, expected * 2 - 1)) << 1;
        keys = new long[cap];
        vals = new Object[cap];
        mask = cap - 1;
    }

    private int slot(long k) {
        return (int) Hash128.fmix64(k) & mask;
    }

    @SuppressWarnings("unchecked")
    public V get(long k) {
        for (int i = slot(k); ; i = (i + 1) & mask) {
            Object v = vals[i];
            if (v == null) return null;
            if (keys[i] == k) return (V) v;
        }
    }

    public void put(long k, V v) {
        if ((size + 1) * 4 > keys.length * 3) grow();     // he so tai toi da 0.75
        for (int i = slot(k); ; i = (i + 1) & mask) {
            if (vals[i] == null) {
                keys[i] = k;
                vals[i] = v;
                size++;
                return;
            }
            if (keys[i] == k) {
                vals[i] = v;
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer<Long, V> f) {
        for (int i = 0; i < keys.length; i++) if (vals[i] != null) f.accept(keys[i], (V) vals[i]);
    }

    /** Xoa moi phan tu thoa dieu kien; tra so phan tu da xoa. */
    @SuppressWarnings("unchecked")
    public int removeIf(Predicate<V> p) {
        int removed = 0;
        for (int i = 0; i < keys.length; ) {
            if (vals[i] != null && p.test((V) vals[i])) {
                deleteAt(i);
                removed++;               // khong tang i: o i vua nhan phan tu doi len tu phia sau
            } else {
                i++;
            }
        }
        return removed;
    }

    private void deleteAt(int i) {
        vals[i] = null;
        size--;
        for (int j = (i + 1) & mask; vals[j] != null; j = (j + 1) & mask) {
            int home = slot(keys[j]);
            // phan tu o j co the doi ve i neu i nam tren duong do tu home toi j
            if (((j - home) & mask) >= ((j - i) & mask)) {
                keys[i] = keys[j];
                vals[i] = vals[j];
                vals[j] = null;
                i = j;
            }
        }
    }

    public void clear() {
        java.util.Arrays.fill(vals, null);
        size = 0;
    }

    public int size() {
        return size;
    }

    private void grow() {
        long[] ok = keys;
        Object[] ov = vals;
        keys = new long[ok.length * 2];
        vals = new Object[ok.length * 2];
        mask = keys.length - 1;
        size = 0;
        for (int i = 0; i < ok.length; i++) {
            if (ov[i] != null) {
                @SuppressWarnings("unchecked") V v = (V) ov[i];
                put(ok[i], v);
            }
        }
    }
}
