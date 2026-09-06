package com.prep.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Deadlock: nguyen nhan va CACH SUA DUY NHAT dang tin cay o quy mo lon - LOCK ORDERING.
 *
 * <p>Boi canh Katalon: mot test run giu lock tren "suite" roi xin lock tren "device pool"; mot run
 * khac lam nguoc lai. Duoi tai thap khong bao gio gap. Duoi tai cao thi treo, va treo im lang -
 * khong exception, khong log, chi la request khong bao gio tra loi.
 *
 * <p>Bon dieu kien Coffman de co deadlock: (1) mutual exclusion, (2) hold-and-wait,
 * (3) no preemption, (4) circular wait. Pha VO BAT KY MOT dieu kien la het deadlock.
 * Lock ordering pha dieu kien (4). {@code tryLock} co timeout pha dieu kien (3).
 */
public final class Deadlocks {

    private Deadlocks() {}

    /** Hai resource can lock. Ten co thu tu de minh hoa lock ordering. */
    public static final class Resource {
        private final String name;
        private final int order; // thu tu toan cuc, dung de sap xep truoc khi lock
        private final ReentrantLock lock = new ReentrantLock();

        public Resource(String name, int order) {
            this.name = name;
            this.order = order;
        }

        public String name() {
            return name;
        }

        public int order() {
            return order;
        }

        public ReentrantLock lock() {
            return lock;
        }
    }

    /**
     * CACH SAI: lock theo thu tu tuy y cua tung caller -> circular wait.
     *
     * <p>{@code ready} latch dam bao ca hai thread da giu lock thu nhat TRUOC khi xin lock thu hai,
     * nen deadlock xay ra chac chan - dung de test duoc, khong flaky.
     */
    public static Runnable deadlockProne(Resource first, Resource second, CountDownLatch ready) {
        return () -> {
            first.lock().lock();
            try {
                ready.countDown();
                try {
                    // Doi thread kia cung giu duoc lock dau tien cua no -> dam bao circular wait.
                    ready.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                second.lock().lock(); // <-- treo o day mai mai
                try {
                    // khong bao gio toi
                } finally {
                    second.lock().unlock();
                }
            } finally {
                if (first.lock().isHeldByCurrentThread()) {
                    first.lock().unlock();
                }
            }
        };
    }

    /**
     * CACH DUNG 1 - LOCK ORDERING: luon lock theo mot thu tu toan cuc co dinh.
     *
     * <p>Vi moi thread deu lock theo order tang dan, khong the co circular wait. Day la cach sua
     * duoc dung nhieu nhat trong he thong that vi no khong ton chi phi runtime.
     *
     * <p>Trong thuc te, "order" thuong la mot thuoc tinh on dinh: primary key, UUID, hoac
     * {@code System.identityHashCode()} (can xu ly truong hop trung).
     */
    public static Runnable lockOrdered(Resource a, Resource b, Runnable criticalSection) {
        return () -> {
            Resource firstToLock = a.order() <= b.order() ? a : b;
            Resource secondToLock = a.order() <= b.order() ? b : a;

            firstToLock.lock().lock();
            try {
                secondToLock.lock().lock();
                try {
                    criticalSection.run();
                } finally {
                    secondToLock.lock().unlock();
                }
            } finally {
                firstToLock.lock().unlock();
            }
        };
    }

    /**
     * CACH DUNG 2 - tryLock co timeout: pha dieu kien "no preemption".
     *
     * <p>Uu diem: khong can biet thu tu toan cuc (huu ich khi lock den tu nhieu module khac nhau).
     * Nhuoc diem: co the that bai va phai retry -> caller phai xu ly duoc viec "khong lam duoc",
     * va neu retry ngay lap tuc thi de sinh livelock. Phai co backoff + jitter.
     *
     * @return true neu lay duoc ca hai lock va da chay criticalSection
     */
    public static boolean tryBothOrGiveUp(
            Resource a, Resource b, java.time.Duration timeout, Runnable criticalSection)
            throws InterruptedException {
        long timeoutMillis = timeout.toMillis();
        if (!a.lock().tryLock(timeoutMillis, TimeUnit.MILLISECONDS)) {
            return false;
        }
        try {
            if (!b.lock().tryLock(timeoutMillis, TimeUnit.MILLISECONDS)) {
                return false; // NHA lock a o finally -> khong hold-and-wait
            }
            try {
                criticalSection.run();
                return true;
            } finally {
                b.lock().unlock();
            }
        } finally {
            a.lock().unlock();
        }
    }
}
