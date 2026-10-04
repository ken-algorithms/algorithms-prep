package com.prep.perf.demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * P12 - Virtual thread bi GHIM (pinning) khi blocking ben trong {@code synchronized} - Java 21-23.
 *
 * <p>Moi task co khoa RIENG (khong ai tranh khoa voi ai) - nen moi khac biet la do pinning:
 * tren JDK 21, sleep/IO ben trong synchronized khong unmount duoc -> carrier thread (= so core)
 * bi chiem -> 1000 task x 20 ms chay theo lo bang so core.
 *
 * <p>Tu JDK 24 (JEP 491) synchronized khong con ghim -> hai ban chay ngang nhau. Demo in version de
 * khong ai hieu lam ket qua. Trong code that, cho hay gap: thu vien cu dung synchronized quanh IO
 * (JDBC driver doi cu, connection pool cu, SDK HTTP). Phat hien: -Djdk.tracePinnedThreads=full (21-23)
 * hoac JFR event jdk.VirtualThreadPinned.
 */
public final class D12Pinning {

    private D12Pinning() {}

    public record Result(String mode, int tasks, long wallMs) {}

    public static Result run(boolean useSynchronized, int tasks, long ioMs) {
        long start = System.nanoTime();
        try (ExecutorService ex = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                Object monitor = new Object();
                ReentrantLock lock = new ReentrantLock();
                ex.submit(() -> {
                    if (useSynchronized) {
                        synchronized (monitor) {
                            Lat.sleepMs(ioMs);          // JDK 21: ghim carrier
                        }
                    } else {
                        lock.lock();
                        try {
                            Lat.sleepMs(ioMs);          // unmount binh thuong
                        } finally {
                            lock.unlock();
                        }
                    }
                });
            }
        }
        return new Result(useSynchronized ? "synchronized around IO" : "ReentrantLock around IO",
                tasks, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    public static void main(String[] args) {
        System.out.printf("JDK %s, cores=%d, 1000 virtual threads x 20ms IO, khoa rieng tung task%n",
                Runtime.version(), Runtime.getRuntime().availableProcessors());
        run(false, 200, 1); // warm-up
        System.out.println(run(true, 1000, 20));
        System.out.println(run(false, 1000, 20));
    }
}
