package com.prep.counting;

import com.prep.counting.domain.KeyRegistry;
import com.prep.counting.ingest.IngestService;
import com.prep.counting.ingest.PartitionManager;
import com.prep.counting.rollup.Reconciler;
import com.prep.counting.rollup.RollupJob;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** The four background jobs. With several app instances, run them on one (or guard with pg_try_advisory_lock). */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "counting.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class Jobs {

    private static final Logger log = LoggerFactory.getLogger(Jobs.class);

    private final JdbcTemplate jdbc;
    private final KeyRegistry registry;
    private final PartitionManager partitions;
    private final RollupJob rollup;
    private final Reconciler reconciler;
    private final Clock clock;

    public Jobs(JdbcTemplate jdbc, KeyRegistry registry, PartitionManager partitions, RollupJob rollup,
                Reconciler reconciler, Clock clock) {
        this.jdbc = jdbc;
        this.registry = registry;
        this.partitions = partitions;
        this.rollup = rollup;
        this.reconciler = reconciler;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 60_000)
    public void reloadKeys() {
        registry.replace(Set.copyOf(jdbc.queryForList("SELECT key_name FROM valid_keys", String.class)));
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 5_000)
    public void rollup() {
        log.info("sealed until {}", rollup.refreshRecent());
    }

    @Scheduled(cron = "0 5 * * * *") // five past every hour
    public void reconcile() {
        Reconciler.Report r = reconciler.reconcileLastHours(24);
        if (!r.found().isEmpty()) {
            log.warn("reconcile_drift: {} rows drifted, {} left after repair", r.found().size(), r.afterRepair().size());
        }
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 0)
    public void partitions() {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        for (int d = -7; d <= 1; d++) {
            partitions.ensureDay(today.plusDays(d));
        }
        partitions.dropBefore(today.minus(IngestService.RAW_RETENTION));
    }
}
