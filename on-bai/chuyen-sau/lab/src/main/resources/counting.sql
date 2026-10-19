-- Bai 08: kho so dem. Moi o = (tang, bucket) -> 4 con dem. Aggregator cong DELTA vao ca 4 tang trong cung
-- mot transaction voi offset Kafka, nen o phut luon bang tong 60 o giay cua no, o gio bang tong 60 o phut...
CREATE TABLE IF NOT EXISTS count_cell (
    tier    smallint NOT NULL,             -- 0 giay, 1 phut, 2 gio, 3 ngay (Tier.ordinal)
    bucket  bigint   NOT NULL,             -- giay epoch cua dau bucket (UTC)
    t       bigint   NOT NULL DEFAULT 0,   -- TRUE
    f       bigint   NOT NULL DEFAULT 0,   -- FALSE
    fk      bigint   NOT NULL DEFAULT 0,   -- FAKE_KEY
    mal     bigint   NOT NULL DEFAULT 0,   -- MALFORMED
    PRIMARY KEY (tier, bucket)
);

-- Offset Kafka KHONG luu o Kafka ma luu o day, cung transaction voi so dem: hai thu cung ghi hoac cung khong.
-- next_offset = offset ke tiep phai doc. Cap nhat co dieu kien (= gia tri cu) de chan aggregator "xac song".
CREATE TABLE IF NOT EXISTS agg_offset (
    topic        text   NOT NULL,
    part         int    NOT NULL,
    next_offset  bigint NOT NULL,
    PRIMARY KEY (topic, part)
);
