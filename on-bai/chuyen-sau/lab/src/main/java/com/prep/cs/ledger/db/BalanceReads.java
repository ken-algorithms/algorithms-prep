package com.prep.cs.ledger.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Buoc 5: bon cach TINH so du tu cung mot so cai, khac nhau o do phuc tap va o cau hoi tra loi duoc.
 *   sum        : SUM(amount) moi posting           — O(n), luon dung, dung de doi soat
 *   running    : doc cot balance cua tai khoan     — O(1), hien tai
 *   at(t)      : balance_after cua posting cuoi <= t — O(log n), so du tai thoi diem bat ky
 *   statement  : trang sao ke theo seq (keyset)     — O(log n + trang), khong OFFSET
 */
public final class BalanceReads {

    private final Connection c;

    public BalanceReads(Connection c) {
        this.c = c;
    }

    public long sum(long account) throws SQLException {
        return single("SELECT coalesce(sum(amount), 0) FROM l_posting WHERE account_id = ?", account);
    }

    public long running(long account) throws SQLException {
        return single("SELECT balance FROM l_account WHERE id = ?", account);
    }

    public long at(long account, Instant t) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT balance_after FROM l_posting WHERE account_id = ? AND created_at <= ?
                ORDER BY created_at DESC, seq DESC LIMIT 1""")) {
            ps.setLong(1, account);
            ps.setTimestamp(2, Timestamp.from(t));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    public record Line(long seq, long amount, long balanceAfter) {}

    /** Trang ke tiep sau seq da thay (keyset): moi trang ton nhu nhau, du dang o trang 1 hay trang 10.000. */
    public List<Line> statement(long account, long afterSeq, int limit) throws SQLException {
        List<Line> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT seq, amount, balance_after FROM l_posting WHERE account_id = ? AND seq > ?
                ORDER BY seq LIMIT ?""")) {
            ps.setLong(1, account);
            ps.setLong(2, afterSeq);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Line(rs.getLong(1), rs.getLong(2), rs.getLong(3)));
            }
        }
        return out;
    }

    private long single(String sql, long account) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, account);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
}
