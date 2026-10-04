package com.prep.perf.demo;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * P10 - Goi downstream KHONG co timeout. Mot doi tac cham keo sap ca endpoint khong lien quan.
 *
 * <p>Server mo phong Tomcat: 20 worker thread, hang doi accept phia truoc. Tai 200 req/s trong 3 giay:
 * 90% la GET /balance (5 ms, chi doc DB), 10% la POST /transfer goi doi tac - doi tac dang suy
 * giam, tra loi sau 2 giay.
 *
 * <pre>
 * XAU   RestTemplate/HttpClient mac dinh: khong read timeout -> thread /transfer bi giu 2 s
 *       20 req/s x 2 s = can 40 thread chi de "cho" -> 20 thread het sach -> /balance xep hang
 * SUA   read timeout 200 ms + fallback (tra 202 "dang xu ly", doi soat sau)
 *       20 req/s x 0.2 s = 4 thread -> /balance khong biet la co su co
 * </pre>
 * Timeout duoc mo phong bang sleep(min(do tre doi tac, timeout)) - dung ngu nghia cua read timeout.
 */
public final class D10NoTimeout {

    private D10NoTimeout() {}

    public record Result(String mode, int balanceRequests, String balanceLatency, long balanceOver1s,
                         int transferRequests, int transferFallbacks) {}

    public static Result run(Long partnerTimeoutMs, int rps, int seconds, int workers, long partnerLatencyMs) {
        var server = (ThreadPoolExecutor) Executors.newFixedThreadPool(workers);
        var balance = new Lat();
        var transfers = new AtomicInteger();
        var fallbacks = new AtomicInteger();
        int total = rps * seconds;
        long intervalNs = 1_000_000_000L / rps;
        long start = System.nanoTime();

        for (int i = 0; i < total; i++) {
            long due = start + i * intervalNs;
            long wait = due - System.nanoTime();
            if (wait > 0) {
                java.util.concurrent.locks.LockSupport.parkNanos(wait);
            }
            boolean isTransfer = i % 10 == 0;
            long arrived = System.nanoTime();
            server.execute(() -> {
                if (isTransfer) {
                    transfers.incrementAndGet();
                    long waited = partnerTimeoutMs == null ? partnerLatencyMs : Math.min(partnerLatencyMs, partnerTimeoutMs);
                    Lat.sleepMs(waited);
                    if (waited < partnerLatencyMs) {
                        fallbacks.incrementAndGet();   // SocketTimeoutException -> 202 + doi soat
                    }
                } else {
                    Lat.sleepMs(5);
                    balance.add(System.nanoTime() - arrived);   // gom ca thoi gian xep hang
                }
            });
        }
        server.shutdown();
        try {
            server.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new Result(partnerTimeoutMs == null ? "no timeout" : "timeout " + partnerTimeoutMs + "ms + fallback",
                balance.count(), balance.summary(), balance.countOver(1000), transfers.get(), fallbacks.get());
    }

    public static void main(String[] args) {
        System.out.println("workers=20, 200 req/s x 3 s, 10% /transfer -> partner latency 2000ms");
        System.out.println(run(null, 200, 3, 20, 2000));
        System.out.println(run(200L, 200, 3, 20, 2000));
    }
}
