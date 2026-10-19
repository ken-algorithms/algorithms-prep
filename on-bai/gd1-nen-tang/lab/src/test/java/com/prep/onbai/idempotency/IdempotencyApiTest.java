package com.prep.onbai.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.prep.onbai.testsupport.Pg;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Bai 02 — idempotency key qua HTTP that (MockMvc), Postgres that, PSP gia tiem loi duoc. */
@SpringBootTest(classes = IdempotencyApp.class, properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:idempotency.sql",
        "onbai.idempotency.lease-ms=400"})
@AutoConfigureMockMvc
class IdempotencyApiTest {

    static final String URL = Pg.freshDatabaseUrl("b02_idempotency");
    static final String BODY = "{\"customerId\":\"c-42\",\"amountMinor\":150000,\"currency\":\"VND\"}";

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> URL);
        r.add("spring.datasource.username", () -> "postgres");
    }

    @Autowired MockMvc mvc;
    @Autowired FakePsp psp;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void reset() {
        psp.reset();
    }

    MockHttpServletResponse pay(String key, String body) throws Exception {
        var req = post("/payments").contentType(MediaType.APPLICATION_JSON).content(body);
        if (key != null) {
            req.header("Idempotency-Key", key);
        }
        return mvc.perform(req).andReturn().getResponse();
    }

    long payments(String key) {
        return jdbc.queryForObject("SELECT count(*) FROM payment WHERE idem_key = ?", Long.class, key);
    }

    @Test
    void secondCallWithTheSameKeyReplaysTheFirstResponseByteForByte() throws Exception {
        String key = UUID.randomUUID().toString();
        MockHttpServletResponse first = pay(key, BODY);
        MockHttpServletResponse again = pay(key, BODY);

        System.out.println("[LAB] lan 1: " + first.getStatus() + " " + first.getContentAsString());
        System.out.println("[LAB] lan 2: " + again.getStatus() + " Idempotent-Replayed=" + again.getHeader("Idempotent-Replayed"));
        assertThat(first.getStatus()).isEqualTo(201);
        assertThat(again.getStatus()).isEqualTo(201);
        assertThat(again.getContentAsString()).isEqualTo(first.getContentAsString());
        assertThat(again.getHeader("Idempotent-Replayed")).isEqualTo("true");
        assertThat(psp.charges()).isEqualTo(1);
        assertThat(payments(key)).isEqualTo(1);
    }

    @Test
    void sameKeyWithADifferentBodyIs422AndAMissingKeyIs400() throws Exception {
        String key = UUID.randomUUID().toString();
        pay(key, BODY);
        assertThat(pay(key, BODY.replace("150000", "990000")).getStatus()).isEqualTo(422);
        assertThat(pay(null, BODY).getStatus()).isEqualTo(400);
        assertThat(psp.charges()).isEqualTo(1);
    }

    @Test
    void tenConcurrentDuplicatesChargeExactlyOnce() throws Exception {
        String key = UUID.randomUUID().toString();
        psp.nextLatencies(300); // request dau giu key 300 ms
        int n = 10;
        CyclicBarrier go = new CyclicBarrier(n);
        List<Future<MockHttpServletResponse>> fs = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(n)) {
            for (int i = 0; i < n; i++) {
                fs.add(pool.submit(() -> {
                    go.await();
                    return pay(key, BODY);
                }));
            }
        }
        Map<String, Integer> codes = new TreeMap<>();
        for (Future<MockHttpServletResponse> f : fs) {
            MockHttpServletResponse r = f.get();
            String label = r.getStatus() + ("true".equals(r.getHeader("Idempotent-Replayed")) ? " replay" : "");
            codes.merge(label, 1, Integer::sum);
        }
        System.out.println("[LAB] 10 request trung key cung luc: " + codes + ", PSP tru tien " + psp.charges() + " lan");
        assertThat(codes.getOrDefault("201", 0)).isEqualTo(1);
        assertThat(codes.getOrDefault("409", 0) + codes.getOrDefault("201 replay", 0)).isEqualTo(n - 1);
        assertThat(psp.charges()).isEqualTo(1);
        assertThat(payments(key)).isEqualTo(1);
    }

    @Test
    void aDeclinedCardIsAStoredOutcomeNotARetryableError() throws Exception {
        String key = UUID.randomUUID().toString();
        psp.decline(true);
        MockHttpServletResponse first = pay(key, BODY);
        psp.decline(false); // khach nap tien vao the... nhung CUNG key van tra ket qua cu
        MockHttpServletResponse again = pay(key, BODY);
        assertThat(first.getStatus()).isEqualTo(402);
        assertThat(again.getStatus()).isEqualTo(402);
        assertThat(again.getHeader("Idempotent-Replayed")).isEqualTo("true");
        assertThat(psp.calls()).isEqualTo(1);
    }

    @Test
    void pspDownMeansNothingHappenedSoTheKeyIsReleased() throws Exception {
        String key = UUID.randomUUID().toString();
        psp.down(1);
        assertThat(pay(key, BODY).getStatus()).isEqualTo(503);
        assertThat(pay(key, BODY).getStatus()).isEqualTo(201); // retry ngay, chay that
        assertThat(psp.charges()).isEqualTo(1);
    }

    @Test
    void aTimeoutAfterTheChargeIsRetriedSafelyAfterTheLease() throws Exception {
        String key = UUID.randomUUID().toString();
        psp.timeoutAfterCharge(1); // PSP da tru tien, nhung cau tra loi bi mat
        MockHttpServletResponse first = pay(key, BODY);
        MockHttpServletResponse tooEarly = pay(key, BODY);
        Thread.sleep(500); // > lease 400 ms
        MockHttpServletResponse later = pay(key, BODY);
        System.out.printf("[LAB] timeout sau khi tru tien: %d -> retry ngay %d (Retry-After %ss) -> sau lease %d, PSP tru tien %d lan%n",
                first.getStatus(), tooEarly.getStatus(), tooEarly.getHeader("Retry-After"), later.getStatus(), psp.charges());
        assertThat(first.getStatus()).isEqualTo(504);
        assertThat(tooEarly.getStatus()).isEqualTo(409);
        assertThat(later.getStatus()).isEqualTo(201);
        assertThat(psp.charges()).isEqualTo(1); // lan goi thu hai, CUNG key -> PSP tra charge cu
        assertThat(payments(key)).isEqualTo(1);
    }

    @Test
    void aSlowOwnerWhoseLeaseExpiredIsFencedOff() throws Exception {
        String key = UUID.randomUUID().toString();
        psp.nextLatencies(900, 0); // chu dau tien cham hon lease 400 ms
        MockHttpServletResponse slow;
        MockHttpServletResponse takeover;
        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            Future<MockHttpServletResponse> a = pool.submit(() -> pay(key, BODY));
            Thread.sleep(500); // lease cua A da het, A van dang cho PSP
            takeover = pay(key, BODY);
            slow = a.get();
        }
        System.out.printf("[LAB] fencing: B tiep quan -> %d, A cham toi sau -> %d %s; payment = %d, PSP tru tien %d lan%n",
                takeover.getStatus(), slow.getStatus(), slow.getContentAsString(), payments(key), psp.charges());
        assertThat(takeover.getStatus()).isEqualTo(201);
        assertThat(slow.getStatus()).isEqualTo(409);
        assertThat(payments(key)).isEqualTo(1);
        assertThat(psp.charges()).isEqualTo(1);
    }
}
