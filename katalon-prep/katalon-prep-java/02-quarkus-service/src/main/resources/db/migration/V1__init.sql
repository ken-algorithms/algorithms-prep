-- Flyway la nguon su that duy nhat cho schema.
-- KHONG BAO GIO dung hibernate ddl-auto=update o production: no khong versioned,
-- khong review duoc, khong rollback duoc, va co the drop cot khi ban doi ten field.

CREATE TABLE test_run (
    id          bigserial   PRIMARY KEY,
    tenant_id   varchar(100) NOT NULL,
    suite_name  varchar(200) NOT NULL,
    status      varchar(20)  NOT NULL,
    started_at  timestamptz  NOT NULL,
    finished_at timestamptz
);

-- Index composite (tenant_id, started_at DESC): phuc vu dung query list cua API.
-- Thu tu cot theo quy tac leftmost prefix - xem module 05 Lab 01 test 3.
CREATE INDEX idx_test_run_tenant_started ON test_run (tenant_id, started_at DESC);

-- SEQUENCE (khong dung IDENTITY) de Hibernate co the BATCH insert.
-- IDENTITY buoc Hibernate insert tung dong mot de lay id -> batch bi vo hoan toan.
CREATE SEQUENCE test_result_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE test_result (
    id          bigint      PRIMARY KEY,
    run_id      bigint      NOT NULL REFERENCES test_run (id) ON DELETE CASCADE,
    test_name   varchar(500) NOT NULL,
    status      varchar(20)  NOT NULL,
    duration_ms integer      NOT NULL,
    attempts    integer      NOT NULL DEFAULT 1,
    created_at  timestamptz  NOT NULL
);

CREATE INDEX idx_test_result_run ON test_result (run_id);
CREATE INDEX idx_test_result_created ON test_result (created_at);

-- Partial index: chi 5% dong la FAILED -> index nho hon ~17x (do duoc o module 05 Lab 01 test 4).
CREATE INDEX idx_test_result_failed ON test_result (run_id) WHERE status = 'FAILED';
