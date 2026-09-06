package com.prep.spring.api;

import com.prep.spring.domain.ResultStatus;
import com.prep.spring.domain.RunStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** DTO GIONG HET module 02 -> so sanh API 1:1 duoc. */
public final class SpringDtos {

    private SpringDtos() {}

    public record CreateRunRequest(@NotBlank @Size(max = 200) String suiteName) {}

    public record RunResponse(
            Long id, String tenantId, String suiteName, RunStatus status, Instant startedAt, Instant finishedAt) {}

    public record ResultItem(
            @NotBlank @Size(max = 500) String testName,
            @NotNull ResultStatus status,
            @Min(0) int durationMs,
            @Min(1) int attempts) {}

    public record SubmitResultsRequest(
            @NotNull @Size(min = 1, max = 10_000) List<@Valid ResultItem> results) {}

    public record RunSummary(
            Long runId,
            RunStatus status,
            long total,
            Map<ResultStatus, Long> byStatus,
            double flakinessRate,
            Integer p50DurationMs,
            Integer p95DurationMs) {}
}
