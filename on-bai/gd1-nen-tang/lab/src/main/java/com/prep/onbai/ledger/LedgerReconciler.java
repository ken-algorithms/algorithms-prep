package com.prep.onbai.ledger;

import java.math.BigDecimal;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Doi soat: khong tin la dung, DO de biet dung. Moi danh sach phai RONG; chay hang ngay, khac rong -> alert. */
public final class LedgerReconciler {

    public record Report(List<Long> unbalancedJournals, List<Long> balanceDrift, List<Long> seqGaps,
                         List<String> brokenChains, BigDecimal systemTotal) {

        public boolean clean() {
            return unbalancedJournals.isEmpty() && balanceDrift.isEmpty() && seqGaps.isEmpty()
                    && brokenChains.isEmpty() && systemTotal.signum() == 0;
        }
    }

    private final JdbcTemplate jdbc;

    public LedgerReconciler(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
    }

    public Report run() {
        // 1. ke toan kep: tong moi journal = 0 (trigger da chan luc ghi; day la kiem tra doc lap)
        List<Long> unbalanced = jdbc.queryForList(
                "SELECT journal_id FROM posting GROUP BY journal_id HAVING sum(amount) <> 0 ORDER BY 1", Long.class);
        // 2. cot balance (gia tri dan xuat) phai bang SUM(posting) (nguon su that)
        List<Long> drift = jdbc.queryForList("""
                SELECT a.id FROM account a LEFT JOIN posting p ON p.account_id = a.id
                 WHERE NOT a.hot
                 GROUP BY a.id, a.balance
                HAVING a.balance <> coalesce(sum(p.amount), 0) ORDER BY 1""", Long.class);
        // 3. seq lien tuc 1..last_seq: nhay coc = co posting bien mat
        List<Long> gaps = jdbc.queryForList("""
                SELECT a.id FROM account a LEFT JOIN posting p ON p.account_id = a.id
                 WHERE NOT a.hot
                 GROUP BY a.id, a.last_seq
                HAVING a.last_seq <> count(p.id) OR a.last_seq <> coalesce(max(p.seq), 0) ORDER BY 1""", Long.class);
        // 4. so du chay: balance_after(k) = balance_after(k-1) + amount(k)
        List<String> chains = jdbc.queryForList("""
                SELECT account_id || '#' || seq FROM (
                    SELECT account_id, seq, balance_after,
                           sum(amount) OVER (PARTITION BY account_id ORDER BY seq) AS running
                      FROM posting WHERE seq IS NOT NULL) x
                 WHERE balance_after <> running ORDER BY 1""", String.class);
        // 5. he dong: tong moi posting cua moi account = 0
        BigDecimal total = jdbc.queryForObject("SELECT coalesce(sum(amount), 0) FROM posting", BigDecimal.class);
        return new Report(unbalanced, drift, gaps, chains, total);
    }
}
