package com.prep.perf.code;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * P06 - Dung chuoi log khi level dang TAT.
 *
 * <p>Production chay INFO, nhung {@code log.debug("..." + obj)} van noi chuoi va goi toString()
 * truoc khi vao ham debug - roi vut di. Dung java.util.logging cho khoi phu thuoc; voi SLF4J y het:
 * {@code log.debug("x {}", obj)} hoan toString() (tot), NHUNG {@code log.debug("x {}", risk(obj))}
 * van chay risk(obj) vi tham so duoc tinh truoc khi goi ham. Phai dung guard hoac Supplier
 * ({@code log.atDebug().addArgument(() -> risk(obj))} o SLF4J 2).
 */
public final class P06Logging {

    private P06Logging() {}

    private static final Logger LOG = Logger.getLogger("perf.p06");

    static {
        LOG.setLevel(Level.INFO);   // nhu production: FINE (= debug) dang tat
        LOG.setUseParentHandlers(false);
    }

    public record Payment(String id, String account, long amountMinor, String note) {}

    /** Mo phong mot ham "re tien" thuong gap trong log: mask PII + format. */
    static String describe(Payment p) {
        String masked = p.account().substring(0, 3) + "****" + p.account().substring(p.account().length() - 2);
        return String.format("Payment[id=%s, acct=%s, amount=%d.%02d, note=%s]",
                p.id(), masked, p.amountMinor() / 100, p.amountMinor() % 100, p.note());
    }

    /** XAU: chuoi duoc dung xong moi biet FINE dang tat. */
    public static void logBad(Payment p) {
        LOG.fine("processing " + describe(p));
    }

    /** SUA 1: guard. */
    public static void logGuarded(Payment p) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("processing " + describe(p));
        }
    }

    /** SUA 2: Supplier - chi dung chuoi khi can. */
    public static void logSupplier(Payment p) {
        LOG.fine(() -> "processing " + describe(p));
    }
}
