package com.prep.cs.kafka;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Producer gui deu tay (rate message/giay) de quan sat cum khi broker chet, ISR co lai, leader doi.
 * Moi giay in mot dong: gui bao nhieu, broker xac nhan bao nhieu, loi gi, leader hien tai.
 * Cuoi cung ghi danh sach id DA DUOC XAC NHAN ra file de AckLossCheck doi chieu voi topic.
 * Dung khi het so giay, hoac som hon neu file stop xuat hien (script thi nghiem tao file nay).
 *
 *   scripts/run.sh com.prep.cs.kafka.SteadyProducer localhost:19092 pay all 200 60 results/acked-pay.txt [stop-file]
 */
public final class SteadyProducer {

    public static void main(String[] a) throws Exception {
        String bootstrap = a[0], topic = a[1], acks = a[2];
        int rate = Integer.parseInt(a[3]), seconds = Integer.parseInt(a[4]);
        Path out = Path.of(a[5]);
        Path stop = a.length > 6 ? Path.of(a[6]) : null;

        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.ACKS_CONFIG, acks);
        // Idempotence doi hoi acks=all. Voi acks=1/0 phai tat ro rang, neu khong client bao loi cau hinh.
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, acks.equals("all"));
        p.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        p.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5_000);    // cho mot request toi da 5 giay
        p.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 20_000);  // tong thoi gian ke ca retry, roi bao loi
        p.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5_000);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        ConcurrentLinkedQueue<Long> acked = new ConcurrentLinkedQueue<>();
        AtomicLong ok = new AtomicLong(), maxLatencyMs = new AtomicLong();
        Map<String, AtomicLong> errors = new java.util.concurrent.ConcurrentHashMap<>();
        long sent = 0;
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(p)) {
            long start = System.currentTimeMillis();
            long nextReport = start + 1_000;
            long okBefore = 0, sentBefore = 0;
            Map<String, Long> errBefore = new TreeMap<>();
            while (System.currentTimeMillis() - start < seconds * 1_000L && (stop == null || !Files.exists(stop))) {
                long id = sent++;
                long t0 = System.nanoTime();
                try {
                    producer.send(new ProducerRecord<>(topic, "id-" + id, Long.toString(id)), (md, e) -> {
                        if (e == null) {
                            acked.add(id);
                            ok.incrementAndGet();
                            maxLatencyMs.accumulateAndGet((System.nanoTime() - t0) / 1_000_000, Math::max);
                        } else {
                            errors.computeIfAbsent(e.getClass().getSimpleName(), k -> new AtomicLong()).incrementAndGet();
                        }
                    });
                } catch (Exception e) { // max.block.ms het: buffer day hoac khong lay duoc metadata
                    errors.computeIfAbsent("send:" + e.getClass().getSimpleName(), k -> new AtomicLong()).incrementAndGet();
                }
                // giu nhip: id thu n phai gui luc start + n/rate giay
                long due = start + (sent * 1_000L) / rate;
                long now = System.currentTimeMillis();
                if (due > now) Thread.sleep(due - now);
                if (now >= nextReport) {
                    Map<String, Long> errNow = errors.entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(), (x, y) -> x, TreeMap::new));
                    String errDelta = errNow.entrySet().stream()
                            .filter(e -> e.getValue() > errBefore.getOrDefault(e.getKey(), 0L))
                            .map(e -> e.getKey() + "+" + (e.getValue() - errBefore.getOrDefault(e.getKey(), 0L)))
                            .collect(Collectors.joining(","));
                    System.out.printf("%s t=%2ds gui=%4d xac-nhan=%4d loi=[%s] tre-max=%dms leader=%s%n",
                            java.time.LocalTime.now().withNano(0), (now - start) / 1000,
                            sent - sentBefore, ok.get() - okBefore, errDelta,
                            maxLatencyMs.getAndSet(0), leaders(producer.partitionsFor(topic)));
                    okBefore = ok.get();
                    sentBefore = sent;
                    errBefore.clear();
                    errBefore.putAll(errNow);
                    nextReport += 1_000;
                }
            }
            producer.flush();
        }
        List<Long> ids = new ArrayList<>(acked);
        ids.sort(null);
        writeIds(out, ids);
        System.out.printf("TONG: gui=%d xac-nhan=%d loi=%s -> %s%n", sent, ok.get(), errors, out);
    }

    private static String leaders(List<PartitionInfo> parts) {
        return parts.stream().sorted(java.util.Comparator.comparingInt(PartitionInfo::partition))
                .map(pi -> pi.leader() == null || pi.leader().id() < 0 ? "-" : Integer.toString(pi.leader().id()))
                .collect(Collectors.joining("/"));
    }

    static void writeIds(Path out, List<Long> ids) throws IOException {
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, ids.stream().map(String::valueOf).collect(Collectors.joining("\n")) + "\n");
    }
}
