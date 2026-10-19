-- Bai 09: transactional outbox cho notification. Su kien ghi CUNG transaction voi nghiep vu.
CREATE TABLE IF NOT EXISTS orders (
    id          bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer    text        NOT NULL,
    total_minor bigint      NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS outbox (
    id              bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    message_id      uuid        NOT NULL UNIQUE DEFAULT gen_random_uuid(),  -- consumer dedupe theo id nay
    aggregate_id    text        NOT NULL,      -- key khi publish -> cung partition -> giu thu tu theo don hang
    event_type      text        NOT NULL,
    payload         jsonb       NOT NULL,
    created_at      timestamptz NOT NULL DEFAULT clock_timestamp(),
    published_at    timestamptz,               -- NULL = chua phat. Trang thai, KHONG phai con tro id
    attempts        int         NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    last_error      text
);
-- index chi chua dong chua phat: nho, nam gon trong RAM du bang outbox lon
CREATE INDEX IF NOT EXISTS outbox_pending ON outbox (next_attempt_at, id) WHERE published_at IS NULL;

-- Consumer: da xu ly message nao roi (at-least-once -> idempotent consumer)
CREATE TABLE IF NOT EXISTS inbox (
    consumer     text        NOT NULL,
    message_id   uuid        NOT NULL,
    processed_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (consumer, message_id)
);
CREATE TABLE IF NOT EXISTS notification (
    id       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id bigint NOT NULL,
    channel  text   NOT NULL,
    body     text   NOT NULL
);
