package com.prep.cleancode.after;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Chu y dieu quan trong nhat ve file test nay: KHONG co Spring, KHONG co mock framework,
 * KHONG co network. Chi co fake object thuong.
 *
 * <p>Do la thu do truc tiep cua thiet ke: vi HealthAggregator nhan dependency qua constructor va
 * chi phu thuoc vao interface, test tro nen tam thuong. Ban before khong the test kieu nay - phai
 * dung reflection hoac @SpringBootTest de set 14 field @Autowired.
 *
 * <p>Khi interviewer hoi "lam sao ban biet code de test?", cau tra loi khong phai "toi viet
 * test", ma la "toi khong can framework de test" - do moi la bang chung cua thiet ke tot.
 */
class HealthAggregatorTest {

    private static final Duration TIMEOUT = Duration.ofMillis(200);

    // ---------- Fake helpers ----------

    /** Check tra ve ket qua ngay lap tuc. */
    private static HealthCheck ok(ServiceName service) {
        return fake(service, () -> CompletableFuture.completedFuture(
                Health.up(service, Duration.ofMillis(5), Map.of("version", "1.0.0"))));
    }

    /** Check bao DOWN mot cach tu te (dung hop dong). */
    private static HealthCheck reportsDown(ServiceName service, String reason) {
        return fake(service, () -> CompletableFuture.completedFuture(
                Health.down(service, reason, Duration.ofMillis(5))));
    }

    /** Check ma future bi failed - loi dung cach. */
    private static HealthCheck futureFails(ServiceName service, RuntimeException ex) {
        return fake(service, () -> CompletableFuture.failedFuture(ex));
    }

    /** Check VI PHAM hop dong: nem truc tiep thay vi tra future failed. */
    private static HealthCheck throwsDirectly(ServiceName service, RuntimeException ex) {
        return fake(service, () -> {
            throw ex;
        });
    }

    /** Check VI PHAM hop dong: tra null. */
    private static HealthCheck returnsNullFuture(ServiceName service) {
        return fake(service, () -> null);
    }

