package com.prep.perf.code;

import java.util.regex.Pattern;

/**
 * P02 - Bien dich regex trong duong nong.
 *
 * <p>{@code String.matches}, {@code replaceAll}, {@code split} (tru truong hop 1 ky tu thuong) deu
 * goi {@code Pattern.compile} MOI LAN. Validate so tai khoan o moi request = compile lai mot cai
 * automaton moi request.
 */
public final class P02Regex {

    private P02Regex() {}

    static final String ACCOUNT_REGEX = "^[0-9]{3}-[0-9]{6,10}$";
    private static final Pattern ACCOUNT = Pattern.compile(ACCOUNT_REGEX);
    private static final Pattern COMMA = Pattern.compile("\\s*,\\s*");

    /** XAU: String.matches compile lai regex moi lan goi. */
    public static boolean isValidAccountBad(String s) {
        return s.matches(ACCOUNT_REGEX);
    }

    /** SUA: Pattern static final, chi tao Matcher (re). */
    public static boolean isValidAccountGood(String s) {
        return ACCOUNT.matcher(s).matches();
    }

    /** XAU: regex nhieu ky tu -> split compile moi lan. */
    public static String[] splitTagsBad(String s) {
        return s.split("\\s*,\\s*");
    }

    public static String[] splitTagsGood(String s) {
        return COMMA.split(s);
    }
}
