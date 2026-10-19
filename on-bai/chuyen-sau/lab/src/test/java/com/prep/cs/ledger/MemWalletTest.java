package com.prep.cs.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.prep.cs.ledger.mem.WalletStress;
import com.prep.cs.ledger.mem.Wallets;

/** Buoc 1: ba loi kinh dien trong RAM, cung mot tai (8 luong x 200.000 lan chuyen giua 8 tai khoan). */
class MemWalletTest {

    static final int THREADS = 8, ACCOUNTS = 8, PER_THREAD = 200_000;

    @Test
    void naiveLosesOrCreatesMoney() throws Exception {
        WalletStress.Result r = WalletStress.run(new Wallets.Naive(ACCOUNTS, 1_000), THREADS, ACCOUNTS, PER_THREAD, 50);
        System.out.println("[LAB] " + r);
        assertThat(r.totalAfter()).isNotEqualTo(r.totalBefore());
    }

    @Test
    void globalLockIsCorrect() throws Exception {
        WalletStress.Result r = WalletStress.run(new Wallets.GlobalLock(ACCOUNTS, 1_000), THREADS, ACCOUNTS, PER_THREAD, 50);
        System.out.println("[LAB] " + r);
        assertThat(r.totalAfter()).isEqualTo(r.totalBefore());
        assertThat(r.minBalance()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void lockingInTransferDirectionDeadlocks() throws Exception {
        WalletStress.Result r = WalletStress.run(new Wallets.PerAccountLock(ACCOUNTS, 1_000), THREADS, ACCOUNTS, PER_THREAD, 50);
        System.out.println("[LAB] " + r);
        assertThat(r.deadlocked()).isTrue();
    }

    @Test
    void lockingInIdOrderNeverDeadlocks() throws Exception {
        WalletStress.Result r = WalletStress.run(new Wallets.OrderedLock(ACCOUNTS, 1_000), THREADS, ACCOUNTS, PER_THREAD, 50);
        System.out.println("[LAB] " + r);
        assertThat(r.deadlocked()).isFalse();
        assertThat(r.totalAfter()).isEqualTo(r.totalBefore());
        assertThat(r.minBalance()).isGreaterThanOrEqualTo(0);
    }
}
