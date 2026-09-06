package com.prep.quarkus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Integration test THAT: HTTP -> Quarkus -> Hibernate -> PostgreSQL 16.4 that -> Flyway migration.
 * Khong mock gi ca, va khong can Docker.
 *
 * <p>Day chinh la thu dang thieu nhat o project RCI cua ban (4 file test / 470 file Java).
 */
@QuarkusTest
@QuarkusTestResource(EmbeddedPostgresResource.class)
class TestRunResourceTest {

    private static final String TENANT = "X-Tenant-Id";
    private static final String ACME = "acme";

    private long createRun(String tenant, String suite) {
        return given()
                .contentType(ContentType.JSON)
                .header(TENANT, tenant)
                .body(Map.of("suiteName", suite))
                .when()
                .post("/api/runs")
                .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("status", equalTo("RUNNING"))
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private static Map<String, Object> result(String name, String status, int durationMs, int attempts) {
        return Map.of("testName", name, "status", status, "durationMs", durationMs, "attempts", attempts);
    }

    @Test
    @DisplayName("Flyway da chay: tao run -> nop ket qua -> summary dung so lieu")
    void fullHappyPath() {
        long runId = createRun(ACME, "checkout-suite");

        List<Map<String, Object>> results = List.of(
                result("login", "PASSED", 120, 1),
                result("search", "PASSED", 300, 1),
                result("checkout", "FAILED", 900, 1),
                result("profile", "SKIPPED", 0, 1),
                // PASSED nhung attempts=3 -> service phai chuan hoa thanh FLAKY
                result("payment", "PASSED", 450, 3));

        given().contentType(ContentType.JSON)
                .header(TENANT, ACME)
                .body(Map.of("results", results))
                .when()
                .post("/api/runs/" + runId + "/results")
                .then()
                .statusCode(202)
                .body("accepted", equalTo(5));

        given().header(TENANT, ACME)
                .when()
                .get("/api/runs/" + runId + "/summary")
                .then()
                .statusCode(200)
                .body("total", equalTo(5))
                .body("byStatus.PASSED", equalTo(2))
                .body("byStatus.FAILED", equalTo(1))
                .body("byStatus.SKIPPED", equalTo(1))
                // FLAKY duoc tach rieng, KHONG gop vao PASSED
                .body("byStatus.FLAKY", equalTo(1))
                // executed = 5 - 1 skipped = 4; flaky = 1 -> 0.25
                .body("flakinessRate", is(0.25f))
                .body("p95DurationMs", notNullValue());
    }

    @Test
    @DisplayName("MULTI-TENANT: tenant khac KHONG doc duoc run cua tenant nay -> 404")
    void tenantIsolation() {
        long runId = createRun(ACME, "private-suite");

        given().header(TENANT, "other-corp")
                .when()
                .get("/api/runs/" + runId + "/summary")
                .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("title", equalTo("Not Found"));
    }

    @Test
    @DisplayName("thieu header tenant -> 400 problem+json, khong phai 500")
    void missingTenantHeader() {
        given().contentType(ContentType.JSON)
                .body(Map.of("suiteName", "x"))
                .when()
                .post("/api/runs")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"));
    }

    @Test
    @DisplayName("validation: bao CA CUM loi mot lan, khong chi loi dau tien")
    void validationReportsAllErrors() {
        long runId = createRun(ACME, "v");

        given().contentType(ContentType.JSON)
                .header(TENANT, ACME)
                .body(Map.of("results", List.of(
                        result("", "PASSED", -5, 0))))
                .when()
                .post("/api/runs/" + runId + "/results")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                // 3 vi pham cung luc: testName blank, durationMs < 0, attempts < 1
                .body("errors", hasSize(greaterThan(2)));
    }

    @Test
    @DisplayName("batch vuot 10.000 -> 400 ro rang, KHONG phai 500 hay OOM")
    void oversizedBatchIsRejectedCleanly() {
        long runId = createRun(ACME, "huge");
        var tooMany = new ArrayList<Map<String, Object>>(10_001);
        for (int i = 0; i < 10_001; i++) {
            tooMany.add(result("t" + i, "PASSED", 1, 1));
        }

        given().contentType(ContentType.JSON)
                .header(TENANT, ACME)
                .body(Map.of("results", tooMany))
                .when()
                .post("/api/runs/" + runId + "/results")
                .then()
                .statusCode(400)
                .body("title", equalTo("Bad Request"));
    }

    @Test
    @DisplayName("run da COMPLETED khong nhan them ket qua -> 409 Conflict")
    void completedRunRejectsMoreResults() {
        long runId = createRun(ACME, "closed");

        given().header(TENANT, ACME)
                .when()
                .post("/api/runs/" + runId + "/complete")
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("finishedAt", notNullValue());

        given().contentType(ContentType.JSON)
                .header(TENANT, ACME)
                .body(Map.of("results", List.of(result("late", "PASSED", 1, 1))))
                .when()
                .post("/api/runs/" + runId + "/results")
                .then()
                .statusCode(409)
                .body("title", equalTo("Conflict"));
    }

    @Test
    @DisplayName("phan trang: size bi gioi han 100, khong cho keo ca bang")
    void pageSizeIsCapped() {
        createRun(ACME, "p1");
        createRun(ACME, "p2");

        given().header(TENANT, ACME)
                .queryParam("size", 1)
                .when()
                .get("/api/runs")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));

        given().header(TENANT, ACME)
                .queryParam("size", 1_000_000)
                .when()
                .get("/api/runs")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("summary cua run rong -> flakinessRate = 0.0, KHONG phai NaN")
    void emptyRunDoesNotProduceNaN() {
        long runId = createRun(ACME, "empty");

        given().header(TENANT, ACME)
                .when()
                .get("/api/runs/" + runId + "/summary")
                .then()
                .statusCode(200)
                .body("total", equalTo(0))
                .body("flakinessRate", is(0.0f));
    }

    @Test
    @DisplayName("catch-all mapper KHONG duoc bien loi 4xx cua client thanh 500")
    void catchAllMapperMustNotSwallowWebApplicationException() {
        // Bug that da gap: ExceptionMapper<RuntimeException> bat luon ca WebApplicationException
        // (NotSupportedException = 415, NotAllowedException = 405...) va tra 500 cho tat ca.
        // Ket qua: loi CUA CLIENT bi bao thanh loi CUA SERVER -> alert 5xx gia.

        // Content-Type sai -> phai la 415 Unsupported Media Type, KHONG phai 500.
        given().contentType(ContentType.TEXT)
                .header(TENANT, ACME)
                .body("not json")
                .when()
                .post("/api/runs")
                .then()
                .statusCode(415);

        // HTTP method khong ton tai tren path -> 405, KHONG phai 500.
        given().header(TENANT, ACME)
                .when()
                .delete("/api/runs")
                .then()
                .statusCode(405);
    }

    @Test
    @DisplayName("endpoint KHONG co body thi khong doi Content-Type (@Consumes WILDCARD)")
    void bodylessEndpointDoesNotRequireContentType() {
        long runId = createRun(ACME, "no-body");

        // Goi KHONG kem Content-Type. Truoc khi sua thi day la 415/500.
        given().header(TENANT, ACME)
                .when()
                .post("/api/runs/" + runId + "/complete")
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));
    }

    @Test
    @DisplayName("health endpoint cua Quarkus bao UP (co check datasource)")
    void healthIsUp() {
        given().when().get("/q/health").then().statusCode(200).body("status", equalTo("UP"));
    }
}
