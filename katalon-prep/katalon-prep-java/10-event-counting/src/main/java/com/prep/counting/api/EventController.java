package com.prep.counting.api;

import com.prep.counting.domain.EventBatch;
import com.prep.counting.ingest.IngestResult;
import com.prep.counting.ingest.IngestService;
import com.prep.counting.query.Stats;
import com.prep.counting.query.StatsService;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventController {

    private final IngestService ingest;
    private final StatsService stats;

    public EventController(IngestService ingest, StatsService stats) {
        this.ingest = ingest;
        this.stats = stats;
    }

    /** 202, not 200: stored, but not counted yet. The counts show up within a minute. */
    @PostMapping("/events")
    public ResponseEntity<IngestResult> ingest(@RequestBody EventBatch batch) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ingest.ingest(batch));
    }

    /** Any half-open range [from, to), e.g. /stats?tenant=t1&from=2026-08-26T03:17:30Z&to=2026-08-29T11:42:10Z */
    @GetMapping("/stats")
    public Stats stats(@RequestParam String tenant, @RequestParam Instant from, @RequestParam Instant to) {
        return stats.query(tenant, from, to);
    }
}
