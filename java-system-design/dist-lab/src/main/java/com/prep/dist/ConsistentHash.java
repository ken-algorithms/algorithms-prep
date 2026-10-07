package com.prep.dist;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/** Vong bam nhat quan voi virtual node. */
public final class ConsistentHash {
    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final int vnodes;

    public ConsistentHash(int vnodes) { this.vnodes = vnodes; }

    public void add(String node) {
        for (int i = 0; i < vnodes; i++) ring.put(hash(node + "#" + i), node);
    }

    public void remove(String node) {
        for (int i = 0; i < vnodes; i++) ring.remove(hash(node + "#" + i));
    }

    public String nodeFor(String key) {
        SortedMap<Long, String> tail = ring.tailMap(hash(key));
        return tail.isEmpty() ? ring.firstEntry().getValue() : tail.get(tail.firstKey());
    }

    static long hash(String s) {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            long h = 0;
            for (int i = 0; i < 8; i++) h = (h << 8) | (d[i] & 0xff);
            return h;
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    // ---------- do dac ----------
    public static void main(String[] a) {
        int keys = 1_000_000;
        List<String> ks = new ArrayList<>(keys);
        for (int i = 0; i < keys; i++) ks.add("acct-" + i);

        // 1) mod N: them node thu 5
        int moved = 0;
        for (String k : ks) {
            long h = Math.floorMod(hash(k), 4L), h2 = Math.floorMod(hash(k), 5L);
            if (h != h2) moved++;
        }
        System.out.printf("hash mod N, 4 -> 5 node: %.1f%% key phai chuyen%n", 100.0 * moved / keys);

        for (int v : new int[]{1, 10, 100, 200}) {
            var ring = new ConsistentHash(v);
            for (int n = 0; n < 4; n++) ring.add("node-" + n);
            Map<String, String> before = new HashMap<>(keys * 2);
            Map<String, Integer> load = new HashMap<>();
            for (String k : ks) { String n = ring.nodeFor(k); before.put(k, n); load.merge(n, 1, Integer::sum); }
            ring.add("node-4");
            int mv = 0;
            for (String k : ks) if (!ring.nodeFor(k).equals(before.get(k))) mv++;
            int max = load.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            int min = load.values().stream().mapToInt(Integer::intValue).min().orElse(0);
            System.out.printf("ring vnodes=%3d: 4 node tai max/min = %.2f (max %.1f%%, ly tuong 25%%) | them node 5: %.1f%% key chuyen (ly tuong 20%%)%n",
                    v, (double) max / min, 100.0 * max / keys, 100.0 * mv / keys);
        }
    }
}
