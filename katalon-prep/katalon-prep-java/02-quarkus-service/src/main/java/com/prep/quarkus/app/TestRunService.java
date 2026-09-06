package com.prep.quarkus.app;

import com.prep.quarkus.api.Dtos;
import com.prep.quarkus.domain.ResultStatus;
import com.prep.quarkus.domain.RunStatus;
import com.prep.quarkus.domain.TestResultRow;
import com.prep.quarkus.domain.TestRun;
import com.prep.quarkus.repo.TestResultRepository;
import com.prep.quarkus.repo.TestRunRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tang application: dieu phoi, giu bien gioi transaction. KHONG co logic HTTP o day.
 *
 * <p>Constructor injection + field final - bai hoc so 1 tu module 01 (RCI dung @Autowired field
 * o 15 cho). Nho vay class nay test duoc bang fake repository, khong can Quarkus.
 */
@ApplicationScoped
public class TestRunService {

    /** Batch size khop voi hibernate.jdbc.batch_size trong application.properties. */
    static final int BATCH_SIZE = 100;

    private final TestRunRepository runs;
    private final TestResultRepository results;
    private final EntityManager em;
    private final Clock clock;

    public TestRunService(
            TestRunRepository runs, TestResultRepository results, EntityManager em) {
        this.runs = runs;
        this.results = results;
        this.em = em;
        // Clock injectable de test duoc thoi gian - cung bai hoc voi module 08.
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public Dtos.RunResponse createRun(String tenantId, Dtos.CreateRunRequest request) {
        var run = new TestRun();
        run.tenantId = tenantId;
        run.suiteName = request.suiteName();
        run.status = RunStatus.RUNNING;
        run.startedAt = Instant.now(clock);
        runs.persist(run);
        return toResponse(run);
    }

    /**
     * Nhan batch ket qua. Mot transaction cho ca batch.
     *
     * <p>Quyet dinh thiet ke can noi ro: mot transaction cho 10.000 dong nghia la all-or-nothing.
     * Danh doi: don gian va nhat quan, nhung transaction dai giu lock lau va sinh nhieu WAL.
     * O quy mo lon hon, ta se chia thanh nhieu transaction nho + idempotency key de retry an toan
     * (xem IdempotencyStore o module 08).
     */
    @Transactional
    public int submitResults(String tenantId, Long runId, Dtos.SubmitResultsRequest request) {
        TestRun run = requireRun(tenantId, runId);
        if (run.status != RunStatus.RUNNING) {
            throw new IllegalStateException(
                    "run %d is %s, cannot accept more results".formatted(runId, run.status));
        }

        Instant now = Instant.now(clock);
        var rows = new ArrayList<TestResultRow>(request.results().size());
        for (Dtos.ResultItem item : request.results()) {
            var row = new TestResultRow();
            row.runId = runId;
            row.testName = item.testName();
            // Normalise: pass sau retry = FLAKY, khong phai PASSED. Xem PATTERNS.md module 01.
            row.status = normaliseStatus(item);
            row.durationMs = item.durationMs();
            row.attempts = item.attempts();
            row.createdAt = now;
            rows.add(row);
        }
        results.persistBatch(rows, BATCH_SIZE, em);
        return rows.size();
    }

    private static ResultStatus normaliseStatus(Dtos.ResultItem item) {
        if (item.status() == ResultStatus.PASSED && item.attempts() > 1) {
            return ResultStatus.FLAKY;
        }
        return item.status();
    }

    @Transactional
    public Dtos.RunResponse completeRun(String tenantId, Long runId) {
        TestRun run = requireRun(tenantId, runId);
        run.status = RunStatus.COMPLETED;
        run.finishedAt = Instant.now(clock);
        return toResponse(run);
    }

    @Transactional
    public Dtos.RunSummary summary(String tenantId, Long runId) {
        TestRun run = requireRun(tenantId, runId);
        Map<ResultStatus, Long> byStatus = results.countByStatus(runId);

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long executed = total - byStatus.getOrDefault(ResultStatus.SKIPPED, 0L);
        long flaky = byStatus.getOrDefault(ResultStatus.FLAKY, 0L);
        // Chia 0 -> tra 0.0, KHONG tra NaN. Bai hoc tu module 00.
        double flakinessRate = executed == 0 ? 0.0 : (double) flaky / executed;

        return new Dtos.RunSummary(
                runId,
                run.status,
                total,
                byStatus,
                flakinessRate,
                results.percentileDuration(runId, 0.5),
                results.percentileDuration(runId, 0.95));
    }

    @Transactional
    public List<Dtos.RunResponse> list(String tenantId, RunStatus status, int page, int size) {
        return runs.findPage(tenantId, status, page, size).stream()
                .map(TestRunService::toResponse)
                .toList();
    }

    private TestRun requireRun(String tenantId, Long runId) {
        return runs.findByIdForTenant(runId, tenantId)
                .orElseThrow(() -> new RunNotFoundException(runId));
    }

    private static Dtos.RunResponse toResponse(TestRun run) {
        return new Dtos.RunResponse(
                run.id, run.tenantId, run.suiteName, run.status, run.startedAt, run.finishedAt);
    }

    /** Exception cua DOMAIN, khong phai cua HTTP. Mapper se dich sang 404. */
    public static class RunNotFoundException extends RuntimeException {
        public RunNotFoundException(Long runId) {
            super("test run " + runId + " not found");
        }
    }
}
