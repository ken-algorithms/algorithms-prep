package com.prep.onbai.idempotency;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bang idempotency_record la mot may trang thai:
 * <pre>
 *   (khong co) --claim--> IN_PROGRESS(owner, lease) --complete(owner)--> COMPLETED(response)
 *                              |  lease het han: request sau TIEP QUAN (doi owner)
 *                              '--release(owner)--> (xoa) khi CHAC CHAN chua co side effect
 * </pre>
 * Moi so sanh thoi gian dung dong ho CUA DATABASE (clock_timestamp()), khong dung dong ho tung pod.
 */
@Component
public class IdempotencyStore {

    public sealed interface Claim permits Acquired, Replay, InProgress, Mismatch {}

    /** Ta la chu, owner la fencing token: chi chu hien tai moi complete duoc. */
    public record Acquired(UUID owner, boolean takeover) implements Claim {}

    public record Replay(int code, String body) implements Claim {}

    public record InProgress(long retryAfterMs) implements Claim {}

    public record Mismatch() implements Claim {}

    private final JdbcTemplate jdbc;

    public IdempotencyStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Chay NGOAI transaction nghiep vu (autocommit): IN_PROGRESS phai hien ra cho request khac ngay lap tuc. */
    public Claim claim(String scope, String key, String requestHash, Duration lease) {
        UUID me = UUID.randomUUID();
        String leaseSql = lease.toMillis() + " milliseconds";
        int inserted = jdbc.update("""
                INSERT INTO idempotency_record (scope, key, request_hash, status, owner_token, locked_until)
                VALUES (?, ?, ?, 'IN_PROGRESS', ?, clock_timestamp() + ?::interval)
                ON CONFLICT (scope, key) DO NOTHING""", scope, key, requestHash, me, leaseSql);
        if (inserted == 1) {
            return new Acquired(me, false);
        }
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT request_hash, status, response_code, response_body,
                       greatest(0, extract(epoch FROM locked_until - clock_timestamp()) * 1000)::bigint AS remaining_ms
                  FROM idempotency_record WHERE scope = ? AND key = ?""", scope, key);
        if (!requestHash.equals(row.get("request_hash"))) {
            return new Mismatch();
        }
        if ("COMPLETED".equals(row.get("status"))) {
            return new Replay((Integer) row.get("response_code"), (String) row.get("response_body"));
        }
        long remaining = ((Number) row.get("remaining_ms")).longValue();
        if (remaining > 0) {
            return new InProgress(remaining);
        }
        // Lease het han: chu cu co le da chet giua chung. Tiep quan co dieu kien -> hai nguoi cung thay thi chi mot thang.
        int took = jdbc.update("""
                UPDATE idempotency_record SET owner_token = ?, locked_until = clock_timestamp() + ?::interval
                 WHERE scope = ? AND key = ? AND status = 'IN_PROGRESS' AND locked_until <= clock_timestamp()""",
                me, leaseSql, scope, key);
        return took == 1 ? new Acquired(me, true) : new InProgress(lease.toMillis());
    }

    /** Goi TRONG transaction nghiep vu: ket qua va side effect DB cung commit hoac cung rollback. */
    public boolean complete(String scope, String key, UUID owner, int code, String body) {
        return jdbc.update("""
                UPDATE idempotency_record SET status = 'COMPLETED', response_code = ?, response_body = ?
                 WHERE scope = ? AND key = ? AND owner_token = ? AND status = 'IN_PROGRESS'""",
                code, body, scope, key, owner) == 1;
    }

    /** Chi khi CHAC CHAN chua co side effect nao: tra key de client thu lai ngay. */
    public void release(String scope, String key, UUID owner) {
        jdbc.update("DELETE FROM idempotency_record WHERE scope = ? AND key = ? AND owner_token = ? AND status = 'IN_PROGRESS'",
                scope, key, owner);
    }
}
