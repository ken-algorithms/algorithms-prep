package com.prep.counting;

import com.prep.counting.domain.EventClassifier;
import com.prep.counting.domain.KeyRegistry;
import com.prep.counting.ingest.IngestService;
import com.prep.counting.ingest.PartitionManager;
import com.prep.counting.query.StatsService;
import com.prep.counting.rollup.Reconciler;
import com.prep.counting.rollup.RollupJob;
import java.time.Clock;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** Plain constructors, wired here: the classes stay free of Spring so the tests can build them directly. */
@Configuration
public class CountingConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    KeyRegistry keyRegistry(JdbcTemplate jdbc) {
        return new KeyRegistry(Set.copyOf(jdbc.queryForList("SELECT key_name FROM valid_keys", String.class)));
    }

    @Bean
    EventClassifier eventClassifier(KeyRegistry registry) {
        return new EventClassifier(registry);
    }

    @Bean
    PartitionManager partitionManager(JdbcTemplate jdbc) {
        return new PartitionManager(jdbc);
    }

    @Bean
    IngestService ingestService(JdbcTemplate jdbc, EventClassifier classifier, PartitionManager partitions, Clock clock) {
        return new IngestService(jdbc, classifier, partitions, clock);
    }

    @Bean
    RollupJob rollupJob(JdbcTemplate jdbc, TransactionTemplate tx, Clock clock) {
        return new RollupJob(jdbc, tx, clock);
    }

    @Bean
    Reconciler reconciler(JdbcTemplate jdbc, RollupJob rollup) {
        return new Reconciler(jdbc, rollup);
    }

    @Bean
    StatsService statsService(JdbcTemplate jdbc, RollupJob rollup) {
        return new StatsService(jdbc, rollup);
    }
}
