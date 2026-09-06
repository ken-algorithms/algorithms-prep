package com.prep.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class ConcurrencyTest {

    private static final int THREADS = 8;
    private static final int INCREMENTS_PER_THREAD = 500;
    private static final int EXPECTED = THREADS * INCREMENTS_PER_THREAD;

    /** Chay `action` dong thoi tren THREADS thread, xuat phat cung luc bang CyclicBarrier. */
    private static void runConcurrently(Runnable action) throws InterruptedException {
        var barrier = new CyclicBarrier(THREADS);
        var done = new CountDownLatch(THREADS);
        var threads = new ArrayList<Thread>(THREADS);

        for (int t = 0; t < THREADS; t++) {
            var thread = new Thread(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS); // dong bo diem xuat phat -> tang xac suat dua
                    for (int i = 0; i < INCREMENTS_PER_THREAD; i++) {
                        action.run();
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }
        assertTrue(done.await(30, TimeUnit.SECONDS), "cac thread phai ket thuc");
        for (Thread thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(5));
        }
    }

    @Nested
    @DisplayName("Race condition - mat update mot cach im lang")
    class RaceConditions {

        @Test
        @DisplayName("bo dem KHONG thread-safe lam MAT update (chay lai toi 5 lan de khong flaky)")
        void unsafeCounterLosesUpdates() throws InterruptedException {
            // Race la xac suat. De test khong flaky: thu nhieu lan, chi can MOT lan mat update
            // la da chung minh bo dem khong an toan. Nguoc lai neu 5 lan deu dung thi cung khong
            // the ket luan no an toan - do la ban chat cua bug concurrency, va la ly do vi sao
            // "test chay xanh" KHONG phai bang chung khong co race.
            boolean observedLostUpdate = false;
            int lastObserved = -1;

            for (int attempt = 0; attempt < 5 && !observedLostUpdate; attempt++) {
                var counter = new ResultCounter.Unsafe();
                runConcurrently(counter::increment);
                lastObserved = counter.value();
                if (lastObserved < EXPECTED) {
                    observedLostUpdate = true;
                }
            }

            assertTrue(
                    observedLostUpdate,
                    "khong quan sat duoc lost update (lan cuoi doc %d/%d) - bug van ton tai, chi la khong lo ra"
                            .formatted(lastObserved, EXPECTED));
        }

        @Test
        @DisplayName("synchronized: dung tuyet doi")
        void synchronisedCounterIsCorrect() throws InterruptedException {
            var counter = new ResultCounter.Synchronised();
            runConcurrently(counter::increment);
            assertEquals(EXPECTED, counter.value());
        }

        @Test
        @DisplayName("AtomicLong (CAS): dung tuyet doi, khong lock")
        void atomicCounterIsCorrect() throws InterruptedException {
            var counter = new ResultCounter.Atomic();
            runConcurrently(counter::increment);
            assertEquals(EXPECTED, counter.value());
        }

        @Test
        @DisplayName("LongAdder: dung tuyet doi, toi uu cho nhieu writer")
        void adderCounterIsCorrect() throws InterruptedException {
            var counter = new ResultCounter.Adder();
            runConcurrently(counter::increment);
            assertEquals(EXPECTED, counter.value());
        }
    }

    @Nested
    @DisplayName("Deadlock - phat hien va sua")
    class DeadlockHandling {

        @Test
        @Timeout(30)
        @DisplayName("lock nguoc thu tu -> JVM PHAT HIEN duoc deadlock qua ThreadMXBean")
        void oppositeLockOrderDeadlocks() throws InterruptedException {
            var suite = new Deadlocks.Resource("suite", 1);
            var devicePool = new Deadlocks.Resource("devicePool", 2);
            var ready = new CountDownLatch(2);

            // Thread A: suite -> devicePool. Thread B: devicePool -> suite. Circular wait.
            var a = new Thread(Deadlocks.deadlockProne(suite, devicePool, ready), "run-A");
            var b = new Thread(Deadlocks.deadlockProne(devicePool, suite, ready), "run-B");
            a.setDaemon(true);
            b.setDaemon(true);
            a.start();
            b.start();

            // Day la KY THUAT DANG GIA nhat trong file nay: ThreadMXBean tu tim ra deadlock.
            // Trong production ban dung dung cach nay (hoac jstack / JFR) de chan doan request treo.
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            long[] deadlocked = null;
            for (int i = 0; i < 60 && deadlocked == null; i++) {
                Thread.sleep(100);
                deadlocked = threadBean.findDeadlockedThreads();
            }

            assertNotNull(deadlocked, "phai phat hien duoc deadlock");
            assertEquals(2, deadlocked.length, "dung 2 thread khoa nhau");

            // Khong the "sua" deadlock luc runtime - chi co the restart. Do la ly do phai chan
            // no o thiet ke (lock ordering), khong phai xu ly luc chay.
            a.interrupt();
            b.interrupt();
        }

        @Test
        @Timeout(30)
        @DisplayName("LOCK ORDERING: cung 2 resource, lock theo thu tu co dinh -> khong bao gio deadlock")
        void lockOrderingPreventsDeadlock() throws InterruptedException {
            var suite = new Deadlocks.Resource("suite", 1);
            var devicePool = new Deadlocks.Resource("devicePool", 2);
            var counter = new ResultCounter.Atomic();

            // CO Y truyen thu tu tham so nguoc nhau giua 2 thread - dung cai da gay deadlock o tren.
            // lockOrdered() tu sap xep lai theo order() nen khong con circular wait.
            var a = new Thread(Deadlocks.lockOrdered(suite, devicePool, counter::increment), "ordered-A");
            var b = new Thread(Deadlocks.lockOrdered(devicePool, suite, counter::increment), "ordered-B");
            a.start();
            b.start();
            a.join(TimeUnit.SECONDS.toMillis(10));
            b.join(TimeUnit.SECONDS.toMillis(10));

            assertFalse(a.isAlive(), "thread A phai ket thuc");
            assertFalse(b.isAlive(), "thread B phai ket thuc");
            assertEquals(2, counter.value());
            assertNotNull(ManagementFactory.getThreadMXBean());
        }

        @Test
        @Timeout(30)
        @DisplayName("tryLock co timeout: khong deadlock, nhung CO THE that bai -> caller phai xu ly")
        void tryLockGivesUpInsteadOfHanging() throws InterruptedException {
            var a = new Deadlocks.Resource("a", 1);
            var b = new Deadlocks.Resource("b", 2);

            // Giu lock b tu mot thread khac de tryBothOrGiveUp khong the lay du ca hai.
            var holderStarted = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            var holder = new Thread(() -> {
                b.lock().lock();
                try {
                    holderStarted.countDown();
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    b.lock().unlock();
                }
            }, "holder");
            holder.setDaemon(true);
            holder.start();
            assertTrue(holderStarted.await(5, TimeUnit.SECONDS));

            // Lay duoc a, khong lay duoc b trong 100ms -> tra false, KHONG treo.
            boolean succeeded =
                    Deadlocks.tryBothOrGiveUp(a, b, Duration.ofMillis(100), () -> {});
            assertFalse(succeeded, "phai bo cuoc thay vi treo");

            // Va quan trong: lock a da duoc NHA ra (khong hold-and-wait).
            assertTrue(a.lock().tryLock(), "lock a phai duoc nha sau khi bo cuoc");
            a.lock().unlock();

            release.countDown();
        }
    }

    @Nested
    @DisplayName("Virtual threads (Java 21)")
    class VirtualThreadBehaviour {

        @Test
        @Timeout(60)
        @DisplayName("2000 task blocking chay gan nhu SONG SONG HET tren virtual thread")
        void virtualThreadsScaleForBlockingWork() {
            int tasks = 2_000;
            var blockFor = Duration.ofMillis(100);

            var measurement = VirtualThreads.runBlockingOnVirtualThreads(tasks, blockFor);

            assertEquals(tasks, measurement.completed());
            assertEquals(0, measurement.failed());

            // 2000 task x 100ms. Neu tuan tu: 200s. Neu pool 8 thread: ~25s.
            // Virtual thread: gan 100ms (cong overhead). Nguong 10s la rat rong nhung du de
            // chung minh chung KHONG bi gioi han boi so OS thread.
            assertTrue(
                    measurement.elapsed().toMillis() < 10_000,
                    "mat %d ms - qua lau, virtual thread dang khong unmount nhu ky vong"
                            .formatted(measurement.elapsed().toMillis()));
        }

        @Test
        @Timeout(120)
        @DisplayName("cung khoi luong tren pool 8 platform thread thi CHAM HON ro ret")
        void platformPoolIsSlowerForBlockingWork() {
            int tasks = 200;
            var blockFor = Duration.ofMillis(100);

            var onPlatform = VirtualThreads.runBlockingOnPlatformPool(tasks, blockFor, 8);
            var onVirtual = VirtualThreads.runBlockingOnVirtualThreads(tasks, blockFor);

            assertEquals(tasks, onPlatform.completed());
            assertEquals(tasks, onVirtual.completed());

            // 200 task / 8 thread = 25 dot x 100ms = ~2.5s tren platform pool.
            // Tren virtual thread: ~100-300ms. So sanh dinh tinh de tranh flaky tren may khac nhau.
            assertTrue(
                    onVirtual.elapsed().toMillis() < onPlatform.elapsed().toMillis(),
                    "virtual=%d ms, platform=%d ms"
                            .formatted(onVirtual.elapsed().toMillis(), onPlatform.elapsed().toMillis()));
        }

        @Test
        @Timeout(30)
        @DisplayName("mot task loi KHONG lam chet cac task khac, va loi KHONG bien mat")
        void failuresAreCollectedNotSwallowed() {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                int index = i;
                tasks.add(() -> {
                    if (index % 5 == 0) {
                        throw new IllegalStateException("task " + index + " failed");
                    }
                    return "ok-" + index;
                });
            }

            var partitioned = VirtualThreads.runAllCollectingFailures(tasks);

            assertEquals(16, partitioned.results().size(), "cac task tot van phai xong");
            assertEquals(4, partitioned.failures().size(), "loi phai duoc GIU LAI, khong bien mat");
            assertTrue(partitioned.failures().stream().allMatch(IllegalStateException.class::isInstance));
            // getCause() da duoc mo ra -> khong phai ExecutionException boc ngoai.
            assertTrue(partitioned.failures().getFirst().getMessage().contains("failed"));
        }

        @Test
        @DisplayName("virtual thread la daemon va khong thuoc thread group nao dac biet")
        void virtualThreadProperties() throws Exception {
            var thread = Thread.ofVirtual().unstarted(() -> {});
            assertTrue(thread.isVirtual());
            assertTrue(thread.isDaemon(), "virtual thread LUON la daemon - khong giu JVM song");

            // Bay: khong the set priority hay daemon=false cho virtual thread.
            assertFalse(Thread.ofVirtual().unstarted(() -> {}).isAlive());

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Boolean isVirtual = executor.submit(() -> Thread.currentThread().isVirtual()).get();
                assertTrue(isVirtual);
            }
        }
    }
}
