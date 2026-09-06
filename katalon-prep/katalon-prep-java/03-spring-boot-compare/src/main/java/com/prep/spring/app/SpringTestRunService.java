package com.prep.spring.app;

import com.prep.spring.api.SpringDtos;
import com.prep.spring.domain.ResultStatus;
import com.prep.spring.domain.RunStatus;
import com.prep.spring.domain.TestResultRow;
import com.prep.spring.domain.TestRun;
import com.prep.spring.repo.TestResultRepository;
import com.prep.spring.repo.TestRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================================
 * CONSTRUCTOR INJECTION + FIELD FINAL — sua dung loi so 1 cua project RCI.
 * ============================================================================
 *
 * <p>RCI dung {@code @Autowired} field o 15 cho. Hau qua: (1) object ton tai duoc o trang thai nua
 * voi, (2) test phai dung reflection hoac {@code @SpringBootTest} chi de set dependency,
 * (3) khong nhin ra duoc class phu thuoc vao bao nhieu thu (o day 3 la da nhieu).
 *
 * <p>Voi constructor injection: KHONG can {@code @Autowired} (Spring tu suy ra khi co 1 constructor),
 * field {@code final} -> compiler bao dam khong ai gan lai, va test chi viec
 * {@code new SpringTestRunService(fakeRuns, fakeResults, fixedClock)}.
 *
 * <p>{@code Clock} cung duoc inject — de test thoi gian khong can Thread.sleep (bai hoc module 08).
 */
@Service
public class SpringTestRunService {

    static final int BATCH_SIZE = 100;

    private final TestRunRepository runs;
    private final TestResultRepository results;
    private final Clock clock;

    public SpringTestRunService(TestRunRepository runs, TestResultRepository results, Clock clock) {
        this.runs = runs;
        this.results = results;
        this.clock = clock;
    }

    @Transactional
    public SpringDtos.RunResponse createRun(String tenantId, SpringDtos.CreateRunRequest request) {
        var run = new TestRun();
        run.setTenantId(tenantId);
        run.setSuiteName(request.suiteName());
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(Instant.now(clock));
        return toResponse(runs.save(run));
    }

    @Transactional
    public int submitResults(String tenantId, Long runId, SpringDtos.SubmitResultsRequest request) {
        TestRun run = requireRun(tenantId, runId);
        if (run.getStatus() != RunStatus.RUNNING) {
            throw new RunClosedException(runId, run.getStatus().name());
        }

        Instant now = Instant.now(clock);
        var rows = new ArrayList<TestResultRow>(request.results().size());
        for (SpringDtos.ResultItem item : request.results()) {
            var row = new TestResultRow();
            row.setRunId(runId);
            row.setTestName(item.testName());
            row.setStatus(normalise(item));
            row.setDurationMs(item.durationMs());
            row.setAttempts(item.attempts());
            row.setCreatedAt(now);
            rows.add(row);
        }
        // saveAll + spring.jpa.properties.hibernate.jdbc.batch_size -> JDBC batch.
        // Chia lo thu cong de persistence context khong phinh vo han.
        for (int i = 0; i < rows.size(); i += BATCH_SIZE) {
            results.saveAll(rows.subList(i, Math.min(i + BATCH_SIZE, rows.size())));
            results.flush();
        }
        return rows.size();
    }

    private static ResultStatus normalise(SpringDtos.ResultItem item) {
        return item.status() == ResultStatus.PASSED && item.attempts() > 1
                ? ResultStatus.FLAKY
                : item.status();
    }

    @Transactional
    public SpringDtos.RunResponse completeRun(String tenantId, Long runId) {
        TestRun run = requireRun(tenantId, runId);
        run.setStatus(RunStatus.COMPLETED);
        run.setFinishedAt(Instant.now(clock));
        return toResponse(runs.save(run));
    }

    @Transactional(readOnly = true)
    public SpringDtos.RunSummary summary(String tenantId, Long runId) {
        TestRun run = requireRun(tenantId, runId);

        var byStatus = new EnumMap<ResultStatus, Long>(ResultStatus.class);
        for (Object[] row : results.countByStatusRaw(runId)) {
            byStatus.put((ResultStatus) row[0], (Long) row[1]);
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long executed = total - byStatus.getOrDefault(ResultStatus.SKIPPED, 0L);
        long flaky = byStatus.getOrDefault(ResultStatus.FLAKY, 0L);
        double flakinessRate = executed == 0 ? 0.0 : (double) flaky / executed;

        return new SpringDtos.RunSummary(
                runId,
                run.getStatus(),
                total,
                byStatus,
                flakinessRate,
                results.percentileDuration(runId, 0.5),
                results.percentileDuration(runId, 0.95));
    }

    @Transactional(readOnly = true)
    public List<SpringDtos.RunResponse> list(String tenantId, RunStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        List<TestRun> found = status == null
                ? runs.findByTenantIdOrderByStartedAtDesc(tenantId, pageable)
                : runs.findByTenantIdAndStatusOrderByStartedAtDesc(tenantId, status, pageable);
        return found.stream().map(SpringTestRunService::toResponse).toList();
    }

    private TestRun requireRun(String tenantId, Long runId) {
        return runs.findByIdAndTenantId(runId, tenantId)
                .orElseThrow(() -> new RunNotFoundException(runId));
    }

    private static SpringDtos.RunResponse toResponse(TestRun run) {
        return new SpringDtos.RunResponse(
                run.getId(),
                run.getTenantId(),
                run.getSuiteName(),
                run.getStatus(),
                run.getStartedAt(),
                run.getFinishedAt());
    }

    public static class RunNotFoundException extends RuntimeException {
        public RunNotFoundException(Long runId) {
            super("test run " + runId + " not found");
        }
    }

    public static class RunClosedException extends RuntimeException {
        public RunClosedException(Long runId, String status) {
            super("run %d is %s, cannot accept more results".formatted(runId, status));
        }
    }
}
