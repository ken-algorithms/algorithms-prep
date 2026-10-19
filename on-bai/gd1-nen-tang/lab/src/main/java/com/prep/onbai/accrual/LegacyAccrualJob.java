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
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * ANTI-PATTERN: dung lai dung hinh dang job tinh lai trong he thong that (doc 06 §2):
 * <pre>
 *  (1) doc so du CA LO                       -- transaction 1
 *      ... xu ly ca lo (giay toi phut) ...   -- KHOANG HO: khong ai giu account
 *  (2) ghi but toan lai, key ngau nhien      -- transaction 2
 *  (3) UPDATE current_balance = so DA DOC    -- transaction 3: de nguoc so du da dung
 * </pre>
 */
public final class LegacyAccrualJob {

    private final JdbcTemplate jdbc;
    private final LedgerService ledger;

    public LegacyAccrualJob(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.ledger = new LedgerService(ds);
    }

    /** gapAfterRead: thoi gian xu ly ca lo. Test chen mot giao dich cua khach vao dung cho nay. */
    public void run(List<Loan> loans, LocalDate date, Runnable gapAfterRead) {
        Map<Long, BigDecimal> balances = readBalances(loans);  // (1)
        gapAfterRead.run();
        for (Loan loan : loans) {
            BigDecimal basis = balances.get(loan.principal());
            BigDecimal interest = LoanBook.interestOn(basis, loan.dailyRate());
            postInterest(loan, date, interest);                                                  // (2)
            jdbc.update("UPDATE account SET balance = ? WHERE id = ?", basis, loan.principal()); // (3)
        }
    }

    /** Buoc (2) dung rieng: key ngau nhien -> retry sau timeout = ghi lai lan nua. */
    public Receipt postInterest(Loan loan, LocalDate date, BigDecimal interest) {
        return ledger.post(Request.of("accrual:" + UUID.randomUUID(), "ACCRUAL", date,
                new Leg(loan.interest(), interest), new Leg(LoanBook.INTEREST_INCOME, interest.negate())));
    }

    private Map<Long, BigDecimal> readBalances(List<Loan> loans) {
        Map<Long, BigDecimal> out = new HashMap<>();
        jdbc.query("SELECT account_id, sum(amount) FROM posting WHERE account_id = ANY(?) GROUP BY account_id",
                rs -> {
                    out.put(rs.getLong(1), rs.getBigDecimal(2));
                },
                (Object) loans.stream().map(Loan::principal).toArray(Long[]::new));
        return out;
    }
}
