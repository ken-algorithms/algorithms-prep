package com.prep.concurrency;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Bo dem ket qua test - vi du toi gian nhung dung ban chat cua race condition.
 *
 * <p>Boi canh Katalon: nhieu worker chay test song song va cung cong don so pass/fail vao mot bo
 * dem. Neu bo dem khong thread-safe, bao cao cuoi ngay se THIEU so mot cach ngau nhien - loai bug
 * kho chiu nhat vi khong reproduce duoc va khong ai biet con so dung la bao nhieu.
 */
public final class ResultCounter {

    private ResultCounter() {}

    /**
     * KHONG thread-safe. {@code value = v + 1} la ba lenh: doc, cong, ghi (read-modify-write).
     * Hai thread doc cung mot gia tri cu -> mot lan cong bi mat.
     *
     * <p>{@code Thread.yield()} o giua chi de LAM LO ra bug mot cach on dinh trong test. Bo yield
     * ra thi bug van con, chi kho gap hon - va do chinh la ly do no song sot len production.
     */
    public static final class Unsafe {
        private int value;

        public void increment() {
            int v = value;
            Thread.yield();
            value = v + 1;
        }

        public int value() {
            return value;
        }
    }

    /**
     * Thread-safe bang synchronized. Dung, nhung moi thread phai xep hang -> contention cao khi
     * nhieu thread.
     *
     * <p>Chu y: KHONG dung `synchronized` trong code se chay tren virtual thread o Java 21,
     * vi no PIN carrier thread (xem VirtualThreads.java). Dung ReentrantLock thay the.
     */
    public static final class Synchronised {
        private int value;

        public synchronized void increment() {
            value++;
        }

        public synchronized int value() {
            return value;
        }
    }

    /**
     * Thread-safe bang CAS (compare-and-swap), khong lock. Tot cho contention thap-trung binh.
     */
    public static final class Atomic {
        private final AtomicLong value = new AtomicLong();

        public void increment() {
            value.incrementAndGet();
        }

        public long value() {
            return value.get();
        }
    }

    /**
     * LongAdder - lua chon DUNG khi rat nhieu thread cung GHI va it doc.
     *
     * <p>Vi sao nhanh hon AtomicLong: no chia gia tri ra nhieu cell rieng cho tung thread (striping)
     * nen cac thread khong tranh nhau cung mot dong cache (false sharing). Chi khi goi sum() moi
     * cong lai. Doi lai: sum() khong phai snapshot nguyen tu, va ton bo nho hon.
     *
     * <p>Day chinh xac la trade-off ma interviewer muon nghe khi hoi "AtomicLong hay LongAdder?".
     */
    public static final class Adder {
        private final LongAdder value = new LongAdder();

        public void increment() {
            value.increment();
        }

        public long value() {
            return value.sum();
        }
    }
}
