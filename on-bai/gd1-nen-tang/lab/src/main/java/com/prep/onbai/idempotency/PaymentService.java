package com.prep.onbai.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prep.onbai.idempotency.IdempotencyStore.Acquired;
import com.prep.onbai.idempotency.IdempotencyStore.InProgress;
import com.prep.onbai.idempotency.IdempotencyStore.Mismatch;
import com.prep.onbai.idempotency.IdempotencyStore.Replay;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Thanh toan co side effect NGOAI database (PSP). Mot transaction khong boc duoc cuoc goi mang, nen:
 * <pre>
 *   1. claim key (autocommit)               -> IN_PROGRESS, ai den sau thay ngay
 *   2. goi PSP NGOAI transaction            -> khong giu DB connection khi cho mang; gui kem key -> PSP tu dedupe
 *   3. MOT transaction: complete(owner) + INSERT payment -> chu cu bi tiep quan thi complete tra false -> rollback
 * </pre>
 */
@Service
public class PaymentService {

    public record PaymentRequest(String customerId, long amountMinor, String currency) {

        /** Dau van tay CHUAN HOA cua noi dung (khong phai cua chuoi JSON, vi thu tu field co the khac). */
        public String hash() {
            String canonical = customerId + "|" + amountMinor + "|" + currency;
            try {
                return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                        .digest(canonical.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Ket qua HTTP: body luu nguyen van de replay giong het tung byte. */
    public record Outcome(int code, String body, boolean replayed, long retryAfterMs) {}

    static final class Fenced extends RuntimeException {
        Fenced() {
            super("key da bi request khac tiep quan");
        }
    }

    private final IdempotencyStore store;
    private final FakePsp psp;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final Duration lease;

    public PaymentService(IdempotencyStore store, FakePsp psp, JdbcTemplate jdbc, TransactionTemplate tx,
                          ObjectMapper json, @Value("${onbai.idempotency.lease-ms:30000}") long leaseMs) {
        this.store = store;
        this.psp = psp;
        this.jdbc = jdbc;
        this.tx = tx;
        this.json = json;
        this.lease = Duration.ofMillis(leaseMs);
    }

    public Outcome pay(String scope, String key, PaymentRequest req) {
        return switch (store.claim(scope, key, req.hash(), lease)) {
            case Mismatch m -> new Outcome(422, error("idempotency_key_reused", "key da dung cho mot request khac noi dung"), false, 0);
            case Replay r -> new Outcome(r.code(), r.body(), true, 0);
            case InProgress p -> new Outcome(409, error("request_in_progress", "request cung key dang chay"), false, p.retryAfterMs());
            case Acquired a -> execute(scope, key, req, a.owner());
        };
    }

    private Outcome execute(String scope, String key, PaymentRequest req, UUID owner) {
        FakePsp.Charge charge;
        try {
            charge = psp.charge(scope + ":" + key, req.amountMinor(), req.currency()); // key truyen xuong: PSP dedupe
        } catch (FakePsp.Declined e) {
            return finish(scope, key, owner, 402, error("card_declined", e.getMessage()), () -> {});
        } catch (FakePsp.Down e) {
            store.release(scope, key, owner); // chac chan chua tru tien -> tra key
            return new Outcome(503, error("psp_down", e.getMessage()), false, 0);
        } catch (FakePsp.Timeout e) {
            // KHONG biet PSP da tru tien chua -> giu IN_PROGRESS; retry sau lease se goi lai PSP CUNG key
            return new Outcome(504, error("psp_timeout", e.getMessage()), false, lease.toMillis());
        }
        UUID paymentId = UUID.randomUUID();
        String body = toJson(Map.of("paymentId", paymentId.toString(), "status", "SUCCEEDED",
                "amountMinor", req.amountMinor(), "currency", req.currency(), "chargeId", charge.chargeId()));
        return finish(scope, key, owner, 201, body, () -> jdbc.update("""
                INSERT INTO payment (id, scope, idem_key, amount_minor, currency, psp_charge_id)
                VALUES (?, ?, ?, ?, ?, ?)""", paymentId, scope, key, req.amountMinor(), req.currency(), charge.chargeId()));
    }

    /** Chot ket qua + side effect DB trong MOT transaction, co kiem tra fencing token truoc. */
    private Outcome finish(String scope, String key, UUID owner, int code, String body, Runnable sideEffect) {
        try {
            tx.executeWithoutResult(s -> {
                if (!store.complete(scope, key, owner, code, body)) {
                    throw new Fenced(); // da bi tiep quan: rollback, khong ghi gi
                }
                sideEffect.run();
            });
            return new Outcome(code, body, false, 0);
        } catch (Fenced e) {
            return new Outcome(409, error("superseded", e.getMessage()), false, 0);
        }
    }

    private String error(String code, String message) {
        return toJson(Map.of("error", code, "message", message));
    }

    private String toJson(Map<String, Object> m) {
        try {
            return json.writeValueAsString(new java.util.TreeMap<>(m)); // thu tu field co dinh
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
