package com.prep.onbai.shortener;

import java.math.BigInteger;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Sinh ma 7 ky tu tu mot sequence, qua mot HOAN VI cua [0, 62^7):
 * <pre>
 *   n = nextval(seq)  ->  p = (A*n + B) mod 62^7  ->  base62(p), dem du 7 ky tu
 * </pre>
 * Hoan vi (A nguyen to cung nhau voi 62^7) la song anh: n khac nhau -> ma khac nhau, KHONG can kiem tra trung,
 * KHONG can retry. Ma lien tiep trong ngau nhien nen khong ai do duoc ma ke tiep bang cach +1.
 * (Khong phai ma hoa: lay du mau van giai duoc A, B. Can bi mat that thi dung Feistel co khoa.)
 */
public final class UrlShortener {

    public static final int LENGTH = 7;
    static final BigInteger SPACE = BigInteger.valueOf(62).pow(LENGTH);
    static final BigInteger A = BigInteger.valueOf(2176477521915L); // ~ 62^7 x 0,618 (ti le vang): ma lien tiep nhay khap khong gian
    static final BigInteger B = BigInteger.valueOf(1_234_567_891L);

    static {
        if (!A.gcd(SPACE).equals(BigInteger.ONE)) {
            throw new IllegalStateException("A phai nguyen to cung nhau voi 62^7 thi moi la hoan vi");
        }
    }

    public static final class AliasTaken extends RuntimeException {
        AliasTaken(String alias) {
            super("alias da co nguoi dung: " + alias);
        }
    }

    private final JdbcTemplate jdbc;

    public UrlShortener(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
    }

    public String shorten(String longUrl) {
        long n = jdbc.queryForObject("SELECT nextval('short_url_seq')", Long.class);
        String code = codeFor(n);
        jdbc.update("INSERT INTO short_url (code, long_url) VALUES (?, ?)", code, longUrl);
        return code;
    }

    /** Alias tu chon nam o KHONG GIAN TEN RIENG (khong phai dung 7 ky tu base62) -> khong bao gio chiem ma sinh tu dong. */
    public String shortenWithAlias(String longUrl, String alias) {
        if (alias.length() < 3 || alias.length() > 40 || (alias.length() == LENGTH && Base62.isBase62(alias))
                || !alias.matches("[0-9A-Za-z-]+")) {
            throw new IllegalArgumentException("alias phai 3-40 ky tu [0-9A-Za-z-], va khong dung dang ma tu dong");
        }
        try {
            jdbc.update("INSERT INTO short_url (code, long_url, custom) VALUES (?, ?, true)", alias, longUrl);
            return alias;
        } catch (DuplicateKeyException e) {
            throw new AliasTaken(alias); // unique index quyet dinh, khong "SELECT roi INSERT"
        }
    }

    public Optional<String> resolve(String code) {
        return jdbc.query("SELECT long_url FROM short_url WHERE code = ?", rs -> rs.next() ? Optional.of(rs.getString(1)) : Optional.empty(), code);
    }

    static String codeFor(long n) {
        long permuted = A.multiply(BigInteger.valueOf(n)).add(B).mod(SPACE).longValueExact();
        return Base62.encode(permuted, LENGTH);
    }
}
