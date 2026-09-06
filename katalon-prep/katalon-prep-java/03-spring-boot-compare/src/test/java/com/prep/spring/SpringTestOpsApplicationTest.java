package com.prep.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration test THAT voi PostgreSQL 16.4 that — KHONG Docker, KHONG Testcontainers.
 *
 * <p>Bình thường ta se dung {@code @Testcontainers} + {@code @ServiceConnection} (rat gon o Spring
 * Boot 3.1+). Nhung Testcontainers CAN Docker. {@code @DynamicPropertySource} + embedded-postgres
 * cho ket qua tuong duong ma khong can Docker.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpringTestOpsApplicationTest {

    private static final EmbeddedPostgres POSTGRES = startPostgres();
    private static final String TENANT = "X-Tenant-Id";
    private static final String ACME = "acme";

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder()
                    .setServerConfig("fsync", "off")
                    .setServerConfig("synchronous_commit", "off")
                    .start();
        } catch (IOException e) {
            throw new IllegalStateException("cannot start embedded postgres", e);
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:postgresql://localhost:%d/postgres".formatted(POSTGRES.getPort()));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private long createRun(String tenant, String suite) throws Exception {
        String body = mvc.perform(post("/api/runs")
                        .header(TENANT, tenant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("suiteName", suite))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private static Map<String, Object> result(String name, String status, int ms, int attempts) {
        return Map.of("testName", name, "status", status, "durationMs", ms, "attempts", attempts);
    }

    @Test
    @DisplayName("Flyway chay + happy path + FLAKY duoc tach khoi PASSED")
    void fullHappyPath() throws Exception {
        long runId = createRun(ACME, "checkout-suite");

        List<Map<String, Object>> results = List.of(
                result("login", "PASSED", 120, 1),
                result("search", "PASSED", 300, 1),
                result("checkout", "FAILED", 900, 1),
                result("profile", "SKIPPED", 0, 1),
                result("payment", "PASSED", 450, 3));

        mvc.perform(post("/api/runs/" + runId + "/results")
                        .header(TENANT, ACME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("results", results))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accepted").value(5));

        mvc.perform(get("/api/runs/" + runId + "/summary").header(TENANT, ACME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.byStatus.PASSED").value(2))
                .andExpect(jsonPath("$.byStatus.FLAKY").value(1))
                .andExpect(jsonPath("$.flakinessRate").value(0.25));
    }

    @Test
    @DisplayName("MULTI-TENANT: tenant khac -> 404 problem+json")
    void tenantIsolation() throws Exception {
        long runId = createRun(ACME, "private");

        mvc.perform(get("/api/runs/" + runId + "/summary").header(TENANT, "other-corp"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("validation body -> 400 kem DANH SACH loi (ProblemDetail cua Spring 6)")
    void bodyValidation() throws Exception {
        long runId = createRun(ACME, "v");

        String body = mvc.perform(post("/api/runs/" + runId + "/results")
                        .header(TENANT, ACME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                Map.of("results", List.of(result("", "PASSED", -5, 0))))))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(json.readTree(body).get("errors")).hasSizeGreaterThan(2);
    }

    @Test
    @DisplayName("batch > 10.000 -> 400 sach se, khong 500/OOM")
    void oversizedBatch() throws Exception {
        long runId = createRun(ACME, "huge");
        var tooMany = new ArrayList<Map<String, Object>>(10_001);
        for (int i = 0; i < 10_001; i++) {
            tooMany.add(result("t" + i, "PASSED", 1, 1));
        }

        mvc.perform(post("/api/runs/" + runId + "/results")
                        .header(TENANT, ACME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("results", tooMany))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("run COMPLETED khong nhan them -> 409")
    void completedRunRejectsResults() throws Exception {
        long runId = createRun(ACME, "closed");

        // Spring khong doi Content-Type cho endpoint khong co body -> khong gap bug nhu Quarkus.
        mvc.perform(post("/api/runs/" + runId + "/complete").header(TENANT, ACME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(post("/api/runs/" + runId + "/results")
                        .header(TENANT, ACME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                Map.of("results", List.of(result("late", "PASSED", 1, 1))))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("pageSize bi gioi han 100")
    void pageSizeCapped() throws Exception {
        createRun(ACME, "p1");

        mvc.perform(get("/api/runs").header(TENANT, ACME).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));

        mvc.perform(get("/api/runs").header(TENANT, ACME).param("size", "1000000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("run rong -> flakinessRate 0.0, khong NaN")
    void emptyRunNoNaN() throws Exception {
        long runId = createRun(ACME, "empty");

        mvc.perform(get("/api/runs/" + runId + "/summary").header(TENANT, ACME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.flakinessRate").value(0.0));
    }

    @Test
    @DisplayName("thieu header tenant -> 400, KHONG phai 500")
    void missingTenantHeader() throws Exception {
        mvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("suiteName", "x"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Spring MVC tu tra 405/415 dung — advice cua ta KHONG nuot chung")
    void frameworkErrorsKeepTheirStatus() throws Exception {
        // Day la bug toi da gap o module 02 (Quarkus): catch-all bien 415 thanh 500.
        // O Spring, chi bat exception CUA MINH nen khong gap van de do.
        mvc.perform(delete("/api/runs").header(TENANT, ACME))
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(post("/api/runs")
                        .header(TENANT, ACME)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not json"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("actuator health UP + virtual threads dang bat")
    void healthAndVirtualThreads() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
