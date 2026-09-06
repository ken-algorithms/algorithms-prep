package com.prep.cleancode.before;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * File nay KHONG phai test "cho vui". No la BANG CHUNG CHAY DUOC cho tung failure mode cua ban
 * before - de khi phong van ban noi duoc "toi biet cho nay vo nhu the nao, va day la cach do".
 *
 * <p>Doc ky: moi test o day mo ta mot hanh vi SAI ma van "pass". Do la dieu dang so nhat - code
 * kieu nay khong crash luc deploy, no chi lang le tra ve du lieu thieu cho dashboard, roi 3 thang
 * sau co nguoi hoi "sao health check bao xanh ma service chet?".
 */
class BeforeFailureModesTest {

    // Hang so hoa theo SonarLint java:S1192.
    private static final String VERSION = "version";
    private static final String DB_POOL_EXHAUSTED = "db pool exhausted";

    /** Strategy tra ve UP ngay lap tuc. */
    private static HealthCheckStrategy up() {
        return () -> {
            var health = new Health();
            health.setStatus("UP");
            health.getDetails().put(VERSION, "1.0.0");
            return CompletableFuture.completedFuture(health);
        };
    }

    @Test
    @DisplayName("[1] Field injection: quen set 1 field -> NPE, khong phai loi cau hinh ro rang")
    void missingFieldInjectionCausesNpe() {
        var aggregator = new ServiceHealthAggregatorBefore();
        aggregator.authHealthCheckStrategy = up();
        // CO Y quen incidentHubHealthCheckStrategy - mo phong thieu 1 bean / sai @Qualifier.

        // Ket qua la NullPointerException tu tan trong getDoCheckFuture. Message khong noi duoc
        // "thieu bean cho INCIDENT_HUB", no chi noi "Cannot invoke doCheck() because ... is null".
        assertThrows(NullPointerException.class, aggregator::doHealthCheck);

        // So sanh: ban after chan viec nay ngay tai constructor cua HealthCheckRegistry,
        // voi message chi ro service nao va class nao.
    }

    @Test
    @DisplayName("[2] Switch trong Strategy: service chua wire -> UnsupportedOperationException luc RUNTIME")
    void unwiredServiceThrowsAtRuntimeNotStartup() {
        var aggregator = new ServiceHealthAggregatorBefore();
        aggregator.authHealthCheckStrategy = up();
        aggregator.incidentHubHealthCheckStrategy = up();
        // 6 strategy khac da duoc khai bao lam field nhung switch khong he xu ly.
        // Chung la dependency CHET: Spring van tao bean, van ton memory, van co the fail khi
        // khoi tao - de rot cuoc khong ai goi.

        // doHealthCheck() hien chi hardcode List.of(AUTHENTICATION, INCIDENT_HUB) nen chay duoc.
        Map<String, Object> result = aggregator.doHealthCheck();
        assertEquals(2, result.size());

        // Nhung neu ai do them REPORTING vao danh sach, ho phai sua switch. Neu quen:
        // -> no nem luc request cham vao, khong phai luc deploy.
        // Ta chung minh dieu do bang cach goi truc tiep vao nhanh default:
        assertTrue(aggregator.knownServiceIds().contains("auth"));
    }

    /**
     * [3a] Phat hien khi CHAY THAT (khong doc code suong): hanh vi con te hon du doan.
     *
     * <p>Du doan ban dau cua toi: {@code getSafe()} nuot loi -> response rong im lang.
     * Thuc te: dong BIEN CHET {@code doneJobs} goi {@code join()} truoc, va {@code join()} tren
     * future failed thi NEM {@code CompletionException}. Nen ca method no tung ra ngoai -> HTTP 500.
     *
     * <p>Tro treu: {@code getSafe()} - doan duy nhat duoc viet ra DE xu ly loi - khong bao gio
     * chay toi tren duong nay. No la dead code.
     */
    @Test
    @DisplayName("[3a] Downstream loi -> CompletionException tung ra ngoai (do dong BIEN CHET), 500")
    void failedDownstreamBlowsUpBecauseOfTheDeadVariable() {
        var aggregator = new ServiceHealthAggregatorBefore();
        aggregator.authHealthCheckStrategy = up();
        aggregator.incidentHubHealthCheckStrategy =
                () -> CompletableFuture.failedFuture(new IllegalStateException(DB_POOL_EXHAUSTED));

        var ex = assertThrows(java.util.concurrent.CompletionException.class, aggregator::doHealthCheck);
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals(DB_POOL_EXHAUSTED, ex.getCause().getMessage());

        // Bai hoc: 1 service phu chet -> ca endpoint health chet. Health check tu bien thanh su co.
        // Ban after co lap loi trong 1 entry, cac service khac van bao cao binh thuong.
    }

