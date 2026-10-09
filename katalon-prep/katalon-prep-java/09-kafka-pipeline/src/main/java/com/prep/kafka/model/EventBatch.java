package com.prep.kafka.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Mot request gom lo ~20 items gui ve API:
 * - batchId: UUID sinh boi client de dung lam idempotency key
 * - timestamp: thoi diem phat sinh tren client
 * - tenantId: dinh danh tenant (vd "tenant_a")
 * - items: toi da 20 cap key-value
 */
public record EventBatch(
        String batchId,
        Instant timestamp,
        String tenantId,
        List<EventItem> items
) implements Serializable {
    public EventBatch {
        if (items == null) items = List.of();
    }
}
