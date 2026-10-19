package com.prep.onbai.shortener;

import java.util.function.LongSupplier;

/**
 * Snowflake 64 bit: 1 bit dau = 0 | 41 bit mili giay tu EPOCH (~69 nam) | 10 bit worker (1.024) | 12 bit seq (4.096/ms).
 * Khong goi mang, khong khoa DB: moi pod tu cap ~4 trieu id/giay, id tang theo thoi gian (index B-tree than thien).
 * Dieu kien SONG CON: worker id duy nhat giua cac pod dang chay, va dong ho khong lui xa.
 */
public final class Snowflake {

    public static final long EPOCH = 1_735_689_600_000L; // 2025-01-01T00:00:00Z
    static final int WORKER_BITS = 10, SEQ_BITS = 12;
    static final long MAX_WORKER = (1L << WORKER_BITS) - 1;
    static final long SEQ_MASK = (1L << SEQ_BITS) - 1;
    static final long MAX_BACKWARD_MS = 5;

    private final long worker;
    private final LongSupplier clockMillis;
    private long lastMs = -1;
    private long seq;

    public Snowflake(long worker, LongSupplier clockMillis) {
        if (worker < 0 || worker > MAX_WORKER) {
            throw new IllegalArgumentException("worker phai trong [0, " + MAX_WORKER + "]");
        }
        this.worker = worker;
        this.clockMillis = clockMillis;
    }

    public synchronized long next() {
        long now = clockMillis.getAsLong();
        if (now < lastMs) {
            long behind = lastMs - now;
            if (behind > MAX_BACKWARD_MS) {
                throw new IllegalStateException("dong ho lui " + behind + " ms: tu choi cap id thay vi cap trung");
            }
            now = waitUntil(lastMs); // NTP chinh lui vai ms: cho dong ho bat kip
        }
        if (now == lastMs) {
            seq = (seq + 1) & SEQ_MASK;
            if (seq == 0) {
                now = waitUntil(lastMs + 1); // het 4.096 id trong ms nay
            }
        } else {
            seq = 0;
        }
        lastMs = now;
        return ((now - EPOCH) << (WORKER_BITS + SEQ_BITS)) | (worker << SEQ_BITS) | seq;
    }

    private long waitUntil(long targetMs) {
        long now;
        while ((now = clockMillis.getAsLong()) < targetMs) {
            Thread.onSpinWait();
        }
        return now;
    }

    public static long timestampOf(long id) {
        return (id >>> (WORKER_BITS + SEQ_BITS)) + EPOCH;
    }

    public static long workerOf(long id) {
        return (id >>> SEQ_BITS) & MAX_WORKER;
    }

    public static long sequenceOf(long id) {
        return id & SEQ_MASK;
    }
}
