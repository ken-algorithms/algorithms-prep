package com.prep.perf.code;

import java.util.List;

/**
 * P03 - Noi chuoi trong vong lap.
 *
 * <p>Tu Java 9, {@code a + b} dung invokedynamic nen MOT bieu thuc noi chuoi da nhanh. Nhung
 * {@code out += row} trong vong lap van tao String moi moi vong va copy lai toan bo phan truoc:
 * tong so byte copy ~ n^2/2. 10.000 dong x 40 byte = ~2 GB copy cho mot file export.
 */
public final class P03StringConcat {

    private P03StringConcat() {}

    /** XAU: O(n^2) byte copy, rac tang theo binh phuong. */
    public static String csvBad(List<String> rows) {
        String out = "";
        for (String r : rows) {
            out += r + "\n";
        }
        return out;
    }

    /** SUA: mot buffer, uoc luong san dung luong de khoi phai grow. */
    public static String csvGood(List<String> rows) {
        StringBuilder sb = new StringBuilder(rows.size() * 48);
        for (String r : rows) {
            sb.append(r).append('\n');
        }
        return sb.toString();
    }
}
