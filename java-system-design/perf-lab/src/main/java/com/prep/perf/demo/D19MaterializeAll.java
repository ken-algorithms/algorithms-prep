package com.prep.perf.demo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * P19 - Nap HET du lieu vao bo nho roi moi xu ly (findAll(), readAllBytes, List&lt;String&gt; lines).
 *
 * <pre>
 * XAU   List&lt;Txn&gt; all = repo.findAll();  String csv = all.stream().map(..).collect(joining());
 * SUA   try (Stream&lt;Txn&gt; s = repo.streamAll()) { s.forEach(t -&gt; writer.write(..)); }   // fetchSize
 * </pre>
 *
 * Tong byte cap phat gan nhu nhau - khac biet nam o LUONG SONG cung luc (peak heap). 1 trieu dong
 * song cung luc -> chui len old gen -> GC dai; 5 nguoi bam "Export" cung luc la x5. Ban stream giu
 * vai KB song tai moi thoi diem, bat ke bang co 1 trieu hay 1 ty dong.
 */
public final class D19MaterializeAll {

    private D19MaterializeAll() {}

    public record Txn(long id, String account, long amountMinor, String currency, String note) {}

    public record Result(String mode, int rows, long wallMs, long peakHeapMb, long bytesWritten) {}

    static Stream<Txn> source(int rows) {
        return LongStream.rangeClosed(1, rows).mapToObj(i ->
                new Txn(i, "101-" + (22_000_000 + i % 50_000), i * 137 % 10_000_000, "VND", "transfer #" + i));
    }

    static String toCsv(Txn t) {
        return t.id() + "," + t.account() + "," + t.amountMinor() + "," + t.currency() + "," + t.note() + "\n";
    }

    /** Dem byte, khong giu lai - dong vai tro HTTP response body. */
    static final class CountingSink extends OutputStream {
        long count;

        @Override
        public void write(int b) {
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) {
            count += len;
        }
    }

    public static Result exportBad(int rows) throws IOException {
        resetPeaks();
        long t0 = System.nanoTime();
        List<Txn> all = new ArrayList<>(source(rows).toList());     // findAll()
        StringBuilder sb = new StringBuilder();
        for (Txn t : all) {
            sb.append(toCsv(t));
        }
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8); // ResponseEntity<byte[]>
        var sink = new CountingSink();
        sink.write(body, 0, body.length);
        return new Result("materialize all", rows, ms(t0), peakHeapMb(), sink.count);
    }

    public static Result exportGood(int rows) throws IOException {
        resetPeaks();
        long t0 = System.nanoTime();
        var sink = new CountingSink();
        try (Writer w = new OutputStreamWriter(sink, StandardCharsets.UTF_8); Stream<Txn> s = source(rows)) {
            s.forEach(t -> {
                try {
                    w.write(toCsv(t));
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        }
        return new Result("stream row by row", rows, ms(t0), peakHeapMb(), sink.count);
    }

    private static long ms(long t0) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
    }

    private static void resetPeaks() {
        System.gc();
        ManagementFactory.getMemoryPoolMXBeans().forEach(MemoryPoolMXBean::resetPeakUsage);
    }

    private static long peakHeapMb() {
        return ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(p -> p.getType() == MemoryType.HEAP)
                .mapToLong(p -> p.getPeakUsage().getUsed()).sum() >> 20;
    }

    public static void main(String[] args) throws IOException {
        int rows = 1_000_000;
        System.out.printf("export %,d transactions to CSV, -Xmx=%dMB%n", rows, Runtime.getRuntime().maxMemory() >> 20);
        exportGood(50_000);  // warm-up
        exportBad(50_000);
        System.out.println(exportBad(rows));
        System.out.println(exportGood(rows));
    }
}
