package com.prep.onbai.ledger;

import com.prep.onbai.ledger.Ledger.Applied;
import com.prep.onbai.ledger.Ledger.InsufficientFunds;
import com.prep.onbai.ledger.Ledger.InvalidJournal;
import com.prep.onbai.ledger.Ledger.KeyReuse;
import com.prep.onbai.ledger.Ledger.Leg;
import com.prep.onbai.ledger.Ledger.Receipt;
import com.prep.onbai.ledger.Ledger.Request;
import com.prep.onbai.ledger.Ledger.UnknownAccount;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Duong GHI cua so cai. Moi but toan la MOT transaction:
 * <pre>
 *  1. INSERT journal ... ON CONFLICT (idempotency_key) DO NOTHING   -> gianh key, trung thi tra ket qua cu
 *  2. voi tung account theo id TANG DAN:
 *       UPDATE account SET balance = balance + d, last_seq = last_seq + 1
 *        WHERE id = ? AND (allow_negative OR balance + d >= 0) RETURNING ...
 *       -> khoa dong + kiem tra du tien + cap seq trong MOT cau lenh (khong co khoang ho doc -> ghi)
 *       INSERT posting (seq, amount, balance_after)
 *  3. COMMIT -> trigger deferred kiem tra journal can bang
 * </pre>
 * Goi tu ben trong mot transaction co san (TransactionTemplate/@Transactional cung DataSource) thi
 * no THAM GIA transaction do (PROPAGATION_REQUIRED): khoa giu toi khi transaction ngoai commit.
 */
public final class LedgerService {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;

    public LedgerService(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    }

    public void open(long id, String code, boolean allowNegative, boolean hot) {
        jdbc.update("INSERT INTO account (id, code, allow_negative, hot) VALUES (?, ?, ?, ?)", id, code, allowNegative, hot);
    }

    public Receipt post(Request req) {
        SortedMap<Long, BigDecimal> net = req.netByAccount(); // kiem tra hinh thuc TRUOC khi mo transaction
        String hash = req.hash();
        return tx.execute(status -> {
            // 1. Gianh key. Request trung chay song song se CHO o unique index toi khi ben kia commit/rollback.
            Long journalId = jdbc.query("""
                    INSERT INTO journal (idempotency_key, request_hash, kind, effective_date, reverses)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (idempotency_key) DO NOTHING
                    RETURNING id""",
                    rs -> rs.next() ? rs.getLong(1) : null,
                    req.idempotencyKey(), hash, req.kind(), req.effectiveDate(), req.reverses());
            if (journalId == null) {
                return replay(req.idempotencyKey(), hash);
            }
            Map<Long, Boolean> hot = hotFlags(net);
            List<Applied> applied = new ArrayList<>();
            // 2. net la TreeMap -> duyet theo account id tang dan -> moi transaction khoa cung mot thu tu
            for (var e : net.entrySet()) {
                long account = e.getKey();
                BigDecimal amount = e.getValue();
                Boolean isHot = hot.get(account);
                if (isHot == null) {
                    throw new UnknownAccount(account);
                }
                if (isHot) {
                    // account nong (cash, thu nhap...): khong khoa, khong seq; so du tong hop rieng (HotAccountBalances)
                    jdbc.update("INSERT INTO posting (journal_id, account_id, amount, effective_date) VALUES (?, ?, ?, ?)",
                            journalId, account, amount, req.effectiveDate());
                    applied.add(new Applied(account, null, null));
                    continue;
                }
                Applied a = jdbc.query("""
                        UPDATE account
                           SET balance = balance + ?, last_seq = last_seq + 1
                         WHERE id = ? AND (allow_negative OR balance + ? >= 0)
                     RETURNING id, last_seq, balance""",
                        rs -> rs.next() ? new Applied(rs.getLong(1), rs.getLong(2), rs.getBigDecimal(3)) : null,
                        amount, account, amount);
                if (a == null) {
                    throw new InsufficientFunds(account, amount); // rollback ca journal: khong ghi gi, key tu do
                }
                jdbc.update("""
                        INSERT INTO posting (journal_id, account_id, seq, amount, balance_after, effective_date)
                        VALUES (?, ?, ?, ?, ?, ?)""",
                        journalId, account, a.seq(), amount, a.balanceAfter(), req.effectiveDate());
                applied.add(a);
            }
            return new Receipt(journalId, false, applied);
        });
    }

    /** Chuyen tien giua hai account; key do client sinh MOT lan cho mot y dinh chuyen tien. */
    public Receipt transfer(String key, long from, long to, BigDecimal amount, LocalDate effectiveDate) {
        return post(Request.of(key, "TRANSFER", effectiveDate, new Leg(from, amount.negate()), new Leg(to, amount)));
    }

    /** Dao but toan: ghi posting AM, khong sua dong cu. Key tu nhien "reversal:{id}" -> dao hai lan = mot lan. */
    public Receipt reverse(long journalId, LocalDate effectiveDate) {
        List<Leg> legs = jdbc.query("SELECT account_id, amount FROM posting WHERE journal_id = ?",
                (rs, i) -> new Leg(rs.getLong(1), rs.getBigDecimal(2).negate()), journalId);
        if (legs.isEmpty()) {
            throw new InvalidJournal("khong co journal " + journalId);
        }
        return post(new Request("reversal:" + journalId, "REVERSAL", effectiveDate, legs, journalId));
    }

    private Receipt replay(String key, String hash) {
        Map<String, Object> row = jdbc.queryForMap("SELECT id, request_hash FROM journal WHERE idempotency_key = ?", key);
        if (!hash.equals(row.get("request_hash"))) {
            throw new KeyReuse(key);
        }
        long id = ((Number) row.get("id")).longValue();
        List<Applied> applied = jdbc.query(
                "SELECT account_id, seq, balance_after FROM posting WHERE journal_id = ? ORDER BY account_id",
                (rs, i) -> new Applied(rs.getLong(1), (Long) rs.getObject(2), rs.getBigDecimal(3)), id);
        return new Receipt(id, true, applied);
    }

    private Map<Long, Boolean> hotFlags(SortedMap<Long, BigDecimal> net) {
        Map<Long, Boolean> hot = new HashMap<>();
        jdbc.query("SELECT id, hot FROM account WHERE id = ANY(?)",
                rs -> {
                    hot.put(rs.getLong(1), rs.getBoolean(2));
                },
                (Object) net.keySet().toArray(Long[]::new));
        return hot;
    }
}
