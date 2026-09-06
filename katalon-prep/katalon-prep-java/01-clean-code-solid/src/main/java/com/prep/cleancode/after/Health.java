package com.prep.cleancode.after;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable value object thay cho class mutable + HashMap tuy y.
 *
 * <p>Ba thay doi co y do:
 * <ul>
 *   <li>record -> immutable, co equals/hashCode/toString mien phi, an toan khi chia se giua thread.
 *   <li>details la {@code Map<String, String>} chu khong phai {@code Map<String, Object>}
 *       -> khong con cast, khong con ClassCastException an trong helper "browse".
 *   <li>co san factory {@code up/down/unknown} -> cho goi khong tu bay dat status string sai chinh ta.
 * </ul>
 *
 * <p>{@code reason} chi co nghia khi khong phai UP; voi UP thi la chuoi rong (khong dung null,
 * de phia doc khong phai null-check).
 */
public record Health(ServiceName service, HealthStatus status, String reason, Duration latency,
                     Map<String, String> details) {

    public Health {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(latency, "latency");
        reason = reason == null ? "" : reason;
        // Defensive copy: record khong tu dong immutable o noi dung Map.
        details = details == null ? Map.of() : Map.copyOf(details);
    }

    public static Health up(ServiceName service, Duration latency, Map<String, String> details) {
        return new Health(service, HealthStatus.UP, "", latency, details);
    }

    public static Health down(ServiceName service, String reason, Duration latency) {
        return new Health(service, HealthStatus.DOWN, reason, latency, Map.of());
    }

    /** Dung khi ta khong biet trang thai that (timeout, loi ha tang). */
    public static Health unknown(ServiceName service, String reason, Duration latency) {
        return new Health(service, HealthStatus.UNKNOWN, reason, latency, Map.of());
    }

    public boolean isUp() {
        return status == HealthStatus.UP;
    }
}
