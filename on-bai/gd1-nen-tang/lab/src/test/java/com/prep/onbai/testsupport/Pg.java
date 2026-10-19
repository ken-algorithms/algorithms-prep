package com.prep.onbai.testsupport;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * Postgres 16 THAT, khoi dong mot lan cho ca lan chay test (process con, khong Docker).
 * Moi test class xin mot database rieng theo ten, chay cac file schema truyen vao.
 */
public final class Pg {

    private static EmbeddedPostgres pg;
    private static final Map<String, HikariDataSource> POOLS = new HashMap<>();

    private Pg() {}

    /** Database moi tinh (DROP + CREATE), tra ve JDBC URL. Pool cu cung ten bi dong truoc. */
    public static synchronized String freshDatabaseUrl(String name) {
        HikariDataSource old = POOLS.remove(name);
        if (old != null) {
            old.close();
        }
        try {
            if (pg == null) {
                pg = EmbeddedPostgres.builder()
                        .setServerConfig("max_connections", "200")
                        .setServerConfig("max_pred_locks_per_transaction", "256")
                        .start();
            }
            try (Connection c = pg.getPostgresDatabase().getConnection(); Statement s = c.createStatement()) {
                s.execute("DROP DATABASE IF EXISTS " + name + " WITH (FORCE)");
                s.execute("CREATE DATABASE " + name);
            }
            return pg.getJdbcUrl("postgres", name);
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("khong khoi dong duoc embedded postgres", e);
        }
    }

    /** Pool Hikari (giong app that) tren database moi, da chay cac file schema trong classpath. */
    public static synchronized DataSource freshDatabase(String name, int poolSize, String... schemaFiles) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(freshDatabaseUrl(name));
        cfg.setUsername("postgres");
        cfg.setMaximumPoolSize(poolSize);
        cfg.setMinimumIdle(1);
        cfg.setPoolName("pool-" + name);
        HikariDataSource ds = new HikariDataSource(cfg);
        POOLS.put(name, ds);
        if (schemaFiles.length > 0) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.setSeparator(ScriptUtils.EOF_STATEMENT_SEPARATOR); // ca file mot lan: giu nguyen ham plpgsql $$...$$
            for (String f : schemaFiles) {
                populator.addScript(new ClassPathResource(f));
            }
            populator.execute(ds);
        }
        return ds;
    }

    public static DataSource freshDatabase(String name, String... schemaFiles) {
        return freshDatabase(name, 16, schemaFiles);
    }
}
