package com.prep.onbai.accrual;

import com.prep.onbai.ledger.Ledger.Leg;
import com.prep.onbai.ledger.Ledger.Receipt;
import com.prep.onbai.ledger.Ledger.Request;
import com.prep.onbai.ledger.LedgerService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Mo khoan vay, giai ngan, thanh toan — cac duong ghi cua khach, deu di qua LedgerService. */
public final class LoanBook {

    /** Account he thong, NONG: moi khoan vay deu cham -> khong khoa (xem bai 05, HotAccountBalances). */
    public static final long CASH = 1, INTEREST_INCOME = 2;

    public record Loan(long id, long principal, long interest, BigDecimal dailyRate) {}

    /** Gio chot so (EOB). Giao dich sau 19:00 thuoc ve NGAY LAM VIEC SAU. */
    public static final LocalTime EOB = LocalTime.of(19, 0);
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JdbcTemplate jdbc;
    private final LedgerService ledger;

    public LoanBook(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.ledger = new LedgerService(ds);
    }

    public void openSystemAccounts() {
        ledger.open(CASH, "cash", true, true);
        ledger.open(INTEREST_INCOME, "income:interest", true, true);
    }

    public Loan open(long loanId, BigDecimal dailyRate) {
        Loan loan = new Loan(loanId, loanId * 10 + 1, loanId * 10 + 2, dailyRate);
        ledger.open(loan.principal(), "loan:" + loanId + ":principal", false, false);
        ledger.open(loan.interest(), "loan:" + loanId + ":interest", false, false);
        jdbc.update("INSERT INTO loan (id, principal_account, interest_account, daily_rate) VALUES (?, ?, ?, ?)",
                loanId, loan.principal(), loan.interest(), dailyRate);
        return loan;
    }

    public Receipt disburse(Loan loan, BigDecimal amount, LocalDate date) {
        return ledger.post(Request.of("disburse:" + loan.id(), "DISBURSE", date,
                new Leg(loan.principal(), amount), new Leg(CASH, amount.negate())));
    }

    /** Khach tra goc. requestId do client sinh: bam hai lan = mot lan. */
    public Receipt pay(Loan loan, String requestId, BigDecimal amount, LocalDate effectiveDate) {
        return ledger.post(Request.of("payment:" + requestId, "PAYMENT", effectiveDate,
                new Leg(CASH, amount), new Leg(loan.principal(), amount.negate())));
    }

    /** Ngay nghiep vu cua mot giao dich: sau gio chot so -> ngay hom sau. Mot quy tac, khong phai dong ho cua job. */
    public static LocalDate effectiveDate(Instant recordedAt) {
        ZonedDateTime local = recordedAt.atZone(ZONE);
        return local.toLocalTime().isBefore(EOB) ? local.toLocalDate() : local.toLocalDate().plusDays(1);
    }

    /** Lai = du no x lai suat ngay, lam tron 2 chu so kieu ngan hang (HALF_EVEN: khong lech ve mot phia). */
    public static BigDecimal interestOn(BigDecimal basis, BigDecimal dailyRate) {
        return basis.multiply(dailyRate).setScale(2, RoundingMode.HALF_EVEN);
    }
}
