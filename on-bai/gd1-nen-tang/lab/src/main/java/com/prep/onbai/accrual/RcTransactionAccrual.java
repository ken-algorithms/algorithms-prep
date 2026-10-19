package com.prep.onbai.accrual;

import com.prep.onbai.accrual.LoanBook.Loan;
import com.prep.onbai.ledger.Ledger.Leg;
import com.prep.onbai.ledger.Ledger.Receipt;
import com.prep.onbai.ledger.Ledger.Request;
import com.prep.onbai.ledger.LedgerService;
import java.math.BigDecimal;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * "Giai phap truc giac" #2: boc doc + ghi vao MOT transaction (@Transactional), mac dinh READ COMMITTED.
 * Van sai: READ COMMITTED khong khoa gi khi doc; moi cau lenh thay du lieu moi nhat luc no chay.
 * seqAtCommit != basisSeq nghia la: luc ghi, du no goc DA DOI so voi luc tinh (TOCTOU).
 */
public final class RcTransactionAccrual {

    public record Result(long journalId, BigDecimal basis, long basisSeq, long seqAtCommit, BigDecimal interest) {

        public boolean staleAtCommit() {
            return basisSeq != seqAtCommit;
        }
    }

    private final JdbcTemplate jdbc;
    private final LedgerService ledger;
    private final TransactionTemplate tx;

    public RcTransactionAccrual(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.ledger = new LedgerService(ds);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
    }

    public Result accrueOne(Loan loan, LocalDate date, Runnable gapAfterRead) {
        return tx.execute(s -> {
            var basis = jdbc.queryForMap("SELECT balance, last_seq FROM account WHERE id = ?", loan.principal());
            BigDecimal balance = (BigDecimal) basis.get("balance");
            long basisSeq = ((Number) basis.get("last_seq")).longValue();
            gapAfterRead.run();
            BigDecimal interest = LoanBook.interestOn(balance, loan.dailyRate());
            Receipt r = ledger.post(Request.of("accrual:" + loan.id() + ":" + date, "ACCRUAL", date,
                    new Leg(loan.interest(), interest), new Leg(LoanBook.INTEREST_INCOME, interest.negate())));
            Long seqNow = jdbc.queryForObject("SELECT last_seq FROM account WHERE id = ?", Long.class, loan.principal());
            return new Result(r.journalId(), balance, basisSeq, seqNow, interest);
        });
    }
}
