package com.prep.spring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * BAY DA GAP THAT (lan thu hai trong cung mot module):
 *
 * <p>Ban dau toi gom ca 4 kieu nay vao mot class {@code Entities} cho gon file. Hibernate dang ky
 * entity nested voi ten {@code Entities$TestResultRow}, nen JPQL {@code from TestResultRow r} nem
 * {@code UnknownEntityException: Could not resolve root entity 'TestResultRow'}.
 *
 * <p>Cong voi loi truoc do (repository nested khong duoc Spring Data tao proxy), bai hoc la mot:
 * <b>gom kieu vao holder class de tiet kiem file la di nguoc quy uoc framework</b>. Entity va
 * repository phai la kieu TOP-LEVEL. Ca hai loi deu chi lo ra khi CHAY, khong lo khi compile.
 */
@Entity
@Table(name = "test_run")
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "suite_name", nullable = false)
    private String suiteName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
