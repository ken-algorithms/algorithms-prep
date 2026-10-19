package com.prep.cs.ledger.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Buoc 6: so du kha dung = so du so cai - tien dang tam giu. Luong the/vi: authorize (giu tien, chua chuyen) ->
 * capture (chuyen that, nha giu) hoac release (nha giu). Cot held tren tai khoan doi CUNG cau voi kiem tra.
 */
public final class Holds {

    private final Ledgers.Final ledger = new Ledgers.Final();

    /** Giu amount neu kha dung du. Tra id hold, hoac -1 neu khong du. */
    public long authorize(Connection c, long account, long amount, int ttlSeconds) throws SQLException {
        c.setAutoCommit(false);
        try {
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE l_account SET held = held + ?
                     WHERE id = ? AND balance - held - ? >= 0 RETURNING held""")) {
                ps.setLong(1, amount);
                ps.setLong(2, account);
                ps.setLong(3, amount);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        return -1;
                    }
                }
            }
            long id;
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO l_hold (account_id, amount, expires_at)
                    VALUES (?, ?, clock_timestamp() + make_interval(secs => ?)) RETURNING id""")) {
                ps.setLong(1, account);
                ps.setLong(2, amount);
                ps.setInt(3, ttlSeconds);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    id = rs.getLong(1);
                }
            }
            c.commit();
            return id;
        } catch (SQLException e) {
            c.rollback();
            throw e;
        }
    }

    /** Chuyen that so tien da giu sang 'to', trong CUNG transaction voi viec nha giu. false neu hold khong con ACTIVE. */
    public boolean capture(Connection c, long holdId, long to) throws SQLException {
        c.setAutoCommit(false);
        try {
            long account, amount;
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE l_hold SET status = 'CAPTURED' WHERE id = ? AND status = 'ACTIVE'
                    RETURNING account_id, amount""")) {
                ps.setLong(1, holdId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        return false;
                    }
                    account = rs.getLong(1);
                    amount = rs.getLong(2);
                }
            }
            unhold(c, account, amount);
            // transfer tham gia transaction dang mo: neu no rollback thi hold cung quay ve ACTIVE
            Ledgers.Receipt r = ledger.transferInTx(c, "capture:" + holdId, account, to, amount);
            if (r.rejected()) {
                c.rollback();
                return false;
            }
            c.commit();
            return true;
        } catch (SQLException e) {
            c.rollback();
            throw e;
        }
    }

    public boolean release(Connection c, long holdId) throws SQLException {
        c.setAutoCommit(false);
        try {
            try (PreparedStatement ps = c.prepareStatement("""
                    UPDATE l_hold SET status = 'RELEASED' WHERE id = ? AND status = 'ACTIVE'
                    RETURNING account_id, amount""")) {
                ps.setLong(1, holdId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        c.rollback();
                        return false;
                    }
                    unhold(c, rs.getLong(1), rs.getLong(2));
                }
            }
            c.commit();
            return true;
        } catch (SQLException e) {
            c.rollback();
            throw e;
        }
    }

    private static void unhold(Connection c, long account, long amount) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE l_account SET held = held - ? WHERE id = ?")) {
            ps.setLong(1, amount);
            ps.setLong(2, account);
            ps.executeUpdate();
        }
    }

    public static long available(Connection c, long account) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT balance - held FROM l_account WHERE id = ?")) {
            ps.setLong(1, account);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
