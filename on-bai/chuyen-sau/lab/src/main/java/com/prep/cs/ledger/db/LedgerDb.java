package com.prep.cs.ledger.db;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

/**
 * Ha tang chung cho moi phien ban so cai: tao bang, xoa sach, mo tai khoan bang mot but toan "nap dau ky" tu
 * tai khoan he thong 0 (duoc am) — de ngay tu dau, so du = tong posting, doi soat duoc.
 */
public final class LedgerDb {

    public static final long SYSTEM = 0;
    final DataSource ds;

    public LedgerDb(DataSource ds) {
        this.ds = ds;
    }

    public Connection connect() throws SQLException {
        return ds.getConnection();
    }

    public void reset() throws Exception {
        try (InputStream in = LedgerDb.class.getResourceAsStream("/ledger_cs.sql");
             Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            st.execute("TRUNCATE l_hold, l_posting, l_journal, l_account RESTART IDENTITY CASCADE");
            st.execute("INSERT INTO l_account (id, allow_negative) VALUES (0, true)");
        }
    }

    /** Mo tai khoan va nap so du dau ky (mot journal: he thong -opening, tai khoan +opening). */
    public void open(long id, long opening) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO l_account (id) VALUES (?)")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            if (opening > 0) {
                long j = insertJournal(c, "open:" + id, "open");
                applyLeg(c, j, SYSTEM, -opening);
                applyLeg(c, j, id, opening);
            }
            c.commit();
        }
    }

    static long insertJournal(Connection c, String key, String hash) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO l_journal (idem_key, request_hash) VALUES (?, ?) RETURNING id")) {
            ps.setString(1, key);
            ps.setString(2, hash);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /**
     * Mot "chan" cua but toan, ban dung: MOT cau UPDATE vua khoa dong, vua kiem du tien, vua cap seq, vua doi so
     * du chay; roi ghi posting voi seq/so du vua nhan. Tra so du sau, hoac null neu khong du tien.
     */
    static Long applyLeg(Connection c, long journalId, long account, long amount) throws SQLException {
        long seq, after;
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE l_account SET balance = balance + ?, last_seq = last_seq + 1
                 WHERE id = ? AND (allow_negative OR balance - held + ? >= 0)
                RETURNING last_seq, balance""")) {
            ps.setLong(1, amount);
            ps.setLong(2, account);
            ps.setLong(3, amount);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                seq = rs.getLong(1);
                after = rs.getLong(2);
            }
        }
        insertPosting(c, journalId, account, seq, amount, after);
        return after;
    }

    static void insertPosting(Connection c, long journalId, long account, long seq, long amount, long after)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO l_posting (journal_id, account_id, seq, amount, balance_after) VALUES (?, ?, ?, ?, ?)""")) {
            ps.setLong(1, journalId);
            ps.setLong(2, account);
            ps.setLong(3, seq);
            ps.setLong(4, amount);
            ps.setLong(5, after);
            ps.executeUpdate();
        }
    }

    public long balance(long account) throws SQLException {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT balance FROM l_account WHERE id = ?")) {
            ps.setLong(1, account);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /** Doi soat: tra danh sach vi pham, rong = so cai lanh. */
    public java.util.List<String> reconcile() throws SQLException {
        String[][] checks = {
                {"journal khong can bang", "SELECT journal_id FROM l_posting GROUP BY journal_id HAVING sum(amount) <> 0"},
                {"balance <> tong posting", """
                        SELECT a.id FROM l_account a LEFT JOIN l_posting p ON p.account_id = a.id
                        GROUP BY a.id, a.balance HAVING a.balance <> coalesce(sum(p.amount), 0)"""},
                {"seq bi ho hoac last_seq sai", """
                        SELECT a.id FROM l_account a LEFT JOIN l_posting p ON p.account_id = a.id
                        GROUP BY a.id, a.last_seq HAVING a.last_seq <> count(p.id) OR a.last_seq <> coalesce(max(p.seq), 0)"""},
                {"chuoi balance_after bi dut", """
                        SELECT id FROM (SELECT id, balance_after - amount AS before,
                               lag(balance_after, 1, 0::bigint) OVER (PARTITION BY account_id ORDER BY seq) AS prev
                          FROM l_posting) x WHERE before <> prev"""},
                {"held <> tong hold ACTIVE", """
                        SELECT a.id FROM l_account a LEFT JOIN l_hold h ON h.account_id = a.id AND h.status = 'ACTIVE'
                        GROUP BY a.id, a.held HAVING a.held <> coalesce(sum(h.amount), 0)"""}};
        java.util.List<String> bad = new java.util.ArrayList<>();
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            for (String[] ck : checks) {
                try (ResultSet rs = st.executeQuery(ck[1])) {
                    while (rs.next()) bad.add(ck[0] + ": " + rs.getLong(1));
                }
            }
        }
        return bad;
    }
}
