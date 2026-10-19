package com.prep.onbai.accrual;

import com.prep.onbai.accrual.LoanBook.Loan;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Batch khong bao gio bat khach cho: moi chunk mot transaction, khoa bang SKIP LOCKED.
 * Khoan vay nao dang co giao dich (dong account bi khoa) thi BO QUA, vong sau lam; nho key tu nhien
 * nen chay lai bao nhieu vong cung khong ghi trung.
 */
public final class SkipLockedAccrualBatch {

    public record Pass(List<Long> accrued, List<Long> alreadyDone, List<Long> skipped) {}

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final LockedAccrualService accrual;

    public SkipLockedAccrualBatch(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        this.accrual = new LockedAccrualService(ds);
    }

    public Pass runPass(List<Loan> loans, LocalDate date, int chunkSize) {
        List<Long> accrued = new ArrayList<>(), alreadyDone = new ArrayList<>(), skipped = new ArrayList<>();
        List<Loan> sorted = loans.stream().sorted(Comparator.comparingLong(Loan::principal)).toList();
        for (int from = 0; from < sorted.size(); from += chunkSize) {
            List<Loan> chunk = sorted.subList(from, Math.min(from + chunkSize, sorted.size()));
            tx.executeWithoutResult(s -> {
                // MOT cau lenh khoa moi account cua chunk, id tang dan, khong cho ai
                Long[] ids = chunk.stream().flatMap(l -> java.util.stream.Stream.of(l.principal(), l.interest()))
                        .sorted().toArray(Long[]::new);
                Set<Long> got = new HashSet<>(jdbc.queryForList(
                        "SELECT id FROM account WHERE id = ANY(?) ORDER BY id FOR UPDATE SKIP LOCKED", Long.class, (Object) ids));
                for (Loan loan : chunk) {
                    if (!got.contains(loan.principal()) || !got.contains(loan.interest())) {
                        skipped.add(loan.id()); // dem lai: bi bo qua nhieu vong lien tiep -> alert
                        continue;
                    }
                    var r = accrual.accrueOne(loan, date); // tham gia transaction cua chunk, khoa da co san
                    (r.alreadyDone() ? alreadyDone : accrued).add(loan.id());
                }
            });
        }
        return new Pass(accrued, alreadyDone, skipped);
    }
}
