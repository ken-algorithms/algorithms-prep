package com.prep.onbai.pool;

/**
 * L = lambda x W: so viec dang dang do = toc do den x thoi gian moi viec o trong he thong.
 * Dung cho MOI tai nguyen co han: thread, connection DB, slot Kafka consumer, permit cua semaphore.
 */
public final class LittlesLaw {

    private LittlesLaw() {}

    /** Bao nhieu viec dang dong thoi: 2.000 req/s x 50 ms = 100 request dang bay. */
    public static double inFlight(double perSecond, double latencyMs) {
        return perSecond * latencyMs / 1000.0;
    }

    /** Tran thong luong cua mot pool: 10 connection, giu 55 ms moi lan -> 181 req/s, bat ke bao nhieu thread/pod. */
    public static double maxThroughput(int poolSize, double holdMs) {
        return poolSize * 1000.0 / holdMs;
    }

    /** Pool can bao nhieu cho toc do va thoi gian giu cho truoc, chua tinh du phong. */
    public static int requiredPool(double perSecond, double holdMs, double targetUtilization) {
        return (int) Math.ceil(inFlight(perSecond, holdMs) / targetUtilization);
    }

    /**
     * Pool toi thieu de KHONG tu deadlock khi moi thread can nhieu connection cung luc (REQUIRES_NEW, hai DataSource...):
     * Tn x (Cm - 1) + 1  (cong thuc tu wiki HikariCP "About Pool Sizing").
     */
    public static int deadlockFreePool(int maxThreads, int maxConnectionsPerThread) {
        return maxThreads * (maxConnectionsPerThread - 1) + 1;
    }
}
