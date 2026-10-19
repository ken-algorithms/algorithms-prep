package com.prep.onbai.outbox;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Relay: doc dong CHUA PHAT, publish, danh dau da phat. Chay N ban song song: FOR UPDATE SKIP LOCKED chia viec,
 * khong ban nao lay trung dong cua ban khac. Chet giua "publish" va "danh dau" -> phat lai -> consumer phai idempotent.
 * <p>
 * Doi lai su don gian: publish nam TRONG transaction (giu khoa dong + connection trong luc goi broker).
 * Broker cham thi doi sang: claim (lease ngan) -> publish ngoai transaction -> danh dau.
 */
public final class OutboxRelay {

    record Row(long id, String messageId, String aggregateId, String eventType, String payload, int attempts) {}

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final FakeBroker broker;
    private final long baseBackoffMs;
    private final long capBackoffMs;
    private volatile Runnable afterPublish = () -> {};

    public OutboxRelay(DataSource ds, FakeBroker broker, long baseBackoffMs, long capBackoffMs) {
        this.jdbc = new JdbcTemplate(ds);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        this.broker = broker;
        this.baseBackoffMs = baseBackoffMs;
        this.capBackoffMs = capBackoffMs;
    }

    /** Mot vong: tra ve so message da phat. */
    public int pollOnce(int batchSize) {
        return tx.execute(s -> {
            List<Row> rows = jdbc.query("""
                    SELECT id, message_id, aggregate_id, event_type, payload::text, attempts
                      FROM outbox
                     WHERE published_at IS NULL AND next_attempt_at <= clock_timestamp()
                     ORDER BY id
                     LIMIT ?
                       FOR UPDATE SKIP LOCKED""",
                    (rs, i) -> new Row(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6)),
                    batchSize);
            int published = 0;
            for (Row r : rows) {
                try {
                    broker.publish("orders", r.aggregateId(), r.messageId(), r.payload());
                    afterPublish.run(); // test: relay chet ngay sau khi broker da nhan
                    jdbc.update("UPDATE outbox SET published_at = clock_timestamp(), attempts = attempts + 1 WHERE id = ?", r.id());
                    published++;
                } catch (FakeBroker.Unavailable e) {
                    long delay = Backoff.fullJitterMs(r.attempts(), baseBackoffMs, capBackoffMs, ThreadLocalRandom.current());
                    jdbc.update("""
                            UPDATE outbox SET attempts = attempts + 1, last_error = ?,
                                   next_attempt_at = clock_timestamp() + ?::interval
                             WHERE id = ?""", e.getMessage(), delay + " milliseconds", r.id());
                }
            }
            return published;
        });
    }

    public void afterPublish(Runnable hook) {
        this.afterPublish = hook;
    }
}
