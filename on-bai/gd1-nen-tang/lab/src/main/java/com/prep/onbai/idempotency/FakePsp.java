package com.prep.onbai.idempotency;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cong thanh toan gia, hanh xu nhu Stripe/Adyen that: idempotent theo key ta gui sang.
 * Cho phep tiem loi de tap ba loai that bai khac nhau ve ban chat:
 * <pre>
 *   down()             ket noi bi tu choi  -> CHAC CHAN chua tru tien  -> tra key, client retry ngay
 *   timeoutAfterCharge da tru tien roi moi timeout -> KHONG BIET -> giu key, retry sau lease
 *   decline()          the bi tu choi      -> ket qua NGHIEP VU xac dinh -> luu lai, replay 402
 * </pre>
 */
public final class FakePsp {

    public record Charge(String chargeId, long amountMinor, String currency) {}

    public static final class Down extends RuntimeException {
        Down() {
            super("PSP tu choi ket noi");
        }
    }

    public static final class Timeout extends RuntimeException {
        Timeout() {
            super("PSP khong tra loi kip");
        }
    }

    public static final class Declined extends RuntimeException {
        Declined() {
            super("the bi tu choi");
        }
    }

    private final Map<String, Charge> byKey = new ConcurrentHashMap<>();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicInteger charges = new AtomicInteger();
    private final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
    private final AtomicInteger downNext = new AtomicInteger();
    private final AtomicInteger timeoutAfterChargeNext = new AtomicInteger();
    private volatile boolean decline;

    public Charge charge(String idempotencyKey, long amountMinor, String currency) {
        calls.incrementAndGet();
        Long delay = latencies.poll();
        sleep(delay == null ? 0 : delay);
        if (downNext.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            throw new Down();
        }
        if (decline) {
            throw new Declined();
        }
        Charge c = byKey.computeIfAbsent(idempotencyKey,
                k -> new Charge("ch_" + charges.incrementAndGet(), amountMinor, currency)); // tru tien THAT o day
        if (timeoutAfterChargeNext.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            throw new Timeout();
        }
        return c;
    }

    /** So lan tru tien that. Bat bien cua moi test: khong bao gio > so key khac nhau. */
    public int charges() {
        return charges.get();
    }

    public int calls() {
        return calls.get();
    }

    public void reset() {
        byKey.clear();
        calls.set(0);
        charges.set(0);
        latencies.clear();
        downNext.set(0);
        timeoutAfterChargeNext.set(0);
        decline = false;
    }

    /** Do tre cho cac lan goi ke tiep, theo thu tu. */
    public void nextLatencies(long... ms) {
        for (long m : ms) {
            latencies.add(m);
        }
    }

    public void down(int times) {
        downNext.set(times);
    }

    public void timeoutAfterCharge(int times) {
        timeoutAfterChargeNext.set(times);
    }

    public void decline(boolean on) {
        decline = on;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
