package com.prep.onbai.accrual;

import com.prep.onbai.accrual.LoanBook.Loan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Kiem tra lai theo DINH NGHIA, khong theo thoi diem: lai da ghi cho (khoan vay, ngay D) phai bang
 * round(lai suat x du no goc theo ngay D). Vi dinh nghia la ham cua du lieu nen kiem duoc SAU SU KIEN —
 * day la thu bien loi "sai trong im lang" thanh loi bat duoc.
 */
public final class AccrualAudit {

    public record Mismatch(long loanId, LocalDate day, BigDecimal expected, BigDecimal posted) {}

    private final JdbcTemplate jdbc;

    public AccrualAudit(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
    }

    public List<Mismatch> check(LocalDate day) {
        List<Mismatch> out = new ArrayList<>();
        for (Loan loan : jdbc.query("SELECT id, principal_account, interest_account, daily_rate FROM loan ORDER BY id",
                (rs, i) -> new Loan(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getBigDecimal(4)))) {
            BigDecimal basis = jdbc.queryForObject(
                    "SELECT coalesce(sum(amount), 0) FROM posting WHERE account_id = ? AND effective_date <= ?",
                    BigDecimal.class, loan.principal(), day);
            BigDecimal expected = LoanBook.interestOn(basis, loan.dailyRate());
            BigDecimal posted = EffectiveDateAccrual.postedInterest(jdbc, loan, day);
            if (expected.compareTo(posted) != 0) {
                out.add(new Mismatch(loan.id(), day, expected, posted));
            }
        }
        return out;
    }
}
