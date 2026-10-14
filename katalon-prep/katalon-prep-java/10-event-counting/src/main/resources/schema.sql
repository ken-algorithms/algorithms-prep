-- Event counting, the accepted design (doc 04 §4–§6). Plain PostgreSQL 16: no TimescaleDB needed.
-- Every statement is idempotent, so the app can run this file on every start.

-- ══ SOURCE OF TRUTH: one row per key-value item ══════════════════════════════════════════════
-- Partitioned by EVENT time, one partition per UTC day (PartitionManager creates them).
-- Retention = DROP the old partition: O(1), no dead tuples, no vacuum storm.
CREATE TABLE IF NOT EXISTS raw_events (
    event_id   uuid        NOT NULL,   -- deterministic from (tenant, batch_id, index): the idempotency key
    ts         timestamptz NOT NULL,   -- EVENT time sent by the client, never now()
    tenant_id  text        NOT NULL,
    key_name   text        NOT NULL,
    outcome    smallint    NOT NULL,   -- 0 false, 1 true, 2 fake_key, 3 malformed
    PRIMARY KEY (event_id, ts)         -- a partitioned table's unique key must contain the partition column
) PARTITION BY RANGE (ts);

-- read path only touches raw for < 1 minute edges and the unsealed tail; reconcile scans one hour
CREATE INDEX IF NOT EXISTS raw_events_tenant_ts ON raw_events (tenant_id, ts);

-- ══ DERIVED: three rollup tiers, always recomputable from raw ═══════════════════════════════
CREATE TABLE IF NOT EXISTS agg_1m (
    tenant_id text        NOT NULL,
    bucket    timestamptz NOT NULL,
    outcome   smallint    NOT NULL,
    cnt       bigint      NOT NULL,
    PRIMARY KEY (tenant_id, bucket, outcome)
);
CREATE TABLE IF NOT EXISTS agg_1h (LIKE agg_1m INCLUDING ALL);
CREATE TABLE IF NOT EXISTS agg_1d (LIKE agg_1m INCLUDING ALL);

-- Everything before sealed_until is rolled up; the query reads raw only after it.
CREATE TABLE IF NOT EXISTS rollup_state (
    id           int         PRIMARY KEY CHECK (id = 1),
    sealed_until timestamptz NOT NULL
);
INSERT INTO rollup_state (id, sealed_until) VALUES (1, TIMESTAMPTZ '1970-01-01 00:00:00+00') ON CONFLICT (id) DO NOTHING;

-- Registry of valid keys; the app keeps a copy in memory and reloads it every minute.
CREATE TABLE IF NOT EXISTS valid_keys (key_name text PRIMARY KEY);

-- ══ REJECTED APPROACHES: kept only so the tests can show why they were rejected ════════════
-- Approach 1: one running total per tenant, updated under a lock at insert time.
CREATE TABLE IF NOT EXISTS naive_total (
    tenant_id     text PRIMARY KEY,
    true_cnt      bigint NOT NULL DEFAULT 0,
    false_cnt     bigint NOT NULL DEFAULT 0,
    fake_cnt      bigint NOT NULL DEFAULT 0,
    malformed_cnt bigint NOT NULL DEFAULT 0,
    updated_at    timestamptz
);
-- Approach 3: per-minute counters, incremented synchronously (cnt = cnt + n), no raw.
CREATE TABLE IF NOT EXISTS sync_agg_1m (LIKE agg_1m INCLUDING ALL);
