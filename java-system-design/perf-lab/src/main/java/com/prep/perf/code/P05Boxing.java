package com.prep.perf.code;

import java.util.HashMap;
import java.util.Map;

/**
 * P05 - Autoboxing tren duong nong.
 *
 * <p>{@code Map<Integer,Integer>} lam bo dem: moi lan merge tao Integer moi (ngoai cache -128..127)
 * va mot Node 32 byte; doc ra phai unbox va theo con tro di khap heap (cache miss). Mang primitive
 * nam lien tuc trong bo nho.
 *
 * <p>{@code Long total = 0L; total += x} la bien the kin dao hon: JIT CO THE go bo boxing nho escape
 * analysis - benchmark se cho biet lan nay no co go duoc hay khong. Dung dua vao viec JIT cuu minh.
 */
public final class P05Boxing {

    private P05Boxing() {}

    /** XAU: histogram bang HashMap boxed. */
    public static Map<Integer, Integer> histogramBad(int[] values) {
        Map<Integer, Integer> h = new HashMap<>();
        for (int v : values) {
            h.merge(v, 1, Integer::sum);
        }
        return h;
    }

    /** SUA: khoa la so nho lien tuc -> mang primitive. */
    public static int[] histogramGood(int[] values, int buckets) {
        int[] h = new int[buckets];
        for (int v : values) {
            h[v]++;
        }
        return h;
    }

    /** XAU (kin): bien tich luy kieu Long. */
    public static long sumBad(long[] amounts) {
        Long total = 0L;
        for (long a : amounts) {
            total += a;
        }
        return total;
    }

    public static long sumGood(long[] amounts) {
        long total = 0L;
        for (long a : amounts) {
            total += a;
        }
        return total;
    }
}
