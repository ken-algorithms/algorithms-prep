package com.prep.concurrency;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Virtual Threads (Java 21, JEP 444) - phan chac chan bi hoi vi JD Katalon la Java 21.
 *
 * <p><b>Mo hinh:</b> virtual thread khong map 1-1 vao OS thread. JVM giu mot pool nho cac
 * <i>carrier thread</i> (platform thread, mac dinh = so core). Khi virtual thread cham mot phep
 * BLOCKING co nhan thuc (socket, sleep, lock cua java.util.concurrent), JVM <i>unmount</i> no khoi
 * carrier va cho carrier chay viec khac. Nho vay 10.000 task blocking chi can vai OS thread.
 *
 * <p><b>Ba quy tac phai noi dung khi phong van:</b>
 * <ol>
 *   <li><b>DUNG pool virtual thread.</b> Chung re nhu object - tao moi task mot cai. Pool ton tai
 *       de tai su dung tai nguyen DAT; virtual thread khong dat. Pool chung con lam mat tac dung
 *       cua chinh no. Dung {@code newVirtualThreadPerTaskExecutor()}.
 *   <li><b>Chung chi giup IO-bound, KHONG giup CPU-bound.</b> Task tinh toan thuan khong bao gio
 *       unmount -> so viec chay song song van bi gioi han boi so core. Voi CPU-bound van dung
 *       {@code newFixedThreadPool(so_core)}.
 *   <li><b>Van can gioi han so viec dong thoi cham vao downstream.</b> Virtual thread bo gioi han
 *       ve THREAD, khong bo gioi han ve CONNECTION POOL hay QPS cua service phia sau. 10.000
 *       virtual thread dam vao mot connection pool 20 slot = 9.980 cai xep hang. Van phai co
 *       Semaphore / rate limiter. Day la bay ma nhieu nguoi bo qua.
 * </ol>
 *
 * <p><b>PINNING - luu y quan trong ve phien ban:</b> Tren <b>Java 21</b>, khoi {@code synchronized}
 * ma chua diem blocking se <i>ghim</i> (pin) virtual thread vao carrier: carrier khong duoc giai
 * phong -> mat het loi ich, va neu du nhieu thread bi ghim thi ung dung dung hinh. Cach tranh o
 * Java 21: dung {@link java.util.concurrent.locks.ReentrantLock} thay cho {@code synchronized}
 * trong doan co IO.
 *
 * <p>Tu <b>Java 24</b> (JEP 491) van de nay da duoc xu ly - {@code synchronized} khong con ghim.
 * May ban dang chay JDK 25 nen ban se <b>KHONG</b> quan sat duoc pinning bang thuc nghiem o day.
 * Nhung Katalon dung Java 21, nen cau tra loi dung khi phong van la cau tra loi cua 21. Doan nay
 * co y khong viet thanh test: mot test "chung minh pinning" se pass tren JDK 21 va fail tren JDK 25,
 * tuc la mot test doi tra.
 */
public final class VirtualThreads {

    private VirtualThreads() {}

    /** Ket qua do dac de test assert duoc. */
    public record Measurement(int completed, int failed, Duration elapsed, int peakThreadEstimate) {}

    /**
     * Chay {@code taskCount} task BLOCKING (moi task ngu {@code blockFor}) tren virtual thread.
     *
     * <p>Dung {@code try-with-resources}: {@code ExecutorService} implement {@code AutoCloseable}
     * tu Java 19 - {@code close()} se shutdown va CHO tat ca task xong. Khong con phai viet
     * {@code shutdown() + awaitTermination()} thu cong.
     */
    public static Measurement runBlockingOnVirtualThreads(int taskCount, Duration blockFor) {
        var completed = new AtomicInteger();
        var failed = new AtomicInteger();
        long start = System.nanoTime();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(blockFor.toMillis()); // diem unmount
                        completed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failed.incrementAndGet();
                    }
                });
            }
        } // close() = shutdown + cho xong

        return new Measurement(
                completed.get(),
                failed.get(),
                Duration.ofNanos(System.nanoTime() - start),
                Thread.activeCount());
    }

    /** Cung khoi luong nhung tren pool platform thread co dinh - de so sanh. */
    public static Measurement runBlockingOnPlatformPool(int taskCount, Duration blockFor, int poolSize) {
        var completed = new AtomicInteger();
        var failed = new AtomicInteger();
        long start = System.nanoTime();

        ThreadFactory factory = Thread.ofPlatform().name("worker-", 0).factory();
        try (ExecutorService executor = Executors.newFixedThreadPool(poolSize, factory)) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(blockFor.toMillis());
                        completed.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failed.incrementAndGet();
                    }
                });
            }
        }

        return new Measurement(
                completed.get(),
                failed.get(),
                Duration.ofNanos(System.nanoTime() - start),
                poolSize);
    }

    /**
     * Mot task loi KHONG duoc lam chet cac task khac, va KHONG duoc bien mat im lang.
     *
     * <p>Bay pho bien: {@code executor.submit(runnable)} nuot exception vao Future. Neu khong ai
     * goi {@code future.get()}, exception mat tich hoan toan - khong log, khong metric.
     * Day la nguyen nhan that su cua rat nhieu "job chay ma khong ra ket qua".
     *
     * <p>Cach dung o day: thu het Future roi doc TUNG cai, phan loai thanh cong/that bai.
     */
    public static <T> Partitioned<T> runAllCollectingFailures(List<Callable<T>> tasks) {
        List<T> results = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<T>> futures = new ArrayList<>(tasks.size());
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(task));
            }
            for (Future<T> future : futures) {
                try {
                    results.add(future.get());
                } catch (java.util.concurrent.ExecutionException e) {
                    failures.add(e.getCause()); // getCause() moi la loi that
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failures.add(e);
                }
            }
        }
        return new Partitioned<>(List.copyOf(results), List.copyOf(failures));
    }

    public record Partitioned<T>(List<T> results, List<Throwable> failures) {}
}
