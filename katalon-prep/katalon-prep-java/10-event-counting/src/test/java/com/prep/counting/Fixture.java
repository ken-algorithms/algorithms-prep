package com.prep.counting;

import com.prep.counting.domain.EventBatch;
import com.prep.counting.domain.EventClassifier;
import com.prep.counting.domain.KeyRegistry;
import com.prep.counting.ingest.IngestService;
import com.prep.counting.ingest.PartitionManager;
import com.prep.counting.query.StatsService;
import com.prep.counting.rejected.RawScanStats;
import com.prep.counting.rollup.Reconciler;
import com.prep.counting.rollup.RollupJob;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** The whole system wired by hand on a fresh database, plus a load generator shaped like the Katalon question. */
final class Fixture {

    static final List<String> KEYS = new ArrayList<>();

    static {
        for (int i = 0; i < 500; i++) {
            KEYS.add("feature_" + i);
        }
    }

    final DataSource ds;
    final JdbcTemplate jdbc;
    final TransactionTemplate tx;
    final MutableClock clock;
    final KeyRegistry registry;
    final EventClassifier classifier;
    final PartitionManager partitions;
    final IngestService ingest;
    final RollupJob rollup;
    final Reconciler reconciler;
    final StatsService stats;
    final RawScanStats oracle;

    Fixture(String db, Instant now) {
        ds = Pg.freshDatabase(db);
        jdbc = new JdbcTemplate(ds);
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        clock = new MutableClock(now);
        Set<String> keys = new HashSet<>(KEYS);
        registry = new KeyRegistry(keys);
        classifier = new EventClassifier(registry);
        partitions = new PartitionManager(jdbc);
        ingest = new IngestService(jdbc, classifier, partitions, clock);
        rollup = new RollupJob(jdbc, tx, clock);
        reconciler = new Reconciler(jdbc, rollup);
        stats = new StatsService(jdbc, rollup);
        oracle = new RawScanStats(jdbc);
    }

    /** One request: 20 items, ~15% fake keys, ~2% malformed values, the rest ~50% true (the shape of the question). */
    static EventBatch batch(String tenant, Instant ts, Random rnd) {
        List<EventBatch.Item> items = new ArrayList<>(20);
        for (int i = 0; i < 20; i++) {
            double r = rnd.nextDouble();
            if (r < 0.15) {
                items.add(new EventBatch.Item("fake_" + Integer.toHexString(rnd.nextInt()), true));
            } else if (r < 0.17) {
                items.add(new EventBatch.Item(KEYS.get(rnd.nextInt(KEYS.size())), rnd.nextBoolean() ? "yes" : null));
            } else {
                items.add(new EventBatch.Item(KEYS.get(rnd.nextInt(KEYS.size())), rnd.nextBoolean()));
            }
        }
        return new EventBatch(tenant, UUID.randomUUID().toString(), ts, items);
    }

    long rawCount() {
        return jdbc.queryForObject("SELECT count(*) FROM raw_events", Long.class);
    }
}
