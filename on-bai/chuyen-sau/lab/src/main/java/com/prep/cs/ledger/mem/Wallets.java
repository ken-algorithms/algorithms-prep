package com.prep.cs.ledger.mem;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Buoc 1 cua bai so du: bon cach giu so du TRONG RAM khi nhieu luong cung chuyen tien. Chua co database, de thay
 * ba loi kinh dien bang chinh Java: mat cap nhat, rut qua so du, deadlock — truoc khi gap lai chung o Postgres.
 */
public final class Wallets {

    private Wallets() {}

    public interface Wallet {
        /** Chuyen amount tu from sang to. false = khong du tien. */
        boolean transfer(int from, int to, long amount);

        long balance(int account);

        /** Tong moi tai khoan. Bat bien: khong doi sau bat ky so lan chuyen nao. */
        long total();
    }

    /** V1: doc - kiem - ghi tren long[] khong dong bo. Hai luong cung doc mot so du roi cung ghi: mat mot lan. */
    public static final class Naive implements Wallet {
        final long[] b;

        public Naive(int accounts, long initial) {
            b = new long[accounts];
            java.util.Arrays.fill(b, initial);
        }

        @Override
        public boolean transfer(int from, int to, long amount) {
            long fromBal = b[from];                 // (1) doc
            if (fromBal < amount) return false;     // (2) kiem
            b[from] = fromBal - amount;             // (3) ghi gia tri TUYET DOI tinh tu lan doc cu
            b[to] = b[to] + amount;                 // doc-sua-ghi, cung khong nguyen tu
            return true;
        }

        @Override
        public long balance(int a) {
            return b[a];
        }

        @Override
        public long total() {
            long t = 0;
            for (long x : b) t += x;
            return t;
        }
    }

    /** V2: mot khoa cho ca vi. Dung tuyet doi, nhung moi giao dich xep mot hang: tran = 1 luong. */
    public static final class GlobalLock implements Wallet {
        final long[] b;

        public GlobalLock(int accounts, long initial) {
            b = new long[accounts];
            java.util.Arrays.fill(b, initial);
        }

        @Override
        public synchronized boolean transfer(int from, int to, long amount) {
            if (b[from] < amount) return false;
            b[from] -= amount;
            b[to] += amount;
            return true;
        }

        @Override
        public synchronized long balance(int a) {
            return b[a];
        }

        @Override
        public synchronized long total() {
            long t = 0;
            for (long x : b) t += x;
            return t;
        }
    }

    /** V3: moi tai khoan mot khoa, khoa from roi khoa to. A->B va B->A cung luc: moi ben giu mot khoa, cho khoa kia. */
    public static class PerAccountLock implements Wallet {
        final long[] b;
        final ReentrantLock[] locks;

        public PerAccountLock(int accounts, long initial) {
            b = new long[accounts];
            java.util.Arrays.fill(b, initial);
            locks = new ReentrantLock[accounts];
            for (int i = 0; i < accounts; i++) locks[i] = new ReentrantLock();
        }

        /** Thu tu khoa: V3 theo chieu chuyen tien, V4 ghi de theo id. */
        int first(int from, int to) {
            return from;
        }

        @Override
        public boolean transfer(int from, int to, long amount) {
            int a = first(from, to), c = a == from ? to : from;
            locks[a].lock();
            try {
                locks[c].lock();
                try {
                    if (b[from] < amount) return false;
                    b[from] -= amount;
                    b[to] += amount;
                    return true;
                } finally {
                    locks[c].unlock();
                }
            } finally {
                locks[a].unlock();
            }
        }

        @Override
        public long balance(int a) {
            locks[a].lock();
            try {
                return b[a];
            } finally {
                locks[a].unlock();
            }
        }

        @Override
        public long total() {          // chi goi khi khong con luong nao chuyen tien
            long t = 0;
            for (long x : b) t += x;
            return t;
        }
    }

    /** V4: van moi tai khoan mot khoa, nhung LUON khoa id nho truoc. Moi giao dich khoa cung mot thu tu: het vong cho. */
    public static final class OrderedLock extends PerAccountLock {
        public OrderedLock(int accounts, long initial) {
            super(accounts, initial);
        }

        @Override
        int first(int from, int to) {
            return Math.min(from, to);
        }
    }
}
