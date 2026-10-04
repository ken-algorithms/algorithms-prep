package com.prep.perf.code;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * P07 - O(n^2) an trong code trong vo hai.
 *
 * <p>{@code list.contains} trong vong lap, {@code list.remove(0)} tren ArrayList, {@code stream().filter}
 * long trong {@code forEach}... Voi n = 100 khong ai thay. Voi n = 10.000 (mot batch doi soat, mot
 * file import) la 50 trieu phep so sanh tren MOT request - va request do giu thread, connection,
 * lock trong suot thoi gian ay.
 */
public final class P07HiddenQuadratic {

    private P07HiddenQuadratic() {}

    /** XAU: khu trung bang List.contains -> O(n^2). */
    public static List<String> dedupeBad(List<String> ids) {
        List<String> out = new ArrayList<>();
        for (String id : ids) {
            if (!out.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }

    /** SUA: LinkedHashSet giu thu tu xuat hien dau tien, O(n). */
    public static List<String> dedupeGood(List<String> ids) {
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
