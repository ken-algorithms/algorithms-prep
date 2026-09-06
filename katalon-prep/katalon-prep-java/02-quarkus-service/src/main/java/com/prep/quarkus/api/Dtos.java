package com.prep.quarkus.api;

import com.prep.quarkus.domain.ResultStatus;
import com.prep.quarkus.domain.RunStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO tach khoi entity - KHONG tra entity truc tiep ra API.
 *
 * <p>Ba ly do: (1) doi schema DB khong pha vo API contract cua khach; (2) khong lo ro ri field noi
 * bo; (3) tranh loi lazy-loading serialization kinh dien cua JPA.
 */
public final class Dtos {

    private Dtos() {}

    public record CreateRunRequest(
            @NotBlank(message = "suiteName must not be blank") @Size(max = 200) String suiteName) {}

    public record RunResponse(
            Long id, String tenantId, String suiteName, RunStatus status, Instant startedAt, Instant finishedAt) {}

    public record ResultItem(
            @NotBlank @Size(max = 500) String testName,
            @NotNull ResultStatus status,
            @Min(value = 0, message = "durationMs must be >= 0") int durationMs,
            @Min(value = 1, message = "attempts must be >= 1") int attempts) {}

    /**
     * Batch co GIOI HAN. Khong gioi han = mot client gui 10 trieu dong lam OOM ca service.
     * Gioi han phai duoc DOCUMENT va tra loi 400 ro rang, khong phai 500.
     */
    public record SubmitResultsRequest(
            @NotNull @Size(min = 1, max = 10_000, message = "results must contain 1..10000 items")
                    List<@Valid ResultItem> results) {}

    public record RunSummary(
            Long runId,
            RunStatus status,
            long total,
            Map<ResultStatus, Long> byStatus,
            double flakinessRate,
            Integer p50DurationMs,
            Integer p95DurationMs) {}

    /** RFC 7807 problem+json - chuan de bao loi HTTP, thay vi tra string tu do. */
    public record Problem(String type, String title, int status, String detail, List<String> errors) {
        public Problem {
            errors = errors == null ? List.of() : List.copyOf(errors);
        }
    }
}
