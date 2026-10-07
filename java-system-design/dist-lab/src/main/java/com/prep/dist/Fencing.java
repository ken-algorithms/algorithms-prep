package com.prep.dist;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Khoa phan tan KHONG du: client giu khoa bi GC pause qua TTL, client khac lay khoa, client cu tinh day
 * va ghi de. Fencing token: storage tu choi moi lenh ghi mang token cu hon token lon nhat da thay.
 */
public final class Fencing {
    /** Dich vu khoa toi gian co TTL; moi lan cap khoa tra token tang dan. */
    static final class LockService {
        private final AtomicLong token = new AtomicLong();
        private String holder; private long expiresAt;
        synchronized Long tryAcquire(String who, long now, long ttl) {
            if (holder == null || now >= expiresAt) { holder = who; expiresAt = now + ttl; return token.incrementAndGet(); }
            return null;
        }
    }

    /** Storage kiem tra token - day la noi DUY NHAT chan duoc client "song lai tu coi chet". */
    static final class Storage {
        long maxSeen = 0; String value = "init";
        final boolean checkToken;
        Storage(boolean checkToken) { this.checkToken = checkToken; }
        synchronized boolean write(String v, long tok) {
            if (checkToken && tok < maxSeen) return false;
            maxSeen = Math.max(maxSeen, tok); value = v; return true;
        }
    }

    public static void main(String[] a) {
        for (boolean check : new boolean[]{false, true}) {
            var lock = new LockService(); var st = new Storage(check);
            long t = 0;
            Long tokA = lock.tryAcquire("A", t, 10_000);          // A lay khoa, token 1
            t += 15_000;                                          // A bi GC pause 15 s > TTL 10 s
            Long tokB = lock.tryAcquire("B", t, 10_000);          // B lay khoa, token 2
            boolean bOk = st.write("B: so du = 50", tokB);
            boolean aOk = st.write("A: so du = 100 (gia tri cu)", tokA); // A tinh day, van nghi minh giu khoa
            System.out.printf("%-22s A token=%d ghi=%s | B token=%d ghi=%s | gia tri cuoi: %s%n",
                    check ? "CO fencing token" : "KHONG fencing token", tokA, aOk, tokB, bOk, st.value);
        }
    }
}
