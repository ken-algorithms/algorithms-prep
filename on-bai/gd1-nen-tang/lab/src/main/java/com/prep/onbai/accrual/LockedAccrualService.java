package com.prep.onbai.accrual;

import com.prep.onbai.accrual.LoanBook.Loan;
import com.prep.onbai.ledger.Ledger.Leg;
import com.prep.onbai.ledger.Ledger.Receipt;
import com.prep.onbai.ledger.Ledger.Request;
import com.prep.onbai.ledger.LedgerService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ban va cho he thong dang chay (doc 06 §6): moi khoan vay MOT transaction ngan:
 * <pre>
 *  1. SELECT ... FROM account WHERE id IN (goc, lai) ORDER BY id FOR UPDATE   -- khoa MOI account se cham
 *  2. da tinh roi (key accrual:{loan}:{ngay} ton tai)? -> bo qua              -- chay lai an toan
 *  3. doc du no goc -> tinh lai -> ghi but toan + dau vet                      -- khong ai chen vao duoc
 *  4. COMMIT -> nha khoa; thanh toan dang cho se chay tiep
 * </pre>
 * Duong thanh toan khoa cung dong account goc (UPDATE trong LedgerService) -> hai ben tuan tu hoa
 * tren TUNG khoan vay, con cac khoan vay khac nhau van chay song song.
 */
public final class LockedAccrualService {

    public record Result(Long journalId, BigDecimal basis, long basisSeq, BigDecimal interest, boolean alreadyDone) {}

    private record Row(BigDecimal balance, long lastSeq) {}

    private final JdbcTemplate jdbc;
    private final LedgerService ledger;
    private final TransactionTemplate tx;

    public LockedAccrualService(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.ledger = new LedgerService(ds);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    }

    public Result accrueOne(Loan loan, LocalDate date) {
        return tx.execute(s -> {
            Map<Long, Row> locked = lockInIdOrder(loan.principal(), loan.interest());
            String key = "accrual:" + loan.id() + ":" + date;
            Long done = jdbc.query("SELECT id FROM journal WHERE idempotency_key = ?",
                    rs -> rs.next() ? rs.getLong(1) : null, key);
            if (done != null) {
                return new Result(done, null, -1, null, true); // check-then-act AN TOAN vi dang giu khoa
            }
            Row principal = locked.get(loan.principal());
            BigDecimal interest = LoanBook.interestOn(principal.balance(), loan.dailyRate());
            if (interest.signum() == 0) {
                return new Result(null, principal.balance(), principal.lastSeq(), interest, false);
            }
            Receipt r = ledger.post(Request.of(key, "ACCRUAL", date,
                    new Leg(loan.interest(), interest), new Leg(LoanBook.INTEREST_INCOME, interest.negate())));
            jdbc.update("""
                    INSERT INTO accrual (journal_id, loan_id, business_date, kind, basis_balance, basis_seq, interest)
                    VALUES (?, ?, ?, 'ACCRUAL', ?, ?, ?)""",
                    r.journalId(), loan.id(), date, principal.balance(), principal.lastSeq(), interest);
            return new Result(r.journalId(), principal.balance(), principal.lastSeq(), interest, false);
        });
    }

    /** Khoa theo id TANG DAN, ke ca account chi doc: cung quy uoc voi LedgerService -> khong vong cho. */
    private Map<Long, Row> lockInIdOrder(Long... ids) {
        Map<Long, Row> rows = new HashMap<>();
        jdbc.query("SELECT id, balance, last_seq FROM account WHERE id = ANY(?) ORDER BY id FOR UPDATE",
                rs -> {
                    rows.put(rs.getLong(1), new Row(rs.getBigDecimal(2), rs.getLong(3)));
                },
                (Object) ids);
        if (rows.size() != ids.length) {
            throw new IllegalStateException("thieu account " + List.of(ids));
        }
        return rows;
    }
}
