package com.prep.quarkus;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.util.Map;

/**
 * ============================================================================
 * POSTGRES THAT TRONG TEST — KHONG CAN DOCKER.
 * ============================================================================
 *
 * <p>Quarkus co "Dev Services" tu bat Postgres container khi test — nhung no CAN DOCKER.
 * May nay khong co Docker, nen ta thay bang embedded-postgres: binary PostgreSQL 16.4 that
 * duoc dong goi trong artifact Maven, chay nhu subprocess, tu dung khi test xong.
 *
 * <p>{@link QuarkusTestResourceLifecycleManager} la co che chuan cua Quarkus de khoi dong ha tang
 * truoc khi ung dung start, va GHI DE cau hinh ({@code start()} tra ve map config override).
 *
 * <p>Vi sao van dung Postgres that chu khong dung H2: H2 khong co {@code percentile_disc},
 * khong co partial index, khong co {@code jsonb}, va {@code EXPLAIN} hoan toan khac. Test tren H2
 * roi deploy len Postgres la cach chac chan nhat de bug lot len production.
 */
public class EmbeddedPostgresResource implements QuarkusTestResourceLifecycleManager {

    private EmbeddedPostgres postgres;

    @Override
    public Map<String, String> start() {
        try {
            postgres = EmbeddedPostgres.builder()
                    .setServerConfig("fsync", "off")            // test khong can ben vung
                    .setServerConfig("synchronous_commit", "off")
                    .setServerConfig("full_page_writes", "off")
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("cannot start embedded postgres", e);
        }
        String jdbcUrl = "jdbc:postgresql://localhost:%d/postgres".formatted(postgres.getPort());
        return Map.of(
                "quarkus.datasource.jdbc.url", jdbcUrl,
                "quarkus.datasource.username", "postgres",
                "quarkus.datasource.password", "postgres");
    }

    @Override
    public void stop() {
        if (postgres != null) {
            try {
                postgres.close();
            } catch (IOException ignored) {
                // dang tat test, khong con gi de lam
            }
        }
    }
}
