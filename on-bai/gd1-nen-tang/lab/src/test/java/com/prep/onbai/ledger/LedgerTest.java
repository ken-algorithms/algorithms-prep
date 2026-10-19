package com.prep.onbai.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prep.onbai.ledger.Ledger.InsufficientFunds;
import com.prep.onbai.ledger.Ledger.KeyReuse;
import com.prep.onbai.ledger.Ledger.Leg;
import com.prep.onbai.ledger.Ledger.Receipt;
import com.prep.onbai.ledger.Ledger.Request;
import com.prep.onbai.testsupport.Pg;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Bai 05 — bat bien cua so cai: can bang, append-only, so du chay, idempotency, dao but toan. */
class LedgerTest {

    static final LocalDate D = LocalDate.of(2026, 10, 18);
    static final long CASH = 1, ALICE = 10, BOB = 11;

    DataSource ds;
    LedgerService ledger;
    BalanceQueries q;
    JdbcTemplate jdbc;

    @BeforeEach
    void fresh() {
        ds = Pg.freshDatabase("b05_ledger", "ledger.sql");
        ledger = new LedgerService(ds);
        q = new BalanceQueries(ds);
        jdbc = new JdbcTemplate(ds);
        ledger.open(CASH, "cash", true, false);
        ledger.open(ALICE, "wallet:alice", false, false);
        ledger.open(BOB, "wallet:bob", false, false);
    }

    Receipt deposit(String key, long account, String amount) {
        return ledger.post(Request.of(key, "DEPOSIT", D, new Leg(CASH, new BigDecimal(amount).negate()),
                new Leg(account, new BigDecimal(amount))));
    }

    @Test
    void transferUpdatesRunningBalanceAndSeqInOneTransaction() {
        deposit("dep:1", ALICE, "100");
        Receipt r = ledger.transfer("tr:1", ALICE, BOB, new BigDecimal("30"), D);

        assertThat(r.on(ALICE).seq()).isEqualTo(2);
        assertThat(r.on(ALICE).balanceAfter()).isEqualByComparingTo("70");
        assertThat(r.on(BOB).seq()).isEqualTo(1);
        assertThat(q.current(ALICE).balance()).isEqualByComparingTo("70");
        assertThat(q.current(BOB).balance()).isEqualByComparingTo("30");
        assertThat(new LedgerReconciler(ds).run().clean()).isTrue();
    }

    @Test
    void overdraftIsRejectedAndNothingIsWritten() {
        deposit("dep:1", ALICE, "100");
        assertThatThrownBy(() -> ledger.transfer("tr:big", ALICE, BOB, new BigDecimal("100.01"), D))
                .isInstanceOf(InsufficientFunds.class);

        // rollback ca transaction: khong journal, khong posting, khong doi so du; key lai dung duoc
        assertThat(jdbc.queryForObject("SELECT count(*) FROM journal WHERE idempotency_key = 'tr:big'", Long.class)).isZero();
        assertThat(q.current(BOB).balance()).isEqualByComparingTo("0");
        assertThat(q.current(ALICE).asOfSeq()).isEqualTo(1);
    }

    @Test
    void sameKeySameRequestReplaysTheFirstReceipt() {
        deposit("dep:1", ALICE, "100");
        Receipt first = ledger.transfer("tr:1", ALICE, BOB, new BigDecimal("30"), D);
        ledger.transfer("tr:2", ALICE, BOB, new BigDecimal("5"), D); // so du doi sau lan dau
        Receipt again = ledger.transfer("tr:1", ALICE, BOB, new BigDecimal("30"), D);

        assertThat(again.replayed()).isTrue();
        assertThat(again.journalId()).isEqualTo(first.journalId());
        // tra DUNG ket qua lan dau (balance_after luc do), khong phai so du hien tai
        assertThat(again.on(ALICE).balanceAfter()).isEqualByComparingTo("70");
        assertThat(q.current(ALICE).balance()).isEqualByComparingTo("65");
    }

    @Test
    void sameKeyDifferentRequestIsRejected() {
        deposit("dep:1", ALICE, "100");
        ledger.transfer("tr:1", ALICE, BOB, new BigDecimal("30"), D);
        assertThatThrownBy(() -> ledger.transfer("tr:1", ALICE, BOB, new BigDecimal("31"), D))
                .isInstanceOf(KeyReuse.class);
    }