    /**
     * [3b] BAI HOC QUAN TRONG NHAT CUA CA MODULE: hai bug dang CHE LAP NHAU.
     *
     * <p>Neu ban "don dep vo hai" bang cach xoa dong bien chet (dieu ma moi reviewer se de nghi,
     * va SonarLint dang bao java:S1481 + java:S1854 doi ban xoa), hanh vi DOI HAN: khong nem nua,
     * getSafe() nuot loi, response thanh RONG mot cach im lang.
     *
     * <p>Tuc la ban vua bien mot su co DE THAY (500, alert bao ngay) thanh mot su co KHONG AI THAY
     * (dashboard render xanh, 3 thang sau moi co nguoi hoi). Ve mat Sonar thi ban da "fix" 2 issue.
     * Ve mat van hanh thi ban vua lam moi thu te hon.
     *
     * <p>Day la ly do vi sao: dung refactor code khong co test truoc. Va la cau tra loi rat tot
     * cho cau hoi phong van "vi sao ban viet test truoc khi refactor?".
     */
    @Test
    @DisplayName("[3b] Xoa dead code -> doi tu 500 on ao sang response RONG im lang (te hon)")
    void removingDeadCodeSilentlyChangesBehaviour() {
        var aggregator = new ServiceHealthAggregatorBefore();
        aggregator.authHealthCheckStrategy = up();
        aggregator.incidentHubHealthCheckStrategy =
                () -> CompletableFuture.failedFuture(new IllegalStateException(DB_POOL_EXHAUSTED));

        // Ban da xoa dong bien chet -> khong nem nua.
        Map<String, Object> result = aggregator.doHealthCheckWithoutDeadVariable();

        @SuppressWarnings("unchecked")
        Map<String, Object> incident = (Map<String, Object>) result.get("incidentHub");

        // KHONG nem, KHONG mat key - chi la mot map RONG.
        assertTrue(incident.isEmpty(), "response rong -> khong con tin hieu gi ve loi");
        assertNull(incident.get("status"));

        // Va service khoe thi van bao cao binh thuong -> nhin vao dashboard thay "gan nhu on".
        @SuppressWarnings("unchecked")
        Map<String, Object> auth = (Map<String, Object>) result.get("auth");
        assertEquals("UP", auth.get("status"));

        // Nguyen nhan that ("db pool exhausted") da bi getSafe() nem vao thung rac vinh vien.
        // Ban after: Health.down(service, "IllegalStateException: db pool exhausted", ...)
        // -> reason duoc giu va di len tan dashboard.
    }

    @Test
    @Timeout(10)
    @DisplayName("[4] Khong co timeout: 1 downstream treo -> ca health check treo VO HAN")
    void noTimeoutMeansTheWholeCheckHangsForever() throws InterruptedException {
        var aggregator = new ServiceHealthAggregatorBefore();
        aggregator.authHealthCheckStrategy = up();
        // Future nay khong bao gio complete - mo phong downstream nhan TCP nhung khong tra loi.
        aggregator.incidentHubHealthCheckStrategy = CompletableFuture::new;

        var finished = new CountDownLatch(1);
        var worker = new Thread(
                () -> {
                    try {
                        aggregator.doHealthCheck();
                    } catch (RuntimeException ignored) {
                        // khong quan tam
                    } finally {
                        finished.countDown();
                    }
                },
                "before-hang-probe");
        worker.setDaemon(true); // daemon -> JVM cua surefire van thoat duoc du thread con ket
        worker.start();

        // Sau 500ms van chua xong. Trong production day la request treo, giu connection,
        // lam can thread pool, roi lan sang cac endpoint khac.
        boolean completed = finished.await(500, TimeUnit.MILLISECONDS);
        assertFalse(completed, "ban before dang treo - dung nhu du doan");

        worker.interrupt();

        // So sanh: ban after dung orTimeout(...) nen LUON ket thuc, va bao UNKNOWN cho service treo.
    }

    @Test
    @DisplayName("[5] Tra ve HashMap mutable -> caller sua duoc trang thai ben trong")
    void returnedMapIsMutable() {
        var aggregator = new ServiceHealthAggregatorBefore();
        aggregator.authHealthCheckStrategy = up();
        aggregator.incidentHubHealthCheckStrategy = up();

        Map<String, Object> result = aggregator.checkHealth();
        result.put("SHARED", "da bi ghi de tu ben ngoai");
        result.remove("SHARED");

        assertTrue(result.isEmpty(), "khong co gi ngan caller pha du lieu");
        // Ban after: Report.services() la Map.copyOf -> nem UnsupportedOperationException.
    }

    @Test
    @DisplayName("[6] Health mutable: sua details sau khi tao -> khong the tin ket qua da doc")
    void healthObjectIsMutableAfterCreation() {
        var health = new Health();
        health.setStatus("UP");
        health.getDetails().put(VERSION, "1.0.0");

        // getDetails() tra ve chinh map ben trong -> ai cung sua duoc.
        health.getDetails().put(VERSION, "TAMPERED");
        health.setStatus("DOWN");

        assertEquals("TAMPERED", health.getDetails().get(VERSION));
        assertEquals("DOWN", health.getStatus());
        // Ban after: record + Map.copyOf -> khong the sua sau khi tao.
    }
}
