package com.prep.quarkus.domain;

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
 * Entity thuong (KHONG extends PanacheEntity).
 *
 * <p>Vi sao khong dung PanacheEntity (active record): active record tron logic truy van vao entity,
 * nen entity vua la model domain vua la DAO -> kho test, va vi pham SRP. Repository pattern
 * (xem {@code repo/}) tach hai vai tro do.
 *
 * <p>Quarkus khong ep ban chon cai nao. Nhung khi phong van hoi "Panache active record hay
 * repository?", cau tra loi tot la: active record cho CRUD don gian/prototype, repository khi co
 * logic truy van phuc tap va can mock trong unit test.
 */
@Entity
@Table(name = "test_run")
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "tenant_id", nullable = false)
    public String tenantId;

    @Column(name = "suite_name", nullable = false)
    public String suiteName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RunStatus status;

    @Column(name = "started_at", nullable = false)
    public Instant startedAt;

    @Column(name = "finished_at")
    public Instant finishedAt;
}