    @Test
    void twentyConcurrentDuplicatesExecuteExactlyOnce() throws Exception {
        deposit("dep:1", ALICE, "100");
        int n = 20;
        CyclicBarrier start = new CyclicBarrier(n);
        List<Future<Receipt>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(n)) {
            for (int i = 0; i < n; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return ledger.transfer("tr:double-click", ALICE, BOB, new BigDecimal("30"), D);
                }));
            }
        }
        long executed = 0;
        for (Future<Receipt> f : results) {
            Receipt r = f.get();
            executed += r.replayed() ? 0 : 1;
            assertThat(r.on(ALICE).balanceAfter()).isEqualByComparingTo("70"); // ai cung nhan cung mot ket qua
        }
        assertThat(executed).isEqualTo(1);
        assertThat(q.current(ALICE).balance()).isEqualByComparingTo("70");
        System.out.println("[LAB] 20 request trung key chay song song -> thuc thi 1, phat lai 19");
    }

    @Test
    void databaseRejectsAnUnbalancedJournalAtCommit() throws SQLException {
        // code co bug (hoac ai do sua tay bang SQL) ghi lech: trigger deferred chan LUC COMMIT
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(false);
            s.execute("INSERT INTO journal (idempotency_key, request_hash, kind, effective_date) VALUES ('bad', 'x', 'BUG', '2026-10-18')");
            s.execute("INSERT INTO posting (journal_id, account_id, amount, effective_date) "
                    + "SELECT id, 1, -100, '2026-10-18' FROM journal WHERE idempotency_key = 'bad'");
            s.execute("INSERT INTO posting (journal_id, account_id, amount, effective_date) "
                    + "SELECT id, 10, 99, '2026-10-18' FROM journal WHERE idempotency_key = 'bad'");
            assertThatThrownBy(c::commit).isInstanceOf(SQLException.class).hasMessageContaining("khong can bang");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM journal", Long.class)).isZero();
    }

    @Test
    void postingsAreAppendOnly() {
        deposit("dep:1", ALICE, "100");
        assertThatThrownBy(() -> jdbc.update("UPDATE posting SET amount = 1000 WHERE account_id = ?", ALICE))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("chi cho INSERT");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM journal"))
                .isInstanceOf(DataIntegrityViolationException.class).hasStackTraceContaining("chi cho INSERT");
    }

    @Test
    void reversalWritesNegativePostingsAndIsIdempotent() {
        Receipt dep = deposit("dep:wrong", ALICE, "100");
        Receipt rev = ledger.reverse(dep.journalId(), D);
        Receipt revAgain = ledger.reverse(dep.journalId(), D);

        assertThat(rev.replayed()).isFalse();
        assertThat(revAgain.replayed()).isTrue();
        assertThat(q.current(ALICE).balance()).isEqualByComparingTo("0");
        assertThat(q.current(ALICE).asOfSeq()).isEqualTo(2); // lich su con nguyen: +100 roi -100
        assertThat(jdbc.queryForObject("SELECT reverses FROM journal WHERE id = ?", Long.class, rev.journalId()))
                .isEqualTo(dep.journalId());
    }

    @Test
    void reversingMoneyAlreadySpentIsRejected() {
        Receipt dep = deposit("dep:1", ALICE, "100");
        ledger.transfer("tr:1", ALICE, BOB, new BigDecimal("80"), D);
        assertThatThrownBy(() -> ledger.reverse(dep.journalId(), D)).isInstanceOf(InsufficientFunds.class);
    }

    @Test
    void statementPagesBySeqAndHistoricalBalancesAreO1() {
        deposit("dep:1", ALICE, "100");
        ledger.transfer("tr:1", ALICE, BOB, new BigDecimal("30"), D);
        ledger.transfer("tr:2", ALICE, BOB, new BigDecimal("20"), D);
        deposit("dep:2", ALICE, "5");

        List<BalanceQueries.Line> page1 = q.statement(ALICE, 0, 2);
        List<BalanceQueries.Line> page2 = q.statement(ALICE, page1.get(1).seq(), 2);
        assertThat(page1).extracting(BalanceQueries.Line::seq).containsExactly(1L, 2L);
        assertThat(page2).extracting(BalanceQueries.Line::balanceAfter)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("50"), new BigDecimal("55"));
        assertThat(q.asOfSeq(ALICE, 2)).isEqualByComparingTo("70");
    }

    @Test
    void backdatedEntryChangesBalanceAsOfDateButNotTheSeqHistory() {
        LocalDate d15 = LocalDate.of(2026, 10, 15);
        ledger.post(Request.of("dep:15", "DEPOSIT", d15, new Leg(CASH, new BigDecimal("-100")), new Leg(ALICE, new BigDecimal("100"))));
        ledger.post(Request.of("dep:18", "DEPOSIT", D, new Leg(CASH, new BigDecimal("-50")), new Leg(ALICE, new BigDecimal("50"))));
        // ghi hom nay nhung thuoc ve ngay 15 (khach chuyen tu ngay 15, ngan hang bao muon)
        ledger.post(Request.of("dep:late", "DEPOSIT", d15, new Leg(CASH, new BigDecimal("-7")), new Leg(ALICE, new BigDecimal("7"))));

        assertThat(q.asOfDate(ALICE, d15)).isEqualByComparingTo("107"); // so du ngay 15 theo hieu biet HOM NAY
        assertThat(q.asOfSeq(ALICE, 1)).isEqualByComparingTo("100");    // luc ghi seq 1, ta TUONG ngay 15 la 100
        assertThat(q.current(ALICE).balance()).isEqualByComparingTo("157");
    }
}
