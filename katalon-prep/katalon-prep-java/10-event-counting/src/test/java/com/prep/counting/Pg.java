package com.prep.counting;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/** Real PostgreSQL 16, started once per test run as a subprocess (no Docker). Each test class gets its own database. */
final class Pg {

    private static EmbeddedPostgres pg;
    private static final java.util.Map<String, HikariDataSource> POOLS = new java.util.HashMap<>();

    private Pg() {}

    static synchronized String freshDatabaseUrl(String name) {
        HikariDataSource old = POOLS.remove(name);
        if (old != null) {
            old.close(); // the previous test's pool on this database
        }
        try {
            if (pg == null) {
                pg = EmbeddedPostgres.builder().setServerConfig("max_connections", "100").start();
            }
            try (Connection c = pg.getPostgresDatabase().getConnection(); Statement s = c.createStatement()) {
                s.execute("DROP DATABASE IF EXISTS " + name);
                s.execute("CREATE DATABASE " + name);
            }
            return pg.getJdbcUrl("postgres", name);
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("cannot start embedded postgres", e);
        }
    }

    /** A pooled data source (like the app's Hikari pool) with schema.sql applied. */
    static synchronized DataSource freshDatabase(String name) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(freshDatabaseUrl(name));
        cfg.setUsername("postgres");
        cfg.setMaximumPoolSize(16);
        cfg.setMinimumIdle(1);
        HikariDataSource ds = new HikariDataSource(cfg);
        POOLS.put(name, ds);
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(ds);
        return ds;
    }
}
