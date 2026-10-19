package com.prep.onbai.ledger;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/** Kieu du lieu cua so cai: lenh ghi, ket qua, loi nghiep vu. */
public final class Ledger {

    private Ledger() {}

    /** Mot ve cua but toan: cong (+) hoac tru (-) vao mot account. */
    public record Leg(long accountId, BigDecimal amount) {}

    /** Lenh ghi mot but toan. idempotencyKey la khoa TU NHIEN cua nghiep vu, khong phai UUID ngau nhien. */
    public record Request(String idempotencyKey, String kind, LocalDate effectiveDate, List<Leg> legs, Long reverses) {

        public Request {
            Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(effectiveDate, "effectiveDate");
            legs = List.copyOf(legs);
        }

        public static Request of(String key, String kind, LocalDate effectiveDate, Leg... legs) {
            return new Request(key, kind, effectiveDate, List.of(legs), null);
        }

        /** Gop theo account, bo dong net = 0, sap theo account id TANG DAN: day chinh la thu tu khoa. */
        public SortedMap<Long, BigDecimal> netByAccount() {
            TreeMap<Long, BigDecimal> net = new TreeMap<>();
            for (Leg leg : legs) {
                if (leg.amount().stripTrailingZeros().scale() > 4) {
                    throw new InvalidJournal("toi da 4 chu so thap phan: " + leg.amount());
                }
                net.merge(leg.accountId(), leg.amount(), BigDecimal::add);
            }
            net.values().removeIf(v -> v.signum() == 0);
            BigDecimal total = net.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (net.size() < 2 || total.signum() != 0) {
                throw new InvalidJournal("can >= 2 account va tong = 0, dang co " + net);
            }
            return net;
        }

        /** Dau van tay noi dung. Cung key ma khac hash = client dung lai key cho mot request KHAC. */
        public String hash() {
            StringBuilder sb = new StringBuilder(kind).append('|').append(effectiveDate).append('|').append(reverses);
            netByAccount().forEach((account, amount) -> sb.append('|').append(account).append(':').append(amount.setScale(4)));
            try {
                byte[] d = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(d);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Ket qua tren mot account. seq va balanceAfter = null voi account nong. */
    public record Applied(long accountId, Long seq, BigDecimal balanceAfter) {}

    /** replayed = true: key da duoc dung, tra lai DUNG ket qua lan dau, khong ghi them gi. */
    public record Receipt(long journalId, boolean replayed, List<Applied> applied) {

        public Applied on(long accountId) {
            return applied.stream().filter(a -> a.accountId() == accountId).findFirst().orElseThrow();
        }
    }

    /** Loi nghiep vu: transaction rollback toan bo, ke ca dong journal -> key duoc giai phong. */
    public static class Rejected extends RuntimeException {
        Rejected(String message) {
            super(message);
        }
    }

    public static final class InsufficientFunds extends Rejected {
        public InsufficientFunds(long accountId, BigDecimal amount) {
            super("account " + accountId + " khong du so du cho " + amount);
        }
    }

    public static final class UnknownAccount extends Rejected {
        public UnknownAccount(long accountId) {
            super("khong co account " + accountId);
        }
    }

    public static final class KeyReuse extends Rejected {
        public KeyReuse(String key) {
            super("key " + key + " da dung cho mot request khac noi dung");
        }
    }

    public static final class InvalidJournal extends Rejected {
        public InvalidJournal(String message) {
            super(message);
        }
    }
}
