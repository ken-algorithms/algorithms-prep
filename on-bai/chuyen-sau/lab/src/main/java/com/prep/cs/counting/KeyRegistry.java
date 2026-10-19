package com.prep.cs.counting;

import java.util.Collection;

/**
 * Tap key hop le, giu TRONG RAM cua moi ingest node, tra cuu khong cap phat.
 * Bang bam dia chi mo (open addressing, do tuyen tinh) tren hai mang long:
 *   fp1[slot] = lane 1 cua Hash128 (0 nghia la o trong), fp2[slot] = lane 2 de xac nhan.
 * He so tai <= 0.6, so o la luy thua cua 2: 1 trieu key -> 2^21 o x 16 byte = 33,6 MB; 10 trieu key -> 2^24 o = 268 MB.
 * Bat bien sau khi dung xong: doi registry = dung bang moi roi doi tham chieu (copy-on-write), doc khong can khoa.
 */
public final class KeyRegistry {

    private final Hash128 hash;
    private final long[] fp1, fp2;
    private final int mask;
    private final long version;
    private int size;

    private KeyRegistry(Hash128 hash, int capacity, long version) {
        this.hash = hash;
        this.fp1 = new long[capacity];
        this.fp2 = new long[capacity];
        this.mask = capacity - 1;
        this.version = version;
    }

    public static KeyRegistry of(Collection<String> keys, Hash128 hash, long version) {
        int capacity = Integer.highestOneBit(Math.max(16, (int) (keys.size() / 0.6))) << 1;
        KeyRegistry r = new KeyRegistry(hash, capacity, version);
        long[] h = new long[2];
        for (String k : keys) {
            hash.hash(k, h);
            r.insert(h[0], h[1]);
        }
        return r;
    }

    private void insert(long h1, long h2) {
        if (h1 == 0) h1 = 1;                                   // 0 danh dau o trong
        for (int slot = (int) h1 & mask; ; slot = (slot + 1) & mask) {
            if (fp1[slot] == 0) {
                fp1[slot] = h1;
                fp2[slot] = h2;
                size++;
                return;
            }
            if (fp1[slot] == h1 && fp2[slot] == h2) return;     // key trung lap trong danh sach
        }
    }

    /** Duong nong: (h1, h2) la Hash128 cua key, nguoi goi bam san bang mang dung lai. */
    public boolean contains(long h1, long h2) {
        if (h1 == 0) h1 = 1;
        for (int slot = (int) h1 & mask; ; slot = (slot + 1) & mask) {
            long f = fp1[slot];
            if (f == 0) return false;                          // gap o trong: chac chan khong co
            if (f == h1 && fp2[slot] == h2) return true;
        }
    }

    public boolean contains(String key) {
        long[] h = new long[2];
        hash.hash(key, h);
        return contains(h[0], h[1]);
    }

    public Hash128 hash() {
        return hash;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return mask + 1;
    }

    public long version() {
        return version;
    }
}
