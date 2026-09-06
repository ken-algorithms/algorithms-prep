package com.prep.spring.api;

import com.prep.spring.app.SpringTestRunService;
import com.prep.spring.domain.RunStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Cung contract voi module 02. @Validated de validate duoc @Min/@Max tren @RequestParam. */
@RestController
@RequestMapping("/api/runs")
@Validated
public class SpringTestRunController {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final SpringTestRunService service;

    public SpringTestRunController(SpringTestRunService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SpringDtos.RunResponse> create(
            @RequestHeader(TENANT_HEADER) String tenantId,
            @Valid @RequestBody SpringDtos.CreateRunRequest request) {
        var created = service.createRun(tenantId, request);
        return ResponseEntity.created(URI.create("/api/runs/" + created.id())).body(created);
    }

    @PostMapping("/{runId}/results")
    public ResponseEntity<Map<String, Integer>> submitResults(
            @RequestHeader(TENANT_HEADER) String tenantId,
            @PathVariable Long runId,
            @Valid @RequestBody SpringDtos.SubmitResultsRequest request) {
        int accepted = service.submitResults(tenantId, runId, request);
        return ResponseEntity.accepted().body(Map.of("accepted", accepted));
    }

    @PostMapping("/{runId}/complete")
    public SpringDtos.RunResponse complete(
            @RequestHeader(TENANT_HEADER) String tenantId, @PathVariable Long runId) {
        return service.completeRun(tenantId, runId);
    }

    @GetMapping("/{runId}/summary")
    public SpringDtos.RunSummary summary(
            @RequestHeader(TENANT_HEADER) String tenantId, @PathVariable Long runId) {
        return service.summary(tenantId, runId);
    }

    @GetMapping
    public List<SpringDtos.RunResponse> list(
            @RequestHeader(TENANT_HEADER) String tenantId,
            @RequestParam(required = false) RunStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(tenantId, status, page, size);
    }
}
