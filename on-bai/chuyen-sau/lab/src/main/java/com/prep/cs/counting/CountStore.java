package com.prep.cs.counting;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;

/**
 * Kho so dem tren Postgres. Hai thao tac quan trong:
 *   commit(): trong MOT transaction — doi offset Kafka (co dieu kien, chan xac song) + cong delta vao cac o.
 *   query():  phan ra [from, to) thanh it bucket nhat roi SUM, so dong doc khong phu thuoc so event.
 */
public final class CountStore {

    public record Cell(Tier tier, long bucket) implements Comparable<Cell> {
        @Override
        public int compareTo(Cell o) {         // thu tu khoa dong: tang roi bucket — moi transaction cung thu tu
            int c = Integer.compare(tier.ordinal(), o.tier.ordinal());
            return c != 0 ? c : Long.compare(bucket, o.bucket);
        }
    }

    private static final String UPSERT = """
            INSERT INTO count_cell (tier, bucket, t, f, fk, mal) VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (tier, bucket) DO UPDATE SET t = count_cell.t + EXCLUDED.t, f = count_cell.f + EXCLUDED.f,
                fk = count_cell.fk + EXCLUDED.fk, mal = count_cell.mal + EXCLUDED.mal""";

    private final DataSource ds;

    public CountStore(DataSource ds) {
        this.ds = ds;
    }

    public static DataSource dataSource(String url, String user, String password) {
        PGSimpleDataSource d = new PGSimpleDataSource();
        d.setUrl(url);
        d.setUser(user);
        d.setPassword(password);
        return d;
    }

    public Connection connect() throws SQLException {
        return ds.getConnection();
    }

    public void createSchema() throws SQLException, IOException {
        try (InputStream in = CountStore.class.getResourceAsStream("/counting.sql");
             Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public void truncate() throws SQLException {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute("TRUNCATE count_cell, agg_offset");
        }
    }

    /** Offset ke tiep phai doc cua partition; lan dau thay partition thi la 0. */
    public long loadOffset(Connection c, String topic, int part) throws SQLException {
        try (PreparedStatement ins = c.prepareStatement(
                "INSERT INTO agg_offset (topic, part, next_offset) VALUES (?, ?, 0) ON CONFLICT DO NOTHING")) {
            ins.setString(1, topic);
            ins.setInt(2, part);
            ins.executeUpdate();
        }
        try (PreparedStatement q = c.prepareStatement("SELECT next_offset FROM agg_offset WHERE topic = ? AND part = ?")) {
            q.setString(1, topic);
            q.setInt(2, part);
            try (ResultSet rs = q.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * Mot transaction: (1) doi offset tu expected sang next — 0 dong nghia la co aggregator khac da commit
     * partition nay (minh la xac song) -> rollback, tra false; (2) cong delta vao tung o, theo thu tu Cell.
     */
    public boolean commit(Connection c, String topic, int part, long expected, long next,
                          SortedMap<Cell, long[]> deltas) throws SQLException {
        boolean auto = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE agg_offset SET next_offset = ? WHERE topic = ? AND part = ? AND next_offset = ?")) {
                ps.setLong(1, next);
                ps.setString(2, topic);
                ps.setInt(3, part);
                ps.setLong(4, expected);
                if (ps.executeUpdate() != 1) {
                    c.rollback();
                    return false;
                }
            }
            if (!deltas.isEmpty()) {
                try (PreparedStatement ps = c.prepareStatement(UPSERT)) {
                    for (Map.Entry<Cell, long[]> e : deltas.entrySet()) {
                        long[] d = e.getValue();
                        ps.setShort(1, (short) e.getKey().tier().ordinal());
                        ps.setLong(2, e.getKey().bucket());
                        for (int i = 0; i < 4; i++) ps.setLong(3 + i, d[i]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
            c.commit();
            return true;
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(auto);
        }
    }

    public Stats query(long fromSec, long toSec) throws SQLException {
        List<RangeDecomposer.Range> ranges = RangeDecomposer.decompose(fromSec, toSec);
        if (ranges.isEmpty()) return new Stats(0, 0, 0, 0, 0);
        StringBuilder sql = new StringBuilder(
                "SELECT coalesce(sum(t),0), coalesce(sum(f),0), coalesce(sum(fk),0), coalesce(sum(mal),0), count(*) "
                        + "FROM count_cell WHERE ");
        for (int i = 0; i < ranges.size(); i++) {
            sql.append(i == 0 ? "" : " OR ").append("(tier = ? AND bucket >= ? AND bucket < ?)");
        }
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            for (RangeDecomposer.Range r : ranges) {
                ps.setShort(i++, (short) r.tier().ordinal());
                ps.setLong(i++, r.fromBucket());
                ps.setLong(i++, r.toBucket());
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new Stats(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5));
            }
        }
    }
}
