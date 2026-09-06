package com.prep.quarkus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Bang nay la bang LON (hang ty dong o quy mo Katalon).
 *
 * <p>Chu y: KHONG dung quan he @ManyToOne toi TestRun. Chi luu run_id dang bigint.
 * Ly do: voi bulk insert 10.000 dong, quan he JPA lam Hibernate phai load va quan ly
 * TestRun trong persistence context, sinh ra hang loat query khong can thiet. O bang lon,
 * "foreign key dang so" don gian va nhanh hon nhieu.
 */
@Entity
@Table(
        name = "test_result",
        indexes = {
            @Index(name = "idx_test_result_run", columnList = "run_id"),
            @Index(name = "idx_test_result_created", columnList = "created_at")
        })
public class TestResultRow {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "test_result_seq")
    public Long id;

    @Column(name = "run_id", nullable = false)
    public Long runId;

    @Column(name = "test_name", nullable = false)
    public String testName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ResultStatus status;

    @Column(name = "duration_ms", nullable = false)
    public int durationMs;

    @Column(nullable = false)
    public int attempts;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
