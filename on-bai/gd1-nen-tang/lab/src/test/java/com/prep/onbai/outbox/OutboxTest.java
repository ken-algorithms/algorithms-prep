package com.prep.onbai.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.prep.onbai.testsupport.Pg;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Bai 09 — transactional outbox, relay SKIP LOCKED, consumer idempotent, tren Postgres that. */
class OutboxTest {

    DataSource ds;
    JdbcTemplate jdbc;
    FakeBroker broker;

    @BeforeEach
    void fresh() {
        ds = Pg.freshDatabase("b09_outbox", 24, "outbox.sql");
        jdbc = new JdbcTemplate(ds);
        broker = new FakeBroker();
    }

    long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    @Test
    void theOrderAndItsEventCommitOrRollBackTogether() {
        OrderService orders = new OrderService(ds);
        TransactionTemplate outer = new TransactionTemplate(new DataSourceTransactionManager(ds));
        assertThatThrownBy(() -> outer.executeWithoutResult(s -> {
            orders.placeOrder("alice", 500_000); // tham gia transaction ngoai
            throw new IllegalStateException("buoc sau that bai");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(count("orders")).isZero();
        assertThat(count("outbox")).isZero(); // khong co su kien ma
        orders.placeOrder("bob", 200_000);
        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("outbox")).isEqualTo(1);
    }

    @Test
    void dualWritePublishesAnEventForAnOrderThatNeverExisted() {
        DualWriteOrderService dual = new DualWriteOrderService(ds, broker);
        assertThatThrownBy(() -> dual.placeOrder("alice", 500_000, () -> {
            throw new IllegalStateException("loi sau khi da publish");
        })).isInstanceOf(IllegalStateException.class);
        System.out.println("[LAB] dual write: orders = " + count("orders") + ", broker da nhan " + broker.received().size() + " su kien");
        assertThat(count("orders")).isZero();
        assertThat(broker.received()).hasSize(1); // consumer se gui email cho don hang KHONG TON TAI
    }

    @Test
    void fourRelaysWithSkipLockedShareTheWorkWithoutDuplicates() throws Exception {
        OrderService orders = new OrderService(ds);
        for (int i = 0; i < 1_000; i++) {
            orders.placeOrder("c" + i, 1_000 + i);
        }
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            for (int r = 0; r < 4; r++) {
                OutboxRelay relay = new OutboxRelay(ds, broker, 10, 100);
                pool.submit(() -> {
                    while (relay.pollOnce(25) > 0) {
                        // het viec thi dung
                    }
                    return null;
                });
            }
        }
        Set<String> ids = new HashSet<>();
        broker.received().forEach(m -> ids.add(m.messageId()));
        System.out.println("[LAB] 4 relay SKIP LOCKED: 1.000 su kien -> broker nhan " + broker.received().size() + ", khac nhau " + ids.size());
        assertThat(broker.received()).hasSize(1_000);
        assertThat(ids).hasSize(1_000);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox WHERE published_at IS NULL", Long.class)).isZero();
    }

    @Test
    void aRelayThatDiesAfterPublishingCausesADuplicateTheConsumerAbsorbs() {
        new OrderService(ds).placeOrder("alice", 500_000);
        OutboxRelay relay = new OutboxRelay(ds, broker, 10, 100);
        relay.afterPublish(() -> {
            throw new IllegalStateException("pod bi kill sau khi broker da nhan, truoc khi danh dau");
        });
        assertThatThrownBy(() -> relay.pollOnce(10)).isInstanceOf(IllegalStateException.class);
        relay.afterPublish(() -> {});
        relay.pollOnce(10); // dong van chua danh dau -> phat LAI

        NotificationConsumer consumer = new NotificationConsumer(ds);
        List<Boolean> handled = broker.received().stream().map(consumer::handle).toList();
        System.out.println("[LAB] relay chet giua chung: broker nhan " + broker.received().size() + " ban, consumer xu ly " + handled
                + " -> " + count("notification") + " email");
        assertThat(broker.received()).hasSize(2);
        assertThat(handled).containsExactly(true, false);
        assertThat(count("notification")).isEqualTo(1);
    }

    @Test
    void aFailingBrokerIsRetriedWithBackoffUntilItWorks() throws Exception {
        new OrderService(ds).placeOrder("alice", 500_000);
        OutboxRelay relay = new OutboxRelay(ds, broker, 20, 200);
        broker.failNext(3);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        int polls = 0;
        while (broker.received().isEmpty() && System.nanoTime() < deadline) {
            relay.pollOnce(10);
            polls++;
            Thread.sleep(5);
        }
        var row = jdbc.queryForMap("SELECT attempts, last_error, published_at IS NOT NULL AS done FROM outbox");
        System.out.println("[LAB] broker loi 3 lan: " + polls + " vong poll, outbox = " + row);
        assertThat(row.get("done")).isEqualTo(true);
        assertThat(row.get("attempts")).isEqualTo(4);
        assertThat(polls).isGreaterThan(4); // co nhung vong khong lay duoc gi vi chua toi next_attempt_at
    }

    @Test
    void anIdCursorSkipsALateCommitStatusPollingDoesNot() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        CountDownLatch inserted = new CountDownLatch(1), release = new CountDownLatch(1);
        IdCursorRelay cursorRelay = new IdCursorRelay(ds, broker);
        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            Future<?> slow = pool.submit(() -> tx.executeWithoutResult(s -> {
                jdbc.update("INSERT INTO outbox (aggregate_id, event_type, payload) VALUES ('1', 'ORDER_PLACED', '{\"orderId\":1}')");
                inserted.countDown(); // lay id 1, chua commit
                await(release);
            }));
            assertThat(inserted.await(5, TimeUnit.SECONDS)).isTrue();
            jdbc.update("INSERT INTO outbox (aggregate_id, event_type, payload) VALUES ('2', 'ORDER_PLACED', '{\"orderId\":2}')");
            cursorRelay.pollOnce(10); // thay id 2, con tro = 2
            release.countDown();
            slow.get(5, TimeUnit.SECONDS);
        }
        int late = cursorRelay.pollOnce(10); // id 1 < con tro: khong bao gio thay
        FakeBroker statusBroker = new FakeBroker();
        int byStatus = new OutboxRelay(ds, statusBroker, 10, 100).pollOnce(10);
        System.out.println("[LAB] con tro id: vong 2 thay " + late + " dong (sot id 1) | theo published_at IS NULL: phat " + byStatus + " dong");
        assertThat(late).isZero();
        assertThat(byStatus).isEqualTo(2);
    }

    @Test
    void fullJitterStaysWithinTheExponentialCeiling() {
        Random rnd = new Random(1);
        for (int attempt = 0; attempt < 12; attempt++) {
            long ceiling = Math.min(5_000, 100L << attempt);
            for (int i = 0; i < 200; i++) {
                assertThat(Backoff.fullJitterMs(attempt, 100, 5_000, rnd)).isBetween(0L, ceiling);
            }
        }
        AtomicBoolean spread = new AtomicBoolean();
        long first = Backoff.fullJitterMs(5, 100, 5_000, rnd);
        for (int i = 0; i < 10; i++) {
            spread.compareAndSet(false, Backoff.fullJitterMs(5, 100, 5_000, rnd) != first);
        }
        assertThat(spread).isTrue(); // 1.000 client cung loi khong retry cung mot mili giay
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
