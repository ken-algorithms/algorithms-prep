package com.prep.perf.code;

/**
 * P04 - Dung exception lam luong dieu khien.
 *
 * <p>Chi phi lon nhat cua exception la {@code fillInStackTrace()}: di qua TOAN BO stack. Trong
 * microbenchmark stack chi ~10 frame nen re; trong Spring MVC + filter + AOP + JPA, stack thuong
 * 100-200 frame -> cung mot exception dat gap nhieu lan. Benchmark co tham so {@code depth} de thay
 * hieu ung nay.
 *
 * <p>Exception cho TINH HUONG LOI that su la dung. Sai la khi input khong hop le la chuyen THUONG
 * (vd. 30% request co ma giam gia sai dinh dang) ma van di bang exception.
 */
public final class P04ExceptionFlow {

    private P04ExceptionFlow() {}

    /** XAU: input sai -> NumberFormatException -> fillInStackTrace tren ca stack. */
    public static int parseOrDefaultBad(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** SUA: kiem tra truoc. Chi con duong overflow (rat hiem) di qua exception. */
    public static int parseOrDefaultGood(String s, int def) {
        if (!looksLikeInt(s)) {
            return def;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException overflow) {
            return def;
        }
    }

    static boolean looksLikeInt(String s) {
        if (s == null || s.isEmpty() || s.length() > 11) {
            return false;
        }
        int i = (s.charAt(0) == '-' || s.charAt(0) == '+') ? 1 : 0;
        if (i == s.length()) {
            return false;
        }
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /** Goi {@code body} sau khi da de them {@code depth} frame len stack - mo phong stack Spring. */
    public static int atDepth(int depth, java.util.function.IntSupplier body) {
        return depth <= 0 ? body.getAsInt() : atDepth(depth - 1, body);
    }
}
