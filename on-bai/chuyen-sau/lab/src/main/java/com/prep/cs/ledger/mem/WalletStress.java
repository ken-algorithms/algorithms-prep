package com.prep.cs.ledger.mem;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Chay T luong, moi luong N lan chuyen tien ngau nhien giua K tai khoan. Mot luong canh (watchdog) theo doi:
 * 2 giay khong co tien do thi hoi JVM co deadlock khong (ThreadMXBean) va in ai dang cho khoa cua ai.
 */
public final class WalletStress {

    public record Result(String wallet, long ms, long ok, long rejected, long totalBefore, long totalAfter,
                         long minBalance, boolean deadlocked, String deadlockInfo) {
        @Override
        public String toString() {
            if (deadlocked) return String.format("%-14s DEADLOCK sau %d giao dich%n%s", wallet, ok + rejected, deadlockInfo);
            return String.format("%-14s %5d ms, %,9d giao dich/s, tong tien truoc %,d sau %,d (%s), so du nho nhat %,d",
                    wallet, ms, (ok + rejected) * 1000 / Math.max(1, ms), totalBefore, totalAfter,
                    totalBefore == totalAfter ? "giu nguyen" : "LECH " + (totalAfter - totalBefore), minBalance);
        }
    }

    public static Result run(Wallets.Wallet w, int threads, int accounts, int perThread, long amountMax)
            throws InterruptedException {
        long before = w.total();
        AtomicLong ok = new AtomicLong(), rejected = new AtomicLong();
        Thread[] ts = new Thread[threads];
        long t0 = System.nanoTime();
        for (int t = 0; t < threads; t++) {
            int seed = t;
            ts[t] = new Thread(() -> {
                SplittableRandom r = new SplittableRandom(seed);
                for (int i = 0; i < perThread; i++) {
                    int from = r.nextInt(accounts), to = r.nextInt(accounts - 1);
                    if (to >= from) to++;                       // khong tu chuyen cho minh
                    if (w.transfer(from, to, 1 + r.nextLong(amountMax))) ok.incrementAndGet();
                    else rejected.incrementAndGet();
                }
            }, "chuyen-" + t);
            ts[t].setDaemon(true);                              // luong ket deadlock khong giu JVM lai
            ts[t].start();
        }
        long lastProgress = -1, stuckSince = System.nanoTime();
        while (true) {
            boolean alive = false;
            for (Thread th : ts) alive |= th.isAlive();
            if (!alive) break;
            long progress = ok.get() + rejected.get();
            if (progress != lastProgress) {
                lastProgress = progress;
                stuckSince = System.nanoTime();
            } else if (System.nanoTime() - stuckSince > 2_000_000_000L) {
                return new Result(w.getClass().getSimpleName(), 0, ok.get(), rejected.get(), before, -1, 0, true,
                        deadlockInfo());
            }
            Thread.sleep(20);
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        long min = Long.MAX_VALUE;
        for (int a = 0; a < accounts; a++) min = Math.min(min, w.balance(a));
        return new Result(w.getClass().getSimpleName(), ms, ok.get(), rejected.get(), before, w.total(), min, false, "");
    }

    static String deadlockInfo() {
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        long[] ids = mx.findDeadlockedThreads();
        if (ids == null) return "  (khong tim thay vong cho)";
        StringBuilder sb = new StringBuilder();
        for (ThreadInfo ti : mx.getThreadInfo(ids)) {
            sb.append(String.format("  %s cho khoa %s, khoa do dang nam trong tay %s%n", ti.getThreadName(),
                    ti.getLockName().replaceAll(".*\\$", ""), ti.getLockOwnerName()));
            if (sb.length() > 600) break;
        }
        return sb.toString().stripTrailing();
    }
}
