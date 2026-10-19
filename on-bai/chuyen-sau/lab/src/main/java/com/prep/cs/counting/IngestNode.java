package com.prep.cs.counting;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;

/**
 * Mot ingest node (phia sau HTTP): nhan body, giai ma + phan loai (RequestDecoder), doi thanh ban ghi 32 byte,
 * gom vao block theo partition, gui Kafka. Tra loi client CHI KHI block chua request do da duoc Kafka xac nhan
 * (acks=all): "202" nghia la da ben vung, khong phai "da nhan vao RAM".
 *
 * Partition = ham bam cua clientId: moi request cua mot client, ke ca ban retry gui qua node khac, roi vao CUNG
 * partition, nen aggregator khu trung bang state cuc bo cua partition do.
 */
public final class IngestNode implements AutoCloseable {

    private final String topic;
    private final int partitions, blockRecords;
    private final Supplier<KeyRegistry> registry;
    private final KafkaProducer<byte[], byte[]> producer;
    private final Buf[] bufs;
    private final ScheduledExecutorService flusher = Executors.newSingleThreadScheduledExecutor();
    private final ThreadLocal<RequestDecoder> decoder = ThreadLocal.withInitial(RequestDecoder::new);
    private final ThreadLocal<Decoded> decoded = ThreadLocal.withInitial(Decoded::new);
    public final AtomicLong accepted = new AtomicLong(), blocksSent = new AtomicLong(), bytesSent = new AtomicLong();
    private volatile boolean crashed;

    /**
     * Block dang mo cua mot partition. Bo dem 64 KB DUNG LAI mai mai: moi lan cat block chi chep phan da dung ra mot
     * mang vua kich thuoc (Kafka can mang rieng vi gui bat dong bo). Ban dau cap phat bo dem moi cho moi block:
     * flush 5 ms x 48 partition = 9.600 bo dem 64 KB/giay, phan lon bo trong (bai 09, do rac moi request).
     */
    private static final class Buf {
        final ByteBuffer bb;
        List<CompletableFuture<Void>> waiters = new ArrayList<>();
        int n;

        Buf(int blockRecords) {
            bb = ByteBuffer.allocate(4 + blockRecords * RequestRecord.SIZE);
            bb.putInt(0);                           // cho so luong ban ghi, dien khi cat block
        }

        /** Goi khi dang giu khoa cua Buf. */
        byte[] cut() {
            bb.putInt(0, n);
            byte[] value = Arrays.copyOf(bb.array(), bb.position());
            bb.position(4);
            n = 0;
            return value;
        }
    }

    public IngestNode(String bootstrap, String topic, int partitions, Supplier<KeyRegistry> registry,
                      int blockRecords, int flushEveryMs) {
        this.topic = topic;
        this.partitions = partitions;
        this.registry = registry;
        this.blockRecords = blockRecords;
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        p.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        p.put(ProducerConfig.BATCH_SIZE_CONFIG, 256 * 1024);
        p.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        p.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 4 * 1024 * 1024);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        this.producer = new KafkaProducer<>(p);
        this.bufs = new Buf[partitions];
        for (int i = 0; i < partitions; i++) bufs[i] = new Buf(blockRecords);
        flusher.scheduleAtFixedRate(this::flushAll, flushEveryMs, flushEveryMs, TimeUnit.MILLISECONDS);
    }

    public static int partitionOf(long clientId, int partitions) {
        return (int) ((Hash128.fmix64(clientId) >>> 1) % partitions);
    }

    /** Nem RequestDecoder.BadRequest ngay (HTTP 400). Future xong khi Kafka xac nhan block (HTTP 202). */
    public CompletableFuture<Void> submit(byte[] body) {
        if (crashed) throw new IllegalStateException("connection refused"); // node chet: client biet ngay
        Decoded d = decoded.get();
        decoder.get().decode(body, 0, body.length, registry.get(), d);
        CompletableFuture<Void> done = new CompletableFuture<>();
        int part = partitionOf(d.clientId, partitions);
        Buf buf = bufs[part];
        byte[] full = null;
        List<CompletableFuture<Void>> fullWaiters = null;
        synchronized (buf) {
            RequestRecord.write(buf.bb, d);
            buf.waiters.add(done);
            if (++buf.n == blockRecords) {          // day block: cat ra trong khoa, gui ngoai khoa
                full = buf.cut();
                fullWaiters = buf.waiters;
                buf.waiters = new ArrayList<>();
            }
        }
        accepted.incrementAndGet();
        if (full != null) send(part, full, fullWaiters);
        return done;
    }

    private void flushAll() {
        for (int part = 0; part < partitions; part++) {
            Buf buf = bufs[part];
            byte[] value;
            List<CompletableFuture<Void>> waiters;
            synchronized (buf) {
                if (buf.n == 0) continue;
                value = buf.cut();
                waiters = buf.waiters;
                buf.waiters = new ArrayList<>();
            }
            send(part, value, waiters);
        }
    }

    private void send(int part, byte[] value, List<CompletableFuture<Void>> waiters) {
        if (crashed) return;                        // chet: block trong RAM mat, client se het gio va retry
        blocksSent.incrementAndGet();
        bytesSent.addAndGet(value.length);
        producer.send(new ProducerRecord<>(topic, part, null, value), (md, e) -> {
            for (CompletableFuture<Void> f : waiters) {
                if (e == null) f.complete(null);
                else f.completeExceptionally(e);    // HTTP 503: client retry cung (clientId, epoch, seq)
            }
        });
    }

    /** Gia lap node chet: moi request dang nam trong block chua gui se khong bao gio duoc tra loi. */
    public void crash() {
        crashed = true;
        flusher.shutdownNow();
        producer.close(java.time.Duration.ZERO);
    }

    @Override
    public void close() {
        if (crashed) return;
        flusher.shutdown();
        flushAll();
        producer.close();
    }
}
