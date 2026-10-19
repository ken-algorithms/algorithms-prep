package com.prep.onbai.pgconcurrency.jpa;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Retry phai BOC NGOAI transaction: moi lan thu la mot transaction moi, doc lai trang thai moi.
 * Retry ben trong @Transactional la vo nghia — persistence context van giu entity cu, transaction da hong.
 */
@Service
public class RetryingWallets {

    private final WalletService wallets;

    public RetryingWallets(WalletService wallets) {
        this.wallets = wallets;
    }

    /** Tra ve so lan thu da dung. */
    public int withdraw(long id, long amount, int maxAttempts) {
        for (int attempt = 1; ; attempt++) {
            try {
                wallets.withdrawOptimistic(id, amount);
                return attempt;
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == maxAttempts) {
                    throw e;
                }
                sleepMillis(ThreadLocalRandom.current().nextLong(1, 5L << attempt)); // backoff co jitter
            }
        }
    }

    private static void sleepMillis(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
