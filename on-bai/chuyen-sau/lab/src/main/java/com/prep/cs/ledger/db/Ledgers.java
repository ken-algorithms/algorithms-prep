package com.prep.cs.ledger.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;

/**
 * Buoc 2-4 cua bai so du: ba phien ban transfer tren Postgres, moi phien ban sua dung mot loi cua ban truoc.
 *   V1 ReadCheckWrite : SELECT so du, kiem trong Java, UPDATE gia tri tuyet doi  -> mat cap nhat, sinh/mat tien
 *   V2 AtomicUnordered: kiem + tru trong MOT cau UPDATE, nhung khoa theo chieu chuyen -> deadlock 40P01
 *   V3 Final          : khoa theo id tang dan + idempotency key + posting co seq/so du sau
 */
public final class Ledgers {

    private Ledgers() {}

    public record Receipt(long journalId, boolean replayed, boolean rejected) {
        static Receipt rejectedReceipt() {
            return new Receipt(-1, false, true);
        }
    }

    public interface Ledger {
        Receipt transfer(Connection c, String key, long from, long to, long amount) throws SQLException;
    }

    /** V1. Code "binh thuong" nhat — va sai. */
    public static final class ReadCheckWrite implements Ledger {
        @Override
        public Receipt transfer(Connection c, String key, long from, long to, long amount) throws SQLException {
            c.setAutoCommit(false);
            try {
                long bal;
                try (PreparedStatement ps = c.prepareStatement("SELECT balance FROM l_account WHERE id = ?")) {
                    ps.setLong(1, from);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        bal = rs.getLong(1);                         // (1) doc: anh chup luc nay
                    }
                }
                if (bal < amount) {                                    // (2) kiem trong Java
                    c.rollback();
                    return Receipt.rejectedReceipt();
                }
                long j = LedgerDb.insertJournal(c, key, key);
                long fromSeq;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE l_account SET balance = ?, last_seq = last_seq + 1 WHERE id = ? RETURNING last_seq")) {
                    ps.setLong(1, bal - amount);                       // (3) ghi gia tri tinh tu lan doc cu
                    ps.setLong(2, from);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        fromSeq = rs.getLong(1);
                    }
                }
                LedgerDb.insertPosting(c, j, from, fromSeq, -amount, bal - amount);
                LedgerDb.applyLeg(c, j, to, amount);
                c.commit();
                return new Receipt(j, false, false);
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** V2. Kiem va tru trong mot cau (khong con khe doc-ghi), nhung khoa from roi to: hai chieu nguoc nhau -> deadlock. */
    public static final class AtomicUnordered implements Ledger {
        @Override
        public Receipt transfer(Connection c, String key, long from, long to, long amount) throws SQLException {
            c.setAutoCommit(false);
            try {
                long j = LedgerDb.insertJournal(c, key, key);
                if (LedgerDb.applyLeg(c, j, from, -amount) == null) {   // khoa dong 'from'
                    c.rollback();
                    return Receipt.rejectedReceipt();
                }
                LedgerDb.applyLeg(c, j, to, amount);                    // roi moi khoa dong 'to'
                c.commit();
                return new Receipt(j, false, false);
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * V4. Cung logic V3 nhung chay trong ham l_transfer phia Postgres: MOT round trip. Thoi gian giu khoa dong
     * merchant chi con thoi gian ham chay + COMMIT, khong con cho mang giua cac cau.
     */
    public static final class ServerSide implements Ledger {
        @Override
        public Receipt transfer(Connection c, String key, long from, long to, long amount) throws SQLException {
            c.setAutoCommit(true);
            try (PreparedStatement ps = c.prepareStatement("SELECT l_transfer(?, ?, ?, ?)")) {
                ps.setString(1, key);
                ps.setLong(2, from);
                ps.setLong(3, to);
                ps.setLong(4, amount);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return new Receipt(rs.getLong(1), false, false);
                }
            } catch (SQLException e) {
                if ("P0001".equals(e.getSQLState()) && e.getMessage().contains("khong du tien")) {
                    return Receipt.rejectedReceipt();
                }
                throw e;
            }
        }
    }

    /** V3. Ban dung: gianh key, khoa theo id tang dan, kiem + tru + cap seq trong mot cau. */
    public static final class Final implements Ledger {
        @Override
        public Receipt transfer(Connection c, String key, long from, long to, long amount) throws SQLException {
            c.setAutoCommit(false);
            try {
                Receipt r = transferInTx(c, key, from, to, amount);
                if (r.rejected() || r.replayed()) c.rollback();   // khong co gi moi de ghi
                else c.commit();
                return r;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }

        /** Chay TRONG transaction cua nguoi goi (vi du capture mot hold), khong commit/rollback o day. */
        public Receipt transferInTx(Connection c, String key, long from, long to, long amount) throws SQLException {
            if (from == to || amount <= 0) throw new IllegalArgumentException("lenh khong hop le");
            String hash = from + ">" + to + ":" + amount;              // noi dung da chuan hoa cua lenh
            TreeMap<Long, Long> legs = new TreeMap<>();                // id tang dan = thu tu khoa
            legs.put(from, -amount);
            legs.put(to, amount);
            Long j;
            // (1) gianh key: request trung den cung luc se CHO o unique index toi khi ben kia commit/rollback
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO l_journal (idem_key, request_hash) VALUES (?, ?)
                    ON CONFLICT (idem_key) DO NOTHING RETURNING id""")) {
                ps.setString(1, key);
                ps.setString(2, hash);
                try (ResultSet rs = ps.executeQuery()) {
                    j = rs.next() ? rs.getLong(1) : null;
                }
            }
            if (j == null) return replay(c, key, hash);               // key da co: lenh nay chay roi
            // (2) tung chan theo id tang dan: khoa + kiem du tien + cap seq + doi so du, mot cau UPDATE
            for (var leg : legs.entrySet()) {
                if (LedgerDb.applyLeg(c, j, leg.getKey(), leg.getValue()) == null) {
                    return Receipt.rejectedReceipt();                  // nguoi goi rollback: bo ca journal, key tu do
                }
            }
            return new Receipt(j, false, false);
        }

        private Receipt replay(Connection c, String key, String hash) throws SQLException {
            try (PreparedStatement ps = c.prepareStatement("SELECT id, request_hash FROM l_journal WHERE idem_key = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (!rs.getString(2).equals(hash)) {
                        throw new IllegalStateException("idempotency key " + key + " da dung cho mot lenh KHAC");
                    }
                    return new Receipt(rs.getLong(1), true, false);
                }
            }
        }
    }
}
