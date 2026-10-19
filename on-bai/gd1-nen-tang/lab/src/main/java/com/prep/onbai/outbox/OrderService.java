package com.prep.onbai.outbox;

import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Dat hang: dong orders va dong outbox trong MOT transaction -> hoac co ca hai, hoac khong co gi. */
public final class OrderService {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public OrderService(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    }

    public long placeOrder(String customer, long totalMinor) {
        return tx.execute(s -> {
            long id = jdbc.queryForObject("INSERT INTO orders (customer, total_minor) VALUES (?, ?) RETURNING id",
                    Long.class, customer, totalMinor);
            jdbc.update("INSERT INTO outbox (aggregate_id, event_type, payload) VALUES (?, 'ORDER_PLACED', ?::jsonb)",
                    String.valueOf(id), "{\"orderId\":" + id + ",\"customer\":\"" + customer + "\",\"totalMinor\":" + totalMinor + "}");
            return id;
        });
    }
}
