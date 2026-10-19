package com.prep.onbai.outbox;

import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ANTI-PATTERN "dual write": ghi DB va publish broker la HAI he thong, khong co transaction chung.
 * Publish truoc commit + commit hong = su kien MA (don hang khong ton tai). Commit truoc publish + publish hong = MAT su kien.
 */
public final class DualWriteOrderService {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final FakeBroker broker;

    public DualWriteOrderService(DataSource ds, FakeBroker broker) {
        this.jdbc = new JdbcTemplate(ds);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        this.broker = broker;
    }

    public long placeOrder(String customer, long totalMinor, Runnable beforeCommit) {
        return tx.execute(s -> {
            long id = jdbc.queryForObject("INSERT INTO orders (customer, total_minor) VALUES (?, ?) RETURNING id",
                    Long.class, customer, totalMinor);
            broker.publish("orders", String.valueOf(id), UUID.randomUUID().toString(), "{\"orderId\":" + id + "}");
            beforeCommit.run(); // vd: buoc sau loi validate, het timeout, constraint luc commit...
            return id;
        });
    }
}
