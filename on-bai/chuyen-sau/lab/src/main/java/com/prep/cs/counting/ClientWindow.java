package com.prep.cs.counting;

/**
 * Khu trung theo client, cung y tuong voi idempotent producer cua Kafka (producerId, epoch, sequence) va cua so
 * chong phat lai cua IPsec: moi client chi can nho seq lon nhat da thay + 64 bit danh dau 64 seq ngay duoi no.
 * State ty le voi SO CLIENT dang hoat dong, khong ty le voi so request trong cua so thoi gian.
 *
 *   accept() tra ve:
 *     NEW        lan dau thay (epoch, seq) nay -> dem
 *     DUPLICATE  da thay roi (client retry, hoac chinh he thong phat lai) -> bo
 *     STALE      epoch cu hon epoch dang giu (tien trinh client cu, "xac song") -> bo
 *     TOO_OLD    seq tut sau qua 64 so: khong con biet da thay hay chua -> bo va dem rieng (phai bang 0)
 */
public final class ClientWindow {

    public enum Verdict { NEW, DUPLICATE, STALE, TOO_OLD }

    public static final int WIDTH = 64;

    private int epoch;
    private long highest;     // seq lon nhat da thay trong epoch
    private long seen;        // bit i = 1: da thay seq (highest - i)
    long lastSeenMillis;      // de don client im lang qua lau

    public ClientWindow(int epoch, long seq, long nowMillis) {
        this.epoch = epoch;
        this.highest = seq;
        this.seen = 1L;
        this.lastSeenMillis = nowMillis;
    }

    public Verdict accept(int e, long seq, long nowMillis) {
        lastSeenMillis = Math.max(lastSeenMillis, nowMillis);
        if (e < epoch) return Verdict.STALE;
        if (e > epoch) {                          // client khoi dong lai: bat dau cua so moi
            epoch = e;
            highest = seq;
            seen = 1L;
            return Verdict.NEW;
        }
        if (seq > highest) {
            long shift = seq - highest;
            seen = shift >= WIDTH ? 1L : (seen << shift) | 1L;
            highest = seq;
            return Verdict.NEW;
        }
        long back = highest - seq;
        if (back >= WIDTH) return Verdict.TOO_OLD;
        long bit = 1L << back;
        if ((seen & bit) != 0) return Verdict.DUPLICATE;
        seen |= bit;                              // den tre nhung chua thay: van la request moi
        return Verdict.NEW;
    }
}
