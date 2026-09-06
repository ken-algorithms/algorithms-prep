package com.prep.spring.bench;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load generator toi gian — KHONG can cai hey/k6/wrk.
 *
 * <p>Dung {@link HttpClient} + VIRTUAL THREADS: mot virtual thread cho moi request dang bay. Voi
 * 200 request dong thoi, ban chi can vai OS thread. Day vua la cong cu do, vua la vi du chay duoc
 * cho module 04.
 *
 * <p>Chay:
 * <pre>{@code
 * java -cp target/classes com.prep.spring.bench.LoadGenerator http://localhost:8080 200 30
 * }</pre>
 *
 * <p><b>Canh bao ve tinh trung thuc cua so do</b> — phai noi ra khi trinh bay ket qua:
 * <ul>
 *   <li>Client va server chay CUNG mot may -> tranh CPU voi nhau. So tuyet doi khong dung voi
 *       production; chi dung de SO SANH hai framework trong cung dieu kien.
 *   <li>Phai co giai doan WARM-UP: JIT chua compile va connection pool chua day thi vai giay dau
 *       cham gap nhieu lan. Ket qua warm-up bi loai khoi thong ke.
 *   <li>Do p50/p95/p99, KHONG do trung binh. Trung binh giau di duoi dai — ma duoi dai moi la thu
 *       nguoi dung phan nan.
 * </ul>
 */
public final class LoadGenerator {

    private LoadGenerator() {}

    public record Report(
            int concurrency,
            int totalRequests,
            int failed,
            Duration wallClock,
            double requestsPerSecond,
            long p50Millis,
            long p95Millis,
            long p99Millis,
            long maxMillis) {

        @Override
        public String toString() {
            return """
                   concurrency : %d
                   requests    : %d (failed: %d)
                   wall clock  : %d ms
                   throughput  : %.1f req/s
                   p50 / p95   : %d ms / %d ms
                   p99 / max   : %d ms / %d ms
                   """
                    .formatted(
                            concurrency, totalRequests, failed, wallClock.toMillis(),
                            requestsPerSecond, p50Millis, p95Millis, p99Millis, maxMillis);
        }
    }

    public static Report run(String baseUrl, int concurrency, int durationSeconds, Duration warmUp) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/runs?size=20"))
                .header("X-Tenant-Id", "bench")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        long warmUpUntil = System.nanoTime() + warmUp.toNanos();
        long stopAt = warmUpUntil + Duration.ofSeconds(durationSeconds).toNanos();

        List<Long> latencies = Collections.synchronizedList(new ArrayList<Long>(100_000));
        var failed = new AtomicInteger();
        long start = System.nanoTime();

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < concurrency; i++) {
                pool.submit(() -> {
                    while (System.nanoTime() < stopAt) {
                        long t0 = System.nanoTime();
                        try {
                            HttpResponse<Void> response =
                                    client.send(request, HttpResponse.BodyHandlers.discarding());
                            long elapsed = System.nanoTime() - t0;
                            if (response.statusCode() >= 400) {
                                failed.incrementAndGet();
                            } else if (System.nanoTime() > warmUpUntil) {
                                // Chi ghi nhan sau warm-up.
                                latencies.add(elapsed / 1_000_000);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        } catch (Exception e) {
                            failed.incrementAndGet();
                        }
                    }
                });
            }
        }

        Duration wall = Duration.ofNanos(System.nanoTime() - start);
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);

        return new Report(
                concurrency,
                sorted.size(),
                failed.get(),
                wall,
                sorted.isEmpty() ? 0 : sorted.size() / (double) durationSeconds,
                percentile(sorted, 0.50),
                percentile(sorted, 0.95),
                percentile(sorted, 0.99),
                sorted.isEmpty() ? 0 : sorted.getLast());
    }

    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(p * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    public static void main(String[] args) {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        int concurrency = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        int seconds = args.length > 2 ? Integer.parseInt(args[2]) : 30;

        System.out.printf("warming up 5s, then measuring %ds at concurrency %d against %s%n",
                seconds, concurrency, baseUrl);
        Report report = run(baseUrl, concurrency, seconds, Duration.ofSeconds(5));
        System.out.println(report);
    }
}
