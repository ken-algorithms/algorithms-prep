package com.prep.spring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "test_result")
public class TestResultRow {

    @Id
    // allocationSize PHAI khop INCREMENT BY 50 trong V1__init.sql.
    // Neu lech, Hibernate cap phat id chong len nhau -> loi trung khoa rat kho doan.
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_result_seq")
    @SequenceGenerator(name = "test_result_seq", sequenceName = "test_result_seq", allocationSize = 50)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultStatus status;

    @Column(name = "duration_ms", nullable = false)
    private int durationMs;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public void setStatus(ResultStatus status) {
        this.status = status;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
