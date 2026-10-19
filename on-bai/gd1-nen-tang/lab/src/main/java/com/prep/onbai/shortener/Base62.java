package com.prep.onbai.shortener;

/** 0-9A-Za-z: an toan trong URL, khong can escape. 7 ky tu = 62^7 ~ 3,52 x 10^12 gia tri. */
public final class Base62 {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private Base62() {}

    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("chi ma hoa so khong am");
        }
        if (value == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(ALPHABET.charAt((int) (value % 62)));
            value /= 62;
        }
        return sb.reverse().toString();
    }

    /** Dem trai bang '0' cho du width ky tu: moi ma cung do dai. */
    public static String encode(long value, int width) {
        String s = encode(value);
        if (s.length() > width) {
            throw new IllegalArgumentException(value + " can hon " + width + " ky tu");
        }
        return "0".repeat(width - s.length()) + s;
    }

    public static long decode(String s) {
        long v = 0;
        for (int i = 0; i < s.length(); i++) {
            int d = ALPHABET.indexOf(s.charAt(i));
            if (d < 0) {
                throw new IllegalArgumentException("ky tu khong hop le: " + s.charAt(i));
            }
            v = Math.addExact(Math.multiplyExact(v, 62), d);
        }
        return v;
    }

    public static boolean isBase62(String s) {
        return !s.isEmpty() && s.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0);
    }
}
