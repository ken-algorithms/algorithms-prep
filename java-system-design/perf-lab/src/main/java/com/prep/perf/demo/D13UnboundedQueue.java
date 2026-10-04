package com.prep.perf.demo;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * P13 - Hang doi KHONG gioi han truoc thread pool khi qua tai.
 *
 * <p>{@code Executors.newFixedThreadPool(n)} dung LinkedBlockingQueue vo han. Khi tai den vuot
 * nang luc xu ly, khong ai bi tu choi - hang doi cu dai ra, latency cua MOI request tang tuyen tinh
 * theo thoi gian, va client (timeout 1 s) da bo di tu lau trong khi server van cam cui xu ly cho
 * nguoi khong con cho: may chay het cong suat ma "goodput" gan bang 0. Cuoi cung la OOM.
 *
 * <p>Sua: hang doi co gioi han + tu choi som (HTTP 503 / 429 + Retry-After). Request duoc nhan thi
 * nhanh; request bi tu choi thi biet NGAY de retry noi khac.
 *
 * <p>Nang luc: 4 worker x 20 ms = 200 req/s. Tai den: 300 req/s trong 4 giay (qua tai 1.5x).
 */
public final class D13UnboundedQueue {

    private D13UnboundedQueue() {}

    public record Result(String mode, int offered, int accepted, int rejected, String acceptedLatency,
                         int maxQueueDepth, long goodputWithin1s) {}

    public static Result run(Integer queueCapacity, int rps, int seconds, int workers, long serviceMs) {
        BlockingQueue<Runnable> q = queueCapacity == null ? new LinkedBlockingQueue<>() : new ArrayBlockingQueue<>(queueCapacity);
        var pool = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.MILLISECONDS, q,
                new ThreadPoolExecutor.AbortPolicy());
        var lat = new Lat();
        var rejected = new AtomicInteger();
        int maxDepth = 0;
        int total = rps * seconds;
        long intervalNs = 1_000_000_000L / rps;
        long start = System.nanoTime();
        for (int i = 0; i < total; i++) {
            long wait = start + i * intervalNs - System.nanoTime();
            if (wait > 0) {
                LockSupport.parkNanos(wait);
            }
            long arrived = System.nanoTime();
            try {
                pool.execute(() -> {
                    Lat.sleepMs(serviceMs);
                    lat.add(System.nanoTime() - arrived);
                });
            } catch (RejectedExecutionException e) {
                rejected.incrementAndGet();          // -> 503 ngay, client retry co backoff
            }
            maxDepth = Math.max(maxDepth, q.size());
        }
        pool.shutdown();
        try {
            pool.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new Result(queueCapacity == null ? "unbounded queue" : "bounded queue " + queueCapacity + " + reject",
                total, lat.count(), rejected.get(), lat.summary(), maxDepth, lat.count() - lat.countOver(1000));
    }

    public static void main(String[] args) {
        System.out.println("capacity 4 workers x 20ms = 200 req/s; offered 300 req/s x 4 s; client timeout 1 s");
        System.out.println(run(null, 300, 4, 4, 20));
        System.out.println(run(40, 300, 4, 4, 20));
    }
}
