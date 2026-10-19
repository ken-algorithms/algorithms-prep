package com.prep.onbai.accrual;

import com.prep.onbai.accrual.LoanBook.Loan;
import com.prep.onbai.ledger.Ledger.Leg;
import com.prep.onbai.ledger.Ledger.Receipt;
import com.prep.onbai.ledger.Ledger.Request;
import com.prep.onbai.ledger.LedgerService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ban thiet ke lai (doc 06 §12.1): doi DINH NGHIA thay vi them khoa.
 * <pre>
 *   CU : lai = f(so du DOC DUOC luc job chay)                  -> phu thuoc dong ho -> phai khoa de ghim
 *   MOI: lai ngay D = f(moi posting goc co effective_date <= D) -> ham thuan cua du lieu
 * </pre>
 * Giao dich sau gio chot so co effective_date = D+1 nen khong anh huong lai ngay D, du job chay truoc
 * hay sau no. Giao dich ghi MUON thuoc ngay D -> khong phai race ma la TINH LAI: ghi but toan dieu
 * chinh (ADJUSTMENT), khong sua dong cu. Khoa o day chi de hai ban chay cung luc khong cung ghi.
 */
public final class EffectiveDateAccrual {

    public record Result(long journalId, String kind, BigDecimal basis, long basisSeq, BigDecimal interest) {}

    private record Basis(BigDecimal balance, long seq) {}

    private final JdbcTemplate jdbc;
    private final LedgerService ledger;
    private final TransactionTemplate tx;

    public EffectiveDateAccrual(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.ledger = new LedgerService(ds);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    }

    /** Tinh lai ngay D mot lan. Chay lai -> tra but toan da co. */
    public Optional<Result> accrue(Loan loan, LocalDate day) {
        return tx.execute(s -> {
            lock(loan);
            String key = "accrual:" + loan.id() + ":" + day;
            if (exists(key)) {
                return Optional.empty();
            }
            Basis b = basis(loan, day);
            return post(loan, day, "ACCRUAL", key, b, LoanBook.interestOn(b.balance(), loan.dailyRate()));
        });
    }

    /** Doi soat ngay D theo dinh nghia; lech -> ghi ADJUSTMENT. Key chua seq cua basis: cung du lieu thi khong ghi hai lan. */
    public Optional<Result> reconcile(Loan loan, LocalDate day) {
        return tx.execute(s -> {
            lock(loan);
            Basis b = basis(loan, day);
            BigDecimal expected = LoanBook.interestOn(b.balance(), loan.dailyRate());
            BigDecimal diff = expected.subtract(postedInterest(jdbc, loan, day));
            if (diff.signum() == 0) {
                return Optional.empty();
            }
            return post(loan, day, "ADJUSTMENT", "accrual-adj:" + loan.id() + ":" + day + ":" + b.seq(), b, diff);
        });
    }

    /** Lai da ghi cho ngay D = tong posting ACCRUAL + ADJUSTMENT tren account lai, effective_date = D. */
    static BigDecimal postedInterest(JdbcTemplate jdbc, Loan loan, LocalDate day) {
        return jdbc.queryForObject("""
                SELECT coalesce(sum(p.amount), 0) FROM posting p JOIN journal j ON j.id = p.journal_id
                 WHERE p.account_id = ? AND p.effective_date = ? AND j.kind IN ('ACCRUAL', 'ADJUSTMENT')""",
                BigDecimal.class, loan.interest(), day);
    }

    private Basis basis(Loan loan, LocalDate day) {
        return jdbc.queryForObject("""
                SELECT coalesce(sum(amount), 0), coalesce(max(seq), 0)
                  FROM posting WHERE account_id = ? AND effective_date <= ?""",
                (rs, i) -> new Basis(rs.getBigDecimal(1), rs.getLong(2)), loan.principal(), day);
    }

    private Optional<Result> post(Loan loan, LocalDate day, String kind, String key, Basis b, BigDecimal amount) {
        if (amount.signum() == 0) {
            return Optional.empty();
        }
        Receipt r = ledger.post(Request.of(key, kind, day,
                new Leg(loan.interest(), amount), new Leg(LoanBook.INTEREST_INCOME, amount.negate())));
        jdbc.update("""
                INSERT INTO accrual (journal_id, loan_id, business_date, kind, basis_balance, basis_seq, interest)
                VALUES (?, ?, ?, ?, ?, ?, ?)""", r.journalId(), loan.id(), day, kind, b.balance(), b.seq(), amount);
        return Optional.of(new Result(r.journalId(), kind, b.balance(), b.seq(), amount));
    }

    private void lock(Loan loan) {
        jdbc.query("SELECT id FROM account WHERE id IN (?, ?) ORDER BY id FOR UPDATE", rs -> {}, loan.principal(), loan.interest());
    }

    private boolean exists(String key) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM journal WHERE idempotency_key = ?)", Boolean.class, key));
    }
}
