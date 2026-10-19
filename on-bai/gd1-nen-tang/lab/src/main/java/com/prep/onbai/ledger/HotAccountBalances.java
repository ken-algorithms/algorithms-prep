package com.prep.onbai.ledger;

import java.math.BigDecimal;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * So du account NONG (cash, thu nhap lai...): moi giao dich deu cham no, nen ghi khong khoa no.
 * Hau qua: posting.id cua no cap theo thu tu INSERT, con commit thi theo thu tu khac.
 * <pre>
 *  T1: INSERT posting id=101 ........................................ COMMIT (muon)
 *  T2:            INSERT posting id=102  COMMIT
 *  tong hop:                                    max(id)=102, moc=102 -> 101 nam DUOI moc, bo sot VINH VIEN
 * </pre>
 * Cach dung: chot theo moc transaction. pg_snapshot_xmin = xid nho nhat con dang chay; moi transaction
 * co xid nho hon da KET THUC (commit hoac rollback) -> khong con ai commit muon duoi moc do.
 */
public final class HotAccountBalances {

    private final JdbcTemplate jdbc;

    public HotAccountBalances(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
    }

    /** DUNG: cong cac posting cua transaction co xid trong [moc cu, xmin hien tai), roi dat moc moi = xmin. */
    public void refresh(long accountId) {
        jdbc.update("""
                WITH h AS (SELECT pg_snapshot_xmin(pg_current_snapshot()) AS x),
                     old AS (SELECT balance, up_to_xid FROM hot_balance WHERE account_id = ?),
                     d AS (SELECT coalesce(sum(p.amount), 0) AS delta
                             FROM posting p, h
                            WHERE p.account_id = ? AND p.seq IS NULL
                              AND p.tx_id < h.x
                              AND p.tx_id >= coalesce((SELECT up_to_xid FROM old), '0'::xid8))
                INSERT INTO hot_balance AS hb (account_id, balance, up_to_xid)
                SELECT ?, coalesce((SELECT balance FROM old), 0) + d.delta, h.x FROM d, h
                ON CONFLICT (account_id) DO UPDATE
                   SET balance = EXCLUDED.balance, up_to_xid = EXCLUDED.up_to_xid, refreshed_at = clock_timestamp()
                 WHERE hb.up_to_xid < EXCLUDED.up_to_xid""", accountId, accountId, accountId);
    }

    /** Snapshot + phan duoi (cac transaction tu moc tro len da commit): dung tai moi thoi diem doc. */
    public BigDecimal read(long accountId) {
        return jdbc.queryForObject("""
                SELECT coalesce(max(hb.balance), 0) + coalesce(sum(p.amount), 0)
                  FROM (SELECT ?::bigint AS id) a
                  LEFT JOIN hot_balance hb ON hb.account_id = a.id
                  LEFT JOIN posting p ON p.account_id = a.id AND p.seq IS NULL
                       AND p.tx_id >= coalesce(hb.up_to_xid, '0'::xid8)""",
                BigDecimal.class, accountId);
    }

    /** SAI (de doi chieu): moc = max(posting.id) da thay. */
    public void refreshByIdNaive(long accountId) {
        jdbc.update("""
                WITH old AS (SELECT balance, up_to_posting FROM hot_balance_by_id WHERE account_id = ?),
                     d AS (SELECT coalesce(sum(amount), 0) AS delta, max(id) AS m
                             FROM posting
                            WHERE account_id = ? AND id > coalesce((SELECT up_to_posting FROM old), 0))
                INSERT INTO hot_balance_by_id AS hb (account_id, balance, up_to_posting)
                SELECT ?, coalesce((SELECT balance FROM old), 0) + d.delta,
                       coalesce(d.m, (SELECT up_to_posting FROM old), 0)
                  FROM d
                ON CONFLICT (account_id) DO UPDATE
                   SET balance = EXCLUDED.balance, up_to_posting = EXCLUDED.up_to_posting
                 WHERE hb.up_to_posting < EXCLUDED.up_to_posting""", accountId, accountId, accountId);
    }

    public BigDecimal readByIdNaive(long accountId) {
        return jdbc.queryForObject("""
                SELECT coalesce(max(hb.balance), 0) + coalesce(sum(p.amount), 0)
                  FROM (SELECT ?::bigint AS id) a
                  LEFT JOIN hot_balance_by_id hb ON hb.account_id = a.id
                  LEFT JOIN posting p ON p.account_id = a.id AND p.id > coalesce(hb.up_to_posting, 0)""",
                BigDecimal.class, accountId);
    }

    /** Su that = SUM toan bo lich su. Chi dung de doi soat, khong dung tren duong doc nong. */
    public BigDecimal truth(long accountId) {
        return jdbc.queryForObject("SELECT coalesce(sum(amount), 0) FROM posting WHERE account_id = ?",
                BigDecimal.class, accountId);
    }
}
