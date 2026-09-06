package com.prep.cleancode.after;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Chay song song toan bo health check, ap timeout cho TUNG cai, va KHONG BAO GIO nem hay tra null.
 *
 * <p>So voi ban before:
 * <table border="1">
 *   <caption>Doi chieu</caption>
 *   <tr><th>before</th><th>after</th></tr>
 *   <tr><td>{@code future.get()} khong timeout -> treo vo han</td>
 *       <td>{@code orTimeout(...)} per-check -> luon ket thuc</td></tr>
 *   <tr><td>nuot exception, tra null</td>
 *       <td>map exception -> {@code Health.down/unknown} co reason</td></tr>
 *   <tr><td>1 downstream loi lam vo ca ham</td>
 *       <td>loi bi CO LAP trong 1 entry, cac service khac van bao cao binh thuong</td></tr>
 *   <tr><td>field injection, khong test duoc</td>
 *       <td>constructor injection, field final -> test khong can framework</td></tr>
 * </table>
 *
 * <p>Dependency Inversion: class nay chi biet {@link HealthCheck} (abstraction) va
 * {@link HealthCheckRegistry}. No khong biet service nao ton tai, khong biet goi HTTP hay gRPC.
 * Nho vay test duoc bang fake, khong can Spring, khong can network.
 */
public final class HealthAggregator {

    private final HealthCheckRegistry registry;
    private final Duration perCheckTimeout;

    /** Constructor injection + field final: object khong the ton tai o trang thai nua voi. */
    public HealthAggregator(HealthCheckRegistry registry, Duration perCheckTimeout) {
        if (registry == null) {
            throw new IllegalArgumentException("registry must not be null");
        }
        if (perCheckTimeout == null || perCheckTimeout.isZero() || perCheckTimeout.isNegative()) {
            throw new IllegalArgumentException("perCheckTimeout must be positive, got " + perCheckTimeout);
        }
        this.registry = registry;
        this.perCheckTimeout = perCheckTimeout;
    }

    /**
     * Ket qua tong hop. Khoa cua map la ServiceName (type-safe), khong phai String tuy y nhu
     * {@code Map<String, Object>} o ban before.
     */
    public record Report(HealthStatus overall, Map<ServiceName, Health> services) {
        public Report {
            services = Map.copyOf(services);
        }

        public List<ServiceName> unhealthy() {
            return services.entrySet().stream()
                    .filter(e -> !e.getValue().isUp())
                    .map(Map.Entry::getKey)
                    .sorted()
                    .toList();
        }
    }

    /**
     * Goi tat ca check SONG SONG, roi cho tat ca xong.
     *
     * <p>Y quan trong ve hieu nang: vong lap dau tien PHAI khoi tao het future truoc, roi moi
     * join. Neu viet {@code checks.stream().map(c -> guarded(c).join())} thi cac check chay TUAN
     * TU - loi rat pho bien. Tong thoi gian se la tong cac latency thay vi max.
     */
    public Report check() {
        List<HealthCheck> checks = registry.all();

        // Buoc 1: khoi tao het - tat ca bat dau chay ngay tai day.
        List<CompletableFuture<Health>> futures = new ArrayList<>(checks.size());
        for (HealthCheck check : checks) {
            futures.add(guarded(check));
        }

        // Buoc 2: cho. allOf de khong join tuan tu tung cai.
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        var byService = new EnumMap<ServiceName, Health>(ServiceName.class);
        HealthStatus overall = HealthStatus.UP;
        for (CompletableFuture<Health> future : futures) {
            Health health = future.join(); // an toan: guarded() bao dam khong bao gio failed
            byService.put(health.service(), health);
            overall = overall.worseOf(health.status());
        }
        return new Report(overall, byService);
    }

    /**
     * Boc mot HealthCheck sao cho no LUON hoan thanh binh thuong trong gioi han thoi gian.
     *
     * <p>Day la cho tap trung toan bo xu ly loi - "errors as values" thay vi exception bay ra
     * ngoai. Nho vay {@link #check()} ben tren khong can try/catch nao ca.
     */
    private CompletableFuture<Health> guarded(HealthCheck check) {
        ServiceName service = check.service();
        long startNanos = System.nanoTime();

        CompletableFuture<Health> raw;
        try {
            raw = check.check();
            if (raw == null) {
                // Implementation vi pham hop dong. Bao cao ro thay vi de NPE o cho khac.
                return CompletableFuture.completedFuture(
                        Health.down(service, "check() returned a null future", elapsed(startNanos)));
            }
        } catch (RuntimeException ex) {
            // Implementation nem truc tiep (vi pham hop dong) - van phai chiu duoc.
            return CompletableFuture.completedFuture(
                    Health.down(service, "check() threw " + describe(ex), elapsed(startNanos)));
        }

        return raw.orTimeout(perCheckTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .handle((health, throwable) -> {
                    Duration took = elapsed(startNanos);
                    if (throwable != null) {
                        return fromThrowable(service, throwable, took);
                    }
                    if (health == null) {
                        return Health.down(service, "check() completed with null", took);
                    }
                    return health;
                });
    }

    /**
     * Phan biet TIMEOUT (-> UNKNOWN) voi loi that (-> DOWN).
     *
     * <p>Day la diem ma interviewer se dao: timeout KHONG chung minh service da chet, chi chung
     * minh ta khong kip biet. Bao DOWN se gay alert gia va lam sai SLO/error budget.
     */
    private Health fromThrowable(ServiceName service, Throwable throwable, Duration took) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof TimeoutException) {
            return Health.unknown(
                    service, "timed out after " + perCheckTimeout.toMillis() + "ms", took);
        }
        return Health.down(service, describe(cause), took);
    }

    /** CompletableFuture boc loi trong CompletionException - phai mo ra moi thay nguyen nhan that. */
    private static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    /** Log/reason phai co TEN LOAI loi, khong chi message (message thuong null). */
    private static String describe(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClass().getSimpleName()
                : throwable.getClass().getSimpleName() + ": " + message;
    }

    private static Duration elapsed(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
