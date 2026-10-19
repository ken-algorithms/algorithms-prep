package com.prep.onbai.ratelimit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Ba bien the cua so: fixed window (re, co lo hong o bien), sliding log (dung, ton bo nho), sliding counter (xap xi re). */
public final class WindowCounters {

    private WindowCounters() {}

    /** Dem trong cua so co dinh [k*W, (k+1)*W). Lo hong: limit cuoi cua so nay + limit dau cua so sau = 2x trong 1 ms. */
    public static final class FixedWindow {
        private record Window(long index, int count) {}

        private final int limit;
        private final long windowMillis;
        private final LongSupplier clockMillis;
        private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

        public FixedWindow(int limit, long windowMillis, LongSupplier clockMillis) {
            this.limit = limit;
            this.windowMillis = windowMillis;
            this.clockMillis = clockMillis;
        }

        public boolean tryAcquire(String key) {
            boolean[] ok = new boolean[1];
            windows.compute(key, (k, w) -> {
                long idx = clockMillis.getAsLong() / windowMillis;
                int count = (w == null || w.index() != idx) ? 0 : w.count();
                ok[0] = count < limit;
                return new Window(idx, ok[0] ? count + 1 : count);
            });
            return ok[0];
        }
    }

    /** Luu tung moc thoi gian: chinh xac tuyet doi, bo nho O(limit) moi key. */
    public static final class SlidingLog {
        private final int limit;
        private final long windowMillis;
        private final LongSupplier clockMillis;
        private final ConcurrentHashMap<String, Deque<Long>> logs = new ConcurrentHashMap<>();

        public SlidingLog(int limit, long windowMillis, LongSupplier clockMillis) {
            this.limit = limit;
            this.windowMillis = windowMillis;
            this.clockMillis = clockMillis;
        }

        public boolean tryAcquire(String key) {
            boolean[] ok = new boolean[1];
            logs.compute(key, (k, log) -> {
                Deque<Long> d = log == null ? new ArrayDeque<>() : log;
                long now = clockMillis.getAsLong();
                while (!d.isEmpty() && d.peekFirst() <= now - windowMillis) {
                    d.pollFirst();
                }
                if (d.size() < limit) {
                    d.addLast(now);
                    ok[0] = true;
                }
                return d;
            });
            return ok[0];
        }
    }

    /** Cua so truot XAP XI: count(hien tai) + count(truoc) x phan cua so truoc con nam trong cua so truot. 2 so nguyen moi key. */
    public static final class SlidingCounter {
        private record State(long index, int current, int previous) {}

        private final int limit;
        private final long windowMillis;
        private final LongSupplier clockMillis;
        private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

        public SlidingCounter(int limit, long windowMillis, LongSupplier clockMillis) {
            this.limit = limit;
            this.windowMillis = windowMillis;
            this.clockMillis = clockMillis;
        }

        public boolean tryAcquire(String key) {
            boolean[] ok = new boolean[1];
            states.compute(key, (k, s) -> {
                long now = clockMillis.getAsLong();
                long idx = now / windowMillis;
                State st = s == null ? new State(idx, 0, 0)
                        : s.index() == idx ? s
                        : s.index() == idx - 1 ? new State(idx, 0, s.current())
                        : new State(idx, 0, 0);
                double elapsed = (double) (now - idx * windowMillis) / windowMillis;   // 0..1 trong cua so hien tai
                double estimate = st.previous() * (1 - elapsed) + st.current();
                ok[0] = estimate < limit;
                return ok[0] ? new State(idx, st.current() + 1, st.previous()) : st;
            });
            return ok[0];
        }
    }
}
