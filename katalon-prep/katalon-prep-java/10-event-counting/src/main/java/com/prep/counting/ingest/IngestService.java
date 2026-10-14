package com.prep.counting.ingest;

import static com.prep.counting.domain.Times.utc;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.prep.counting.domain.EventBatch;
import com.prep.counting.domain.EventClassifier;
import com.prep.counting.rollup.RollupJob;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The write path: validate, classify, append to raw, answer 202. No lock, no counter, no aggregation here.
 * Counting happens later, in {@link RollupJob}, so a burst of writes never waits for arithmetic.
 */
public final class IngestService {

    public static final int MAX_ITEMS = 1000;
    public static final Duration RAW_RETENTION = Duration.ofDays(7);
    public static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);
    private static final int MAX_KEY_LENGTH = 200;

    private final JdbcTemplate jdbc;
    private final EventClassifier classifier;
    private final PartitionManager partitions;
    private final Clock clock;

    private final AtomicLong rejectedTooLate = new AtomicLong();
    private final AtomicLong lateBeyondWindow = new AtomicLong();

    public IngestService(JdbcTemplate jdbc, EventClassifier classifier, PartitionManager partitions, Clock clock) {
        this.jdbc = jdbc;
        this.classifier = classifier;
        this.partitions = partitions;
        this.clock = clock;
    }

    public IngestResult ingest(EventBatch b) {
        validate(b);
        int n = b.items().size();
        Instant now = clock.instant();
        if (b.ts().isBefore(now.minus(RAW_RETENTION))) {
            rejectedTooLate.addAndGet(n); // never drop silently: this metric shows a broken client
            throw new TooLateException("event time " + b.ts() + " is older than raw retention (7 days)");
        }
        if (b.ts().isAfter(now.plus(MAX_CLOCK_SKEW))) {
            throw new IllegalArgumentException("event time " + b.ts() + " is in the future");
        }
        if (b.ts().isBefore(now.minus(RollupJob.RECOMPUTE_WINDOW))) {
            lateBeyondWindow.addAndGet(n); // still stored; the hourly reconcile repairs its bucket
        }
        partitions.ensureFor(b.ts());

        // one multi-row INSERT = one round trip; ON CONFLICT makes a retried batch a no-op
        StringBuilder sql = new StringBuilder("INSERT INTO raw_events (event_id, ts, tenant_id, key_name, outcome) VALUES ");
        List<Object> args = new ArrayList<>(n * 5);
        OffsetDateTime ts = utc(b.ts());
        for (int i = 0; i < n; i++) {
            EventBatch.Item it = b.items().get(i);
            sql.append(i == 0 ? "(?,?,?,?,?)" : ",(?,?,?,?,?)");
            args.add(eventId(b.tenantId(), b.batchId(), i));
            args.add(ts);
            args.add(b.tenantId());
            args.add(keyName(it.key()));
            args.add(classifier.classify(it.key(), it.value()).code);
        }
        sql.append(" ON CONFLICT (event_id, ts) DO NOTHING");
        int inserted = jdbc.update(sql.toString(), args.toArray());
        return new IngestResult(n, inserted, n - inserted);
    }

    /** Same tenant, batch and position give the same id on every retry. A server-side random id would not. */
    public static UUID eventId(String tenantId, String batchId, int index) {
        return UUID.nameUUIDFromBytes((tenantId + '/' + batchId + '/' + index).getBytes(UTF_8));
    }

    private static void validate(EventBatch b) {
        if (b.tenantId() == null || b.tenantId().isBlank() || b.batchId() == null || b.batchId().isBlank()) {
            throw new IllegalArgumentException("tenantId and batchId are required");
        }
        if (b.ts() == null) {
            throw new IllegalArgumentException("ts (event time) is required");
        }
        if (b.items() == null || b.items().isEmpty() || b.items().size() > MAX_ITEMS) {
            throw new IllegalArgumentException("items must hold 1 to " + MAX_ITEMS + " key-value pairs");
        }
    }

    private static String keyName(String key) {
        if (key == null) {
            return "";
        }
        return key.length() <= MAX_KEY_LENGTH ? key : key.substring(0, MAX_KEY_LENGTH);
    }

    public long rejectedTooLate() {
        return rejectedTooLate.get();
    }

    public long lateBeyondWindow() {
        return lateBeyondWindow.get();
    }
}
