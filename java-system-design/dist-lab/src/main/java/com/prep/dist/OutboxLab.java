package com.prep.dist;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/** Outbox + relay SKIP LOCKED + consumer idempotent, tren Postgres 16 that va Kafka that. */
public final class OutboxLab {

    static final String SCHEMA = """
        create table account(id bigint primary key, balance bigint not null check (balance >= 0));
        create table transfer(id uuid primary key, from_acc bigint not null, to_acc bigint not null, amount bigint not null);
        create table ledger_entry(id bigserial primary key, transfer_id uuid not null, account_id bigint not null, amount bigint not null);
        create table outbox(
            id bigserial primary key,
            event_id uuid not null unique,
            topic text not null,
            msg_key text not null,
            payload text not null,
            created_at timestamptz not null default now(),
            published_at timestamptz);
        create index outbox_pending on outbox(id) where published_at is null;
        create table processed_event(event_id uuid primary key, processed_at timestamptz not null default now());
        create table notification(id bigserial primary key, transfer_id uuid not null, account_id bigint not null, body text not null);
        """;

    // ---------- phia ghi: MOT transaction cho so cai + outbox ----------
    static boolean transfer(DataSource ds, long from, long to, long amount) throws SQLException {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement debit = c.prepareStatement(
                    "update account set balance = balance - ? where id = ? and balance >= ?")) {
                debit.setLong(1, amount); debit.setLong(2, from); debit.setLong(3, amount);
                if (debit.executeUpdate() == 0) { c.rollback(); return false; }   // khong du so du
            }
            exec(c, "update account set balance = balance + ? where id = ?", amount, to);
            UUID tid = UUID.randomUUID();
            exec(c, "insert into transfer(id, from_acc, to_acc, amount) values (?, ?, ?, ?)", tid, from, to, amount);
            exec(c, "insert into ledger_entry(transfer_id, account_id, amount) values (?, ?, ?), (?, ?, ?)",
                    tid, from, -amount, tid, to, amount);
            exec(c, "insert into outbox(event_id, topic, msg_key, payload) values (?, ?, ?, ?)",
                    UUID.randomUUID(), TOPIC, "acct-" + from,
                    "{\"transferId\":\"" + tid + "\",\"from\":" + from + ",\"to\":" + to + ",\"amount\":" + amount + "}");
            c.commit();
            return true;
        }
    }

    // ---------- relay: claim bang SKIP LOCKED, gui, danh dau ----------
    static int relayOnce(DataSource ds, KafkaProducer<String, String> producer, boolean crashAfterSend) throws Exception {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            List<Long> ids = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("""
                    select id, event_id, topic, msg_key, payload from outbox
                    where published_at is null order by id limit 100
                    for update skip locked""");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var rec = new ProducerRecord<>(rs.getString("topic"), rs.getString("msg_key"), rs.getString("payload"));
                    rec.headers().add("event-id", rs.getString("event_id").getBytes());
                    producer.send(rec).get();                     // dong bo: chi danh dau khi broker da ack
                    ids.add(rs.getLong("id"));
                }
            }
            if (ids.isEmpty()) { c.rollback(); return 0; }
            if (crashAfterSend) { c.rollback(); throw new IllegalStateException("relay crash sau khi gui, truoc khi danh dau"); }
            try (PreparedStatement mark = c.prepareStatement("update outbox set published_at = now() where id = any(?)")) {
                mark.setArray(1, c.createArrayOf("bigint", ids.toArray()));
                mark.executeUpdate();
            }
            c.commit();
            return ids.size();
        }
    }

    // ---------- consumer idempotent: dedup + tac dung phu trong CUNG transaction ----------
    static int consumeIdempotent(DataSource ds, KafkaConsumer<String, String> consumer, AtomicInteger skipped) throws SQLException {
        int applied = 0;
        for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(500))) {
            String eventId = new String(r.headers().lastHeader("event-id").value());
            try (Connection c = ds.getConnection()) {
                c.setAutoCommit(false);
                int inserted;
                try (PreparedStatement ps = c.prepareStatement(
                        "insert into processed_event(event_id) values (?) on conflict do nothing")) {
                    ps.setObject(1, UUID.fromString(eventId));
                    inserted = ps.executeUpdate();
                }
                if (inserted == 1) {
                    String tid = r.value().split("\"")[3];
                    exec(c, "insert into notification(transfer_id, account_id, body) values (?, ?, ?)",
                            UUID.fromString(tid), Long.parseLong(r.key().substring(5)), "Ban vua chuyen tien");
                    applied++;
                } else {
                    skipped.incrementAndGet();
                }
                c.commit();
            }
        }
        consumer.commitSync();                                    // commit offset SAU khi DB da commit
        return applied;
    }

    static final String TOPIC = "transfer.completed.lab." + System.nanoTime();

    public static void main(String[] args) throws Exception {
        String bootstrap = args.length > 0 ? args[0] : "localhost:9092";
        try (EmbeddedPostgres pg = EmbeddedPostgres.builder().start()) {
            DataSource ds = pg.getPostgresDatabase();
            try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
                System.out.println(">>> " + query(c, "select version()").split(",")[0]);
                st.execute(SCHEMA);
                st.execute("insert into account select g, 1000000 from generate_series(1, 100) g");
            }
            try (Admin admin = Admin.create(Map.of("bootstrap.servers", bootstrap))) {
                admin.createTopics(List.of(new NewTopic(TOPIC, 4, (short) 1))).all().get();
            }

            // 1) 1.000 lenh chuyen tien tu 8 thread, tien ngau nhien
            var ok = new AtomicInteger();
            try (var ex = Executors.newFixedThreadPool(8)) {
                for (int i = 0; i < 1000; i++) {
                    ex.submit(() -> {
                        var rnd = ThreadLocalRandom.current();
                        long from = rnd.nextLong(1, 101), to = rnd.nextLong(1, 101);
                        if (from == to) to = from % 100 + 1;
                        if (transfer(ds, from, to, rnd.nextLong(1, 50_000))) ok.incrementAndGet();
                        return null;
                    });
                }
            }
            System.out.println("transfers committed: " + ok.get());

            // 2) relay A gui 2 lo binh thuong, lo 3 crash sau khi gui -> 100 message bi gui lai sau
            Properties pp = new Properties();
            pp.put("bootstrap.servers", bootstrap);
            pp.put("key.serializer", StringSerializer.class.getName());
            pp.put("value.serializer", StringSerializer.class.getName());
            pp.put("acks", "all");
            try (var producer = new KafkaProducer<String, String>(pp)) {
                int a = relayOnce(ds, producer, false) + relayOnce(ds, producer, false);
                try { relayOnce(ds, producer, true); } catch (IllegalStateException e) { System.out.println("relay A: " + e.getMessage()); }
                System.out.println("relay A published+marked: " + a);
                // 3) hai relay B, C chay SONG SONG tren phan con lai
                var published = new AtomicInteger();
                try (var ex = Executors.newFixedThreadPool(2)) {
                    for (int r = 0; r < 2; r++) {
                        ex.submit(() -> { int n; while ((n = relayOnce(ds, producer, false)) > 0) published.addAndGet(n); return null; });
                    }
                }
                System.out.println("relay B+C published+marked: " + published.get());
            }

            long inTopic = countTopic(bootstrap);
            System.out.println("messages in topic: " + inTopic + " (outbox rows: " + ok.get() + ")");

            // 4) consumer idempotent doc het
            Properties cp = new Properties();
            cp.put("bootstrap.servers", bootstrap);
            cp.put("group.id", "notification-" + System.nanoTime());
            cp.put("key.deserializer", StringDeserializer.class.getName());
            cp.put("value.deserializer", StringDeserializer.class.getName());
            cp.put("auto.offset.reset", "earliest");
            cp.put("enable.auto.commit", "false");
            var skipped = new AtomicInteger();
            int applied = 0;
            try (var consumer = new KafkaConsumer<String, String>(cp)) {
                consumer.subscribe(List.of(TOPIC));
                long deadline = System.currentTimeMillis() + 30_000;
                while (applied + skipped.get() < inTopic && System.currentTimeMillis() < deadline) {
                    applied += consumeIdempotent(ds, consumer, skipped);
                }
            }
            System.out.println("consumer: applied=" + applied + " duplicates skipped=" + skipped.get());

            try (Connection c = ds.getConnection()) {
                System.out.println("sum(balance) = " + query(c, "select sum(balance) from account") + " (ban dau 100000000)");
                System.out.println("sum(ledger)  = " + query(c, "select sum(amount) from ledger_entry"));
                System.out.println("outbox pending = " + query(c, "select count(*) from outbox where published_at is null"));
                System.out.println("notifications = " + query(c, "select count(*) from notification")
                        + ", distinct transfers notified = " + query(c, "select count(distinct transfer_id) from notification"));
            }
        }
    }

    static long countTopic(String bootstrap) {
        Properties cp = new Properties();
        cp.put("bootstrap.servers", bootstrap);
        cp.put("key.deserializer", StringDeserializer.class.getName());
        cp.put("value.deserializer", StringDeserializer.class.getName());
        try (var c = new KafkaConsumer<String, String>(cp)) {
            var parts = c.partitionsFor(TOPIC).stream().map(p -> new TopicPartition(TOPIC, p.partition())).toList();
            return c.endOffsets(parts).values().stream().mapToLong(Long::longValue).sum();
        }
    }

    static void exec(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            ps.executeUpdate();
        }
    }

    static String query(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
