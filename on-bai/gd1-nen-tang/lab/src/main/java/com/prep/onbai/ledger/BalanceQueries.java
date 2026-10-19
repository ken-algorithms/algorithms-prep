package com.prep.onbai.ledger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Duong DOC. Khong khoa gi: moi cau la mot snapshot MVCC nhat quan. */
public final class BalanceQueries {

    /** asOfSeq: so du nay da tinh toi posting seq nao — client giu so nay de doc lai (read-your-writes). */
    public record Balance(long accountId, BigDecimal balance, long asOfSeq) {}

    public record Line(long seq, BigDecimal amount, BigDecimal balanceAfter, String kind, LocalDate effectiveDate) {}

    private final JdbcTemplate jdbc;

    public BalanceQueries(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
    }

    /** O(1): doc dong account. Dung tuyet doi tren primary vi balance cap nhat cung transaction voi posting. */
    public Balance current(long accountId) {
        return current(jdbc, accountId);
    }

    static Balance current(JdbcTemplate db, long accountId) {
        return db.queryForObject("SELECT balance, last_seq FROM account WHERE id = ? AND NOT hot",
                (rs, i) -> new Balance(accountId, rs.getBigDecimal(1), rs.getLong(2)), accountId);
    }

    /** So du ngay sau posting thu seq: O(1) nho balance_after, khong can SUM lai lich su. */
    public BigDecimal asOfSeq(long accountId, long seq) {
        if (seq == 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        return jdbc.queryForObject("SELECT balance_after FROM posting WHERE account_id = ? AND seq = ?",
                BigDecimal.class, accountId, seq);
    }

    /** So du theo NGAY NGHIEP VU: moi but toan co effective_date <= ngay do, ke ca but toan ghi muon (backdated). */
    public BigDecimal asOfDate(long accountId, LocalDate date) {
        return jdbc.queryForObject(
                "SELECT coalesce(sum(amount), 0) FROM posting WHERE account_id = ? AND effective_date <= ?",
                BigDecimal.class, accountId, date);
    }

    /** Sao ke, phan trang keyset theo seq (khong OFFSET): trang nao cung O(limit). */
    public List<Line> statement(long accountId, long afterSeq, int limit) {
        return jdbc.query("""
                SELECT p.seq, p.amount, p.balance_after, j.kind, p.effective_date
                  FROM posting p JOIN journal j ON j.id = p.journal_id
                 WHERE p.account_id = ? AND p.seq > ?
                 ORDER BY p.seq
                 LIMIT ?""",
                (rs, i) -> new Line(rs.getLong(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getString(4),
                        rs.getObject(5, LocalDate.class)),
                accountId, afterSeq, limit);
    }
}
