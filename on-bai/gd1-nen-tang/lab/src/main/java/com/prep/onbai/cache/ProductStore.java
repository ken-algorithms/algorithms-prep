package com.prep.onbai.cache;

import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Nguon su that (Postgres). Dem so lan doc de test thay stampede. */
public final class ProductStore {

    public record Product(long id, String name, long price, long version) {}

    private final JdbcTemplate jdbc;
    private final AtomicInteger reads = new AtomicInteger();
    private volatile long readDelayMs;

    public ProductStore(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
    }

    public Product find(long id) {
        reads.incrementAndGet();
        Product p = jdbc.queryForObject("SELECT id, name, price, version FROM product WHERE id = ?",
                (rs, i) -> new Product(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getLong(4)), id);
        sleep(readDelayMs); // query nang that su (join, aggregate...) -> cua so stampede rong ra
        return p;
    }

    /** Ghi gia moi, tra ve version moi (DB cap, tang don dieu). */
    public long updatePrice(long id, long price) {
        return jdbc.queryForObject("UPDATE product SET price = ?, version = version + 1 WHERE id = ? RETURNING version",
                Long.class, price, id);
    }

    public int reads() {
        return reads.get();
    }

    public void readDelayMs(long ms) {
        this.readDelayMs = ms;
    }

    private static void sleep(long ms) {
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
