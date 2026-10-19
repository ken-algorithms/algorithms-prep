package com.prep.onbai.outbox;

import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Consumer idempotent: ghi message_id vao inbox VA tao notification trong MOT transaction.
 * Message trung (relay phat lai, broker giao lai) -> INSERT inbox khong vao -> bo qua, khong gui email lan hai.
 */
public final class NotificationConsumer {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public NotificationConsumer(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    }

    /** true = xu ly that; false = trung, bo qua. */
    public boolean handle(FakeBroker.Message m) {
        return Boolean.TRUE.equals(tx.execute(s -> {
            int fresh = jdbc.update("INSERT INTO inbox (consumer, message_id) VALUES ('email', ?::uuid) ON CONFLICT DO NOTHING",
                    m.messageId());
            if (fresh == 0) {
                return false;
            }
            long orderId = jdbc.queryForObject("SELECT (?::jsonb ->> 'orderId')::bigint", Long.class, m.payload());
            jdbc.update("INSERT INTO notification (order_id, channel, body) VALUES (?, 'email', ?)",
                    orderId, "Cam on ban da dat hang #" + orderId);
            return true;
        }));
    }
}
