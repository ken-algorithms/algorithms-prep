package com.prep.cs.counting;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * API that de curl duoc va do chi phi tang HTTP. JDK HttpServer + virtual thread (khong framework):
 *   POST /events            -> 202 khi Kafka da xac nhan; 400 body hong; 503 Kafka loi (client retry cung body)
 *   GET  /stats?from=&to=   -> so dem trong [from, to) (ISO-8601), doc tu Postgres
 *   GET  /metrics           -> so request + CPU ns cua TIEN TRINH nay, de tinh CPU/request
 * Che do "noop": tra 202 ngay khong lam gi — do rieng chi phi HTTP.
 *
 *   scripts/run.sh com.prep.cs.counting.HttpIngestServer 8080 ingest localhost:19092,localhost:29092,localhost:39092
 */
public final class HttpIngestServer {

    public static void main(String[] a) throws Exception {
        int port = Integer.parseInt(a[0]);
        boolean noop = a[1].equals("noop");
        String bs = a.length > 2 ? a[2] : "localhost:19092";
        String topic = "http-events", group = "http-agg";
        int partitions = 6;
        AtomicLong requests = new AtomicLong();
        IngestNode node = null;
        CountStore store = null;
        if (!noop) {
            try (Admin admin = Admin.create(Map.of("bootstrap.servers", bs))) {
                if (!admin.listTopics().names().get().contains(topic)) {
                    admin.createTopics(List.of(new NewTopic(topic, partitions, (short) 3))).all().get();
                }
            }
            List<String> keys = IntStream.range(0, 1_000_000).mapToObj(k -> "feature_" + k).toList();
            KeyRegistry reg = KeyRegistry.of(keys, new Hash128(1, 2), 1);
            node = new IngestNode(bs, topic, partitions, () -> reg, 2_000, 5);
            store = new CountStore(CountStore.dataSource("jdbc:postgresql://localhost:55432/lab", "lab", "lab"));
            store.createSchema();
            new Aggregator("http-agg-1", bs, topic, group, store, 500, 15 * 60_000).start();
        }
        IngestNode ingest = node;
        CountStore stats = store;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 4_096);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/events", ex -> {
            byte[] body = ex.getRequestBody().readAllBytes();
            requests.incrementAndGet();
            if (noop) {
                reply(ex, 202, "");
                return;
            }
            try {
                ingest.submit(body).get(5, TimeUnit.SECONDS);   // tra loi SAU khi Kafka xac nhan
                reply(ex, 202, "");
            } catch (RequestDecoder.BadRequest e) {
                reply(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                reply(ex, 503, "{\"error\":\"thu lai cung body\"}");
            }
        });
        server.createContext("/stats", ex -> {
            Map<String, String> q = query(ex.getRequestURI().getRawQuery());
            try {
                long from = Instant.parse(q.get("from")).getEpochSecond(), to = Instant.parse(q.get("to")).getEpochSecond();
                Stats s = stats.query(from, to);
                reply(ex, 200, String.format("{\"trueCount\":%d,\"falseCount\":%d,\"fakeKey\":%d,\"malformed\":%d,"
                                + "\"successRate\":%.4f,\"bucketsRead\":%d}", s.trueCount(), s.falseCount(), s.fakeKey(),
                        s.malformed(), s.successRate(), s.bucketsRead()));
            } catch (Exception e) {
                reply(ex, 400, "{\"error\":\"from/to phai la ISO-8601\"}");
            }
        });
        server.createContext("/metrics", ex -> {
            long cpu = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
                    .getProcessCpuTime();
            reply(ex, 200, "{\"requests\":" + requests.get() + ",\"cpuNanos\":" + cpu + "}");
        });
        server.start();
        System.out.printf("nghe cong %d, che do %s%n", port, noop ? "noop (chi HTTP)" : "ingest + aggregator");
    }

    static void reply(HttpExchange ex, int code, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, b.length == 0 ? -1 : b.length);
        if (b.length > 0) {
            try (OutputStream os = ex.getResponseBody()) {
                os.write(b);
            }
        }
        ex.close();
    }

    static Map<String, String> query(String raw) {
        Map<String, String> m = new java.util.HashMap<>();
        if (raw == null) return m;
        for (String kv : raw.split("&")) {
            int i = kv.indexOf('=');
            if (i > 0) m.put(kv.substring(0, i), java.net.URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
        }
        return m;
    }
}
