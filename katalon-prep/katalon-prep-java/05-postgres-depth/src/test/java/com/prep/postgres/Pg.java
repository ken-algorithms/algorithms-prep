package com.prep.postgres;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;

/**
 * PostgreSQL 16 THAT, chay nhu subprocess. KHONG can Docker, KHONG can brew install.
 *
 * <p>Binary Postgres duoc dong goi trong artifact Maven, giai nen vao temp dir, chay bang
 * {@code ProcessBuilder}, va bi dung khi JVM thoat. Sau khi test xong khong con gi tren may.
 *
 * <p><b>Khoi dong MOT LAN cho ca test suite</b> (mat ~10s), khong phai moi test class. Moi lab dung
 * schema rieng nen khong dinh nhau. Neu khoi dong lai moi class thi 5 lab = 50s cho khong.
 */
final class Pg {

    private static EmbeddedPostgres instance;

    private Pg() {}

    static synchronized DataSource dataSource() {
        if (instance == null) {
            try {
                instance = EmbeddedPostgres.builder()
                        // Tuning de plan on dinh va giong production hon default cua embedded pg.
                        .setServerConfig("shared_buffers", "128MB")
                        .setServerConfig("work_mem", "16MB")
                        .setServerConfig("random_page_cost", "1.1") // gia dinh SSD, giong Aurora
                        .setServerConfig("max_connections", "50")
                        .start();
            } catch (java.io.IOException e) {
                throw new IllegalStateException("cannot start embedded postgres", e);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(Pg::stop, "pg-shutdown"));
        }
        return instance.getPostgresDatabase();
    }

    private static synchronized void stop() {
        if (instance != null) {
            try {
                instance.close();
            } catch (java.io.IOException ignored) {
                // dang tat JVM, khong con gi de lam
            }
            instance = null;
        }
    }

    static Connection connection() throws SQLException {
        return dataSource().getConnection();
    }

    /** Chay nhieu lenh DDL/DML. Dung cho setup. */
    static void exec(String... statements) {
        try (Connection conn = connection();
                Statement st = conn.createStatement()) {
            for (String sql : statements) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("SQL failed: " + e.getMessage(), e);
        }
    }

    /** Doc mot gia tri scalar. */
    static <T> T scalar(String sql, Class<T> type) {
        try (Connection conn = connection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) {
                throw new IllegalStateException("no rows for: " + sql);
            }
            return rs.getObject(1, type);
        } catch (SQLException e) {
            throw new IllegalStateException("SQL failed: " + e.getMessage(), e);
        }
    }

    static long count(String table) {
        return scalar("SELECT count(*) FROM " + table, Long.class);
    }

    /**
     * Chay {@code EXPLAIN (ANALYZE, BUFFERS)} va tra ve query plan dang text.
     *
     * <p>Day la ky nang duoc hoi nhieu nhat khi phong van ve Postgres, va la thu file test nay
     * bien thanh assert duoc: thay vi "toi biet doc EXPLAIN", ban co test CHUNG MINH index da doi
     * plan tu Seq Scan sang Index Scan.
     */
    static String explain(String sql) {
        return explainWith("ANALYZE, BUFFERS", sql);
    }

    /** EXPLAIN khong ANALYZE — chi lay plan uoc luong, khong chay that. */
    static String explainPlanOnly(String sql) {
        return explainWith("", sql);
    }

    private static String explainWith(String options, String sql) {
        String prefix = options.isBlank() ? "EXPLAIN " : "EXPLAIN (" + options + ") ";
        try (Connection conn = connection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(prefix + sql)) {
            var lines = new ArrayList<String>();
            while (rs.next()) {
                lines.add(rs.getString(1));
            }
            return String.join("\n", lines);
        } catch (SQLException e) {
            throw new IllegalStateException("EXPLAIN failed: " + e.getMessage(), e);
        }
    }

    /** Doc mot cot thanh list String — dung de kiem tra danh sach partition bi quet. */
    static List<String> column(String sql) {
        try (Connection conn = connection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            var out = new ArrayList<String>();
            while (rs.next()) {
                out.add(rs.getString(1));
            }
            return out;
        } catch (SQLException e) {
            throw new IllegalStateException("SQL failed: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------
    // Helper doc ket qua EXPLAIN
    // ------------------------------------------------------------------

    static boolean usesSeqScan(String plan) {
        return plan.contains("Seq Scan");
    }

    /** Index Scan HOAC Index Only Scan HOAC Bitmap Index Scan — tuc la co dung index. */
    static boolean usesIndex(String plan) {
        return plan.contains("Index Scan") || plan.contains("Index Only Scan") || plan.contains("Bitmap Index Scan");
    }

    /** Index Only Scan = khong cham heap ca. Dau hieu covering index dang phat huy. */
    static boolean usesIndexOnlyScan(String plan) {
        return plan.contains("Index Only Scan");
    }

    /** Ten cac index xuat hien trong plan. */
    static List<String> indexesUsed(String plan) {
        return plan.lines()
                .filter(line -> line.contains("Index Scan") || line.contains("Bitmap Index Scan")
                        || line.contains("Index Only Scan"))
                .map(line -> {
                    int i = line.indexOf("using ");
                    if (i < 0) {
                        return "";
                    }
                    String rest = line.substring(i + "using ".length());
                    int space = rest.indexOf(' ');
                    return space < 0 ? rest : rest.substring(0, space);
                })
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();
        }

    /** Lay so "actual time=... rows=N" cua node dau tien. */
    static long actualRows(String plan) {
        return plan.lines()
                .filter(line -> line.contains("actual time="))
                .findFirst()
                .map(line -> {
                    int i = line.indexOf("rows=", line.indexOf("actual time="));
                    if (i < 0) {
                        return -1L;
                    }
                    String rest = line.substring(i + "rows=".length());
                    var digits = rest.chars().takeWhile(Character::isDigit)
                            .mapToObj(c -> String.valueOf((char) c))
                            .collect(Collectors.joining());
                    return digits.isEmpty() ? -1L : Long.parseLong(digits);
                })
                .orElse(-1L);
    }

    /** Kich thuoc bang (khong tinh index, khong tinh TOAST) tinh theo byte. */
    static long tableSizeBytes(String table) {
        return scalar("SELECT pg_relation_size('" + table + "')", Long.class);
    }

    /** Tong kich thuoc moi index cua bang. */
    static long indexesSizeBytes(String table) {
        return scalar("SELECT pg_indexes_size('" + table + "')", Long.class);
    }

    static long indexSizeBytes(String index) {
        return scalar("SELECT pg_relation_size('" + index + "')", Long.class);
    }

    static String humanSize(long bytes) {
        return scalar("SELECT pg_size_pretty(" + bytes + "::bigint)", String.class);
    }
}
