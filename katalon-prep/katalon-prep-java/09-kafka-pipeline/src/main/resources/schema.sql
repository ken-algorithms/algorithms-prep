CREATE TABLE IF NOT EXISTS agg_1m (
    tenant_id VARCHAR(64) NOT NULL,
    bucket_minute BIGINT NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    cnt BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (tenant_id, bucket_minute, outcome)
);

CREATE TABLE IF NOT EXISTS processed_batch (
    batch_id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
