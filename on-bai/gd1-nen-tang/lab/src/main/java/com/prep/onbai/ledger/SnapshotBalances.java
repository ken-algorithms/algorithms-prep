package com.prep.onbai.ledger;

import java.math.BigDecimal;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Phuong an B: khong doc cot balance nong ma doc snapshot + phan duoi, chot theo SEQ cua account.
 * <p>
 * Vi sao chot theo seq la an toan: seq cap trong UPDATE account (giu khoa dong toi commit), nen
 * transaction sau khong lay duoc seq k+1 khi seq k chua commit -> cac seq da commit luon la
 * mot DAY LIEN TUC 1..k. Doc max(seq) bat ky luc nao cung khong bo sot ai. (posting.id toan cuc
 * KHONG co tinh chat nay — xem HotAccountBalances.)
 */
public final class SnapshotBalances {

    /** tailRows: so posting phai cong them = chi phi cua mot lan doc, bi chan tren boi tan suat refresh. */
    public record Read(BigDecimal balance, long asOfSeq, long tailRows) {}

    private final JdbcTemplate jdbc;

    public SnapshotBalances(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
    }

    /** Cong don phan duoi vao snapshot. Chay song song bao nhieu ban cung duoc: moc chi tien, khong lui. */
    public void refresh(long accountId) {
        jdbc.update("""
                INSERT INTO balance_snapshot AS s (account_id, balance, up_to_seq)
                SELECT a.id,
                       coalesce(old.balance, 0) + coalesce(sum(p.amount), 0),
                       coalesce(max(p.seq), old.up_to_seq, 0)
                  FROM account a
                  LEFT JOIN balance_snapshot old ON old.account_id = a.id
                  LEFT JOIN posting p ON p.account_id = a.id AND p.seq > coalesce(old.up_to_seq, 0)
                 WHERE a.id = ? AND NOT a.hot
                 GROUP BY a.id, old.balance, old.up_to_seq
                ON CONFLICT (account_id) DO UPDATE
                   SET balance = EXCLUDED.balance, up_to_seq = EXCLUDED.up_to_seq, refreshed_at = clock_timestamp()
                 WHERE s.up_to_seq < EXCLUDED.up_to_seq""", accountId);
    }

    /** Mot cau lenh = mot snapshot MVCC: snapshot va duoi luon khop nhau. */
    public Read read(long accountId) {
        return jdbc.queryForObject("""
                SELECT coalesce(s.balance, 0) + coalesce(sum(p.amount), 0),
                       greatest(coalesce(s.up_to_seq, 0), coalesce(max(p.seq), 0)),
                       count(p.id)
                  FROM account a
                  LEFT JOIN balance_snapshot s ON s.account_id = a.id
                  LEFT JOIN posting p ON p.account_id = a.id AND p.seq > coalesce(s.up_to_seq, 0)
                 WHERE a.id = ?
                 GROUP BY s.balance, s.up_to_seq""",
                (rs, i) -> new Read(rs.getBigDecimal(1), rs.getLong(2), rs.getLong(3)), accountId);
    }
}
