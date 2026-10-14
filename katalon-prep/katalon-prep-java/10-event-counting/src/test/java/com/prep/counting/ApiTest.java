package com.prep.counting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.prep.counting.domain.KeyRegistry;
import com.prep.counting.rollup.RollupJob;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** The HTTP contract through the real Spring wiring: 202 on ingest, duplicates on retry, 422 when too late. */
@SpringBootTest(properties = "counting.jobs.enabled=false")
@AutoConfigureMockMvc
class ApiTest {

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> Pg.freshDatabaseUrl("api"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired KeyRegistry registry;
    @Autowired RollupJob rollup;

    @Test
    void ingestRetryAndQuery() throws Exception {
        registry.replace(Set.of("checkout_v2", "dark_mode"));
        Instant ts = Instant.now().minus(2, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
        String body = """
                {"tenantId":"t1","batchId":"b-42","ts":"%s","items":[
                  {"key":"checkout_v2","value":true},{"key":"dark_mode","value":false},
                  {"key":"nope","value":true},{"key":"dark_mode","value":"yes"}]}""".formatted(ts);

        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.inserted").value(4));
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.duplicates").value(4));

        rollup.refreshRecent();
        mvc.perform(get("/stats").param("tenant", "t1")
                        .param("from", ts.minusSeconds(3600).toString()).param("to", Instant.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trueCount").value(1))
                .andExpect(jsonPath("$.falseCount").value(1))
                .andExpect(jsonPath("$.fakeKey").value(1))
                .andExpect(jsonPath("$.malformed").value(1))
                .andExpect(jsonPath("$.successRate").value(0.5));

        String old = body.replace(ts.toString(), ts.minus(8, ChronoUnit.DAYS).toString());
        mvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON).content(old))
                .andExpect(status().isUnprocessableEntity());
    }
}