    /** Check cham hon timeout. */
    private static HealthCheck slow(ServiceName service, Duration delay) {
        return fake(service, () -> CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Health.up(service, delay, Map.of());
        }));
    }

    private static HealthCheck fake(ServiceName service, java.util.function.Supplier<CompletableFuture<Health>> body) {
        return new HealthCheck() {
            @Override
            public ServiceName service() {
                return service;
            }

            @Override
            public CompletableFuture<Health> check() {
                return body.get();
            }
        };
    }

    private static HealthAggregator aggregator(HealthCheck... checks) {
        return new HealthAggregator(new HealthCheckRegistry(List.of(checks)), TIMEOUT);
    }

    // ---------- Registry: fail fast ----------

    @Nested
    @DisplayName("Registry - fail fast luc khoi tao, khong doi den runtime")
    class RegistryContract {

        @Test
        @DisplayName("hai bean cung nhan mot service -> KHONG khoi dong duoc, message chi ro ca 2 class")
        void duplicateServiceIsRejectedAtStartup() {
            var ex = assertThrows(
                    IllegalStateException.class,
                    () -> new HealthCheckRegistry(
                            List.of(ok(ServiceName.AUTHENTICATION), ok(ServiceName.AUTHENTICATION))));
            assertTrue(ex.getMessage().contains("AUTHENTICATION"), ex.getMessage());
            assertTrue(ex.getMessage().contains("claim service"), ex.getMessage());
        }

        @Test
        @DisplayName("registry rong la loi cau hinh -> chan ngay")
        void emptyRegistryIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> new HealthCheckRegistry(List.of()));
        }

        @Test
        @DisplayName("service chua co check -> Optional.empty, KHONG nem UnsupportedOperationException")
        void unknownServiceReturnsEmptyInsteadOfThrowing() {
            var registry = new HealthCheckRegistry(List.of(ok(ServiceName.AUTHENTICATION)));

            // Ban before: vao nhanh `default:` va nem -> health check tu bien thanh su co.
            assertTrue(registry.find(ServiceName.REPORTING).isEmpty());
            assertTrue(registry.find(ServiceName.AUTHENTICATION).isPresent());
            assertEquals(1, registry.registeredServices().size());
        }

        @Test
        @DisplayName("them service moi KHONG can sua registry hay aggregator (Open/Closed)")
        void addingServiceRequiresNoChangeToExistingClasses() {
            // Chi them 1 phan tu vao list. Khong co switch nao phai sua, khong co field nao phai them.
            var report = aggregator(
                            ok(ServiceName.AUTHENTICATION),
                            ok(ServiceName.INCIDENT_HUB),
                            ok(ServiceName.REPORTING))
                    .check();
            assertEquals(3, report.services().size());
            assertEquals(HealthStatus.UP, report.overall());
        }
    }

    // ---------- Aggregator: fault isolation ----------

    @Nested
    @DisplayName("Aggregator - mot service loi khong duoc lam vo ca bao cao")
    class FaultIsolation {

        @Test
        @DisplayName("tat ca UP -> overall UP")
        void allUp() {
            var report = aggregator(ok(ServiceName.AUTHENTICATION), ok(ServiceName.INCIDENT_HUB)).check();
            assertEquals(HealthStatus.UP, report.overall());
            assertTrue(report.unhealthy().isEmpty());
        }

        @Test
        @DisplayName("mot DOWN -> overall DOWN, cac service con lai VAN duoc bao cao day du")
        void oneDownDoesNotHideTheOthers() {
            var report = aggregator(
                            ok(ServiceName.AUTHENTICATION),
                            reportsDown(ServiceName.INCIDENT_HUB, "db unreachable"),
                            ok(ServiceName.REPORTING))
                    .check();

            assertEquals(HealthStatus.DOWN, report.overall());
            assertEquals(3, report.services().size(), "khong duoc mat service nao khoi bao cao");
            assertEquals(List.of(ServiceName.INCIDENT_HUB), report.unhealthy());
            assertTrue(report.services().get(ServiceName.AUTHENTICATION).isUp());
            assertEquals("db unreachable", report.services().get(ServiceName.INCIDENT_HUB).reason());
        }

        @Test
        @DisplayName("future failed -> DOWN co reason mang TEN LOAI exception, khong phai null")
        void failedFutureBecomesDownWithUsefulReason() {
            var report = aggregator(
                            ok(ServiceName.AUTHENTICATION),
                            futureFails(ServiceName.INCIDENT_HUB, new IllegalStateException("pool exhausted")))
                    .check();

            var incident = report.services().get(ServiceName.INCIDENT_HUB);
            assertEquals(HealthStatus.DOWN, incident.status());
            assertTrue(incident.reason().contains("IllegalStateException"), incident.reason());
            assertTrue(incident.reason().contains("pool exhausted"), incident.reason());
        }

        @Test
        @DisplayName("exception co message null -> reason van co ten class, khong ra chu 'null'")
        void nullMessageStillProducesReadableReason() {
            var report = aggregator(futureFails(ServiceName.CORE, new NullPointerException())).check();
            assertEquals("NullPointerException", report.services().get(ServiceName.CORE).reason());
        }

        @Test
        @DisplayName("implementation VI PHAM hop dong (nem truc tiep) van bi chiu duoc")
        void misbehavingCheckThatThrowsIsContained() {
            var report = aggregator(
                            ok(ServiceName.AUTHENTICATION),
                            throwsDirectly(ServiceName.BATCH, new RuntimeException("bad wiring")))
                    .check();

            assertEquals(2, report.services().size());
            assertEquals(HealthStatus.DOWN, report.services().get(ServiceName.BATCH).status());
            assertTrue(report.services().get(ServiceName.AUTHENTICATION).isUp());
        }

        @Test
        @DisplayName("implementation tra null future -> DOWN, KHONG NPE")
        void nullFutureIsContained() {
            var report = aggregator(ok(ServiceName.AUTHENTICATION), returnsNullFuture(ServiceName.ACCOUNT))
                    .check();
            var account = report.services().get(ServiceName.ACCOUNT);
            assertEquals(HealthStatus.DOWN, account.status());
            assertTrue(account.reason().contains("null future"), account.reason());
        }
    }

    // ---------- Timeout ----------

    @Nested
    @DisplayName("Timeout - loi ma ban before khong the chiu duoc")
    class TimeoutBehaviour {

        @Test
        @Timeout(5) // neu code treo vo han, test nay fail thay vi build treo mai mai
        @DisplayName("check cham hon timeout -> UNKNOWN (khong phai DOWN), va KHONG treo")
        void slowCheckTimesOutAsUnknown() {
            var report = aggregator(
                            ok(ServiceName.AUTHENTICATION),
                            slow(ServiceName.CORE, Duration.ofSeconds(30)))
                    .check();

            var core = report.services().get(ServiceName.CORE);
            // UNKNOWN chu khong phai DOWN: timeout khong chung minh service da chet.
            assertEquals(HealthStatus.UNKNOWN, core.status());
            assertTrue(core.reason().contains("timed out"), core.reason());

            // overall: UNKNOWN xau hon UP nhung chua den DOWN.
            assertEquals(HealthStatus.UNKNOWN, report.overall());
        }

        @Test
        @DisplayName("UNKNOWN + DOWN -> overall DOWN (xau nhat thang)")
        void statusRollupPrefersWorst() {
            var report = aggregator(
                            ok(ServiceName.AUTHENTICATION),
                            slow(ServiceName.CORE, Duration.ofSeconds(30)),
                            reportsDown(ServiceName.BATCH, "boom"))
                    .check();
            assertEquals(HealthStatus.DOWN, report.overall());
            assertEquals(List.of(ServiceName.BATCH, ServiceName.CORE), report.unhealthy());
        }

        @Test
        @Timeout(5)
        @DisplayName("cac check chay SONG SONG, khong tuan tu")
        void checksRunConcurrently() {
            // 3 check moi cai ngu 150ms. Neu tuan tu -> ~450ms > timeout 200ms -> se co cai timeout.
            // Neu song song -> ~150ms < 200ms -> tat ca UP. Assert nay chinh la bang chung song song.
            var report = aggregator(
                            slow(ServiceName.AUTHENTICATION, Duration.ofMillis(150)),
                            slow(ServiceName.INCIDENT_HUB, Duration.ofMillis(150)),
                            slow(ServiceName.CORE, Duration.ofMillis(150)))
                    .check();

            assertEquals(HealthStatus.UP, report.overall(),
                    "neu fail -> cac check dang chay tuan tu, xem lai vong lap khoi tao future");
        }

        @Test
        @DisplayName("check() duoc goi dung MOT lan cho moi service (ban before join roi get -> 2 lan)")
        void eachCheckIsInvokedExactlyOnce() {
            var calls = new AtomicInteger();
            HealthCheck counting = fake(ServiceName.CORE, () -> {
                calls.incrementAndGet();
                return CompletableFuture.completedFuture(
                        Health.up(ServiceName.CORE, Duration.ofMillis(1), Map.of()));
            });

            aggregator(counting).check();
            assertEquals(1, calls.get());
        }
    }

    // ---------- Immutability / value semantics ----------

    @Nested
    @DisplayName("Immutability - caller khong the pha trang thai ben trong")
    class Immutability {

        @Test
        @DisplayName("Report.services() khong sua duoc")
        void reportIsUnmodifiable() {
            var report = aggregator(ok(ServiceName.AUTHENTICATION)).check();
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> report.services().put(ServiceName.CORE, null));
        }

        @Test
        @DisplayName("Health.details() la copy - sua map goc khong anh huong")
        void healthDetailsAreDefensivelyCopied() {
            var mutable = new java.util.HashMap<String, String>();
            mutable.put("version", "1.0.0");
            var health = Health.up(ServiceName.CORE, Duration.ofMillis(1), mutable);

            mutable.put("version", "TAMPERED");

            assertEquals("1.0.0", health.details().get("version"));
        }

        @Test
        @DisplayName("reason null duoc chuan hoa thanh chuoi rong - phia doc khong phai null-check")
        void reasonIsNeverNull() {
            var health = new Health(ServiceName.CORE, HealthStatus.UP, null, Duration.ZERO, null);
            assertEquals("", health.reason());
            assertEquals(Map.of(), health.details());
        }

        @Test
        @DisplayName("worseOf co tinh giao hoan - UP/UNKNOWN/DOWN xep dung do nghiem trong")
        void statusOrderingIsCommutative() {
            assertEquals(HealthStatus.DOWN, HealthStatus.UP.worseOf(HealthStatus.DOWN));
            assertEquals(HealthStatus.DOWN, HealthStatus.DOWN.worseOf(HealthStatus.UP));
            assertEquals(HealthStatus.UNKNOWN, HealthStatus.UP.worseOf(HealthStatus.UNKNOWN));
            assertEquals(HealthStatus.DOWN, HealthStatus.UNKNOWN.worseOf(HealthStatus.DOWN));
            assertSame(HealthStatus.UP, HealthStatus.UP.worseOf(HealthStatus.UP));
        }
    }

    // ---------- Constructor validation ----------

    @Test
    @DisplayName("timeout <= 0 la loi cau hinh -> chan tai constructor, khong doi den luc chay")
    void invalidTimeoutIsRejected() {
        var registry = new HealthCheckRegistry(List.of(ok(ServiceName.AUTHENTICATION)));
        assertThrows(IllegalArgumentException.class, () -> new HealthAggregator(registry, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> new HealthAggregator(registry, Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> new HealthAggregator(registry, null));
        assertThrows(IllegalArgumentException.class, () -> new HealthAggregator(null, TIMEOUT));
    }

    @Test
    @DisplayName("Health.up thi isUp() true, cac trang thai khac thi false")
    void isUpReflectsStatus() {
        assertTrue(Health.up(ServiceName.CORE, Duration.ZERO, Map.of()).isUp());
        assertFalse(Health.down(ServiceName.CORE, "x", Duration.ZERO).isUp());
        assertFalse(Health.unknown(ServiceName.CORE, "x", Duration.ZERO).isUp());
    }
}
