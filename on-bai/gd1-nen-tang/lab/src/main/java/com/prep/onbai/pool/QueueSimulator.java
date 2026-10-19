package com.prep.onbai.pool;

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Mo phong su kien roi rac (khong sleep, khong thread, ket qua tat dinh theo seed) cua MOT pool:
 * c "server" (connection/thread), request den theo Poisson, thoi gian giu theo phan phoi mu, hang doi FIFO,
 * request cho qua maxWaitMs thi bo cuoc (dung nhu Hikari connectionTimeout) va KHONG chiem server.
 * <p>
 * FIFO + "server ranh som nhat" la chinh xac: thoi diem bat dau cua mot request chi phu thuoc cac request
 * den TRUOC no, nen quyet dinh bo cuoc tinh ngay luc den.
 */
public final class QueueSimulator {

    public record Config(double arrivalsPerSec, double meanHoldMs, int servers, double maxWaitMs, int requests, long seed) {

        public double utilization() {
            return arrivalsPerSec * meanHoldMs / 1000.0 / servers;
        }
    }

    public record Result(double offeredUtilization, long completed, long timedOut, double throughputPerSec,
                         double p50Ms, double p99Ms, double p99WaitMs) {

        @Override
        public String toString() {
            return String.format("rho=%.2f thong luong=%.0f/s p50=%.1fms p99=%.1fms (cho p99=%.1fms) bo cuoc=%d",
                    offeredUtilization, throughputPerSec, p50Ms, p99Ms, p99WaitMs, timedOut);
        }
    }

    private QueueSimulator() {}

    public static Result run(Config c) {
        Random rnd = new Random(c.seed());
        PriorityQueue<Double> freeAt = new PriorityQueue<>();
        for (int i = 0; i < c.servers(); i++) {
            freeAt.add(0.0);
        }
        double[] latency = new double[c.requests()];
        double[] wait = new double[c.requests()];
        int done = 0;
        long timedOut = 0;
        double now = 0, lastEnd = 0;
        for (int i = 0; i < c.requests(); i++) {
            now += exponential(rnd, 1000.0 / c.arrivalsPerSec()); // thoi diem request i den (ms)
            double start = Math.max(now, freeAt.peek());
            if (start - now > c.maxWaitMs()) {
                timedOut++;                                       // cho qua lau: bo cuoc, server khong bi chiem
                continue;
            }
            freeAt.poll();
            double end = start + exponential(rnd, c.meanHoldMs());
            freeAt.add(end);
            wait[done] = start - now;
            latency[done++] = end - now;
            lastEnd = Math.max(lastEnd, end);
        }
        double[] lat = Arrays.copyOf(latency, done), w = Arrays.copyOf(wait, done);
        Arrays.sort(lat);
        Arrays.sort(w);
        double horizonSec = Math.max(now, lastEnd) / 1000.0;
        return new Result(c.utilization(), done, timedOut, done / horizonSec, pct(lat, 0.50), pct(lat, 0.99), pct(w, 0.99));
    }

    private static double exponential(Random rnd, double mean) {
        return -mean * Math.log(1 - rnd.nextDouble());
    }

    private static double pct(double[] sorted, double p) {
        return sorted.length == 0 ? 0 : sorted[Math.min(sorted.length - 1, (int) Math.ceil(p * sorted.length) - 1)];
    }
}
