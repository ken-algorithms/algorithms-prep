package com.prep.kafka.api;

import com.prep.kafka.model.EventBatch;
import com.prep.kafka.model.Outcome;
import com.prep.kafka.producer.EventProducer;
import com.prep.kafka.repository.RollupRepository;
import com.prep.kafka.resilience.ProducerRateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/events")
public class EventIngestionController {

    public static final String DEFAULT_TOPIC = "events.ingest";

    private final EventProducer producer;
    private final ProducerRateLimiter rateLimiter;
    private final RollupRepository rollupRepo;

    public EventIngestionController(EventProducer producer,
                                    ProducerRateLimiter rateLimiter,
                                    RollupRepository rollupRepo) {
        this.producer = producer;
        this.rateLimiter = rateLimiter;
        this.rollupRepo = rollupRepo;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> ingestBatch(@RequestBody EventBatch batch) {
        if (batch == null || batch.tenantId() == null || batch.batchId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid event batch payload"));
        }

        // 1. Ap dung Backpressure / Rate Limiter bao ve Ingestion API
        if (!rateLimiter.tryAcquire(batch.tenantId())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit exceeded for tenant " + batch.tenantId()));
        }

        // 2. Day bat dong bo vao Kafka voi key = batch_id (chia deu partition)
        producer.send(DEFAULT_TOPIC, batch, EventProducer.KeyStrategy.BATCH_ID);

        // 3. Tra ve 202 Accepted dung chuan Event Collector
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "status", "ACCEPTED",
                        "batchId", batch.batchId(),
                        "itemsCount", batch.items().size()
                ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam String tenantId,
            @RequestParam long bucketMinute) {

        Map<String, Object> stats = new HashMap<>();
        long trueCnt = rollupRepo.getCount(tenantId, bucketMinute, Outcome.TRUE);
        long falseCnt = rollupRepo.getCount(tenantId, bucketMinute, Outcome.FALSE);
        long fakeKeyCnt = rollupRepo.getCount(tenantId, bucketMinute, Outcome.FAKE_KEY);
        long malformedCnt = rollupRepo.getCount(tenantId, bucketMinute, Outcome.MALFORMED);

        stats.put("tenantId", tenantId);
        stats.put("bucketMinute", bucketMinute);
        stats.put("trueCount", trueCnt);
        stats.put("falseCount", falseCnt);
        stats.put("fakeKeyCount", fakeKeyCnt);
        stats.put("malformedCount", malformedCnt);

        long valid = trueCnt + falseCnt;
        stats.put("successRate", valid > 0 ? (double) trueCnt / valid : 0.0);

        return ResponseEntity.ok(stats);
    }
}
