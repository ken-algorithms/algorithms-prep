-- Bai 02: idempotency key cho API thanh toan co goi ra cong thanh toan (PSP) ben ngoai.
CREATE TABLE IF NOT EXISTS idempotency_record (
    scope         text        NOT NULL,      -- client/merchant: key chi can duy nhat TRONG pham vi cua ho
    key           text        NOT NULL,
    request_hash  text        NOT NULL,      -- cung key, khac noi dung -> 422
    status        text        NOT NULL CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    owner_token   uuid        NOT NULL,      -- ai dang giu lease; doi khi bi tiep quan (fencing token)
    locked_until  timestamptz NOT NULL,      -- lease: qua han -> request sau duoc tiep quan
    response_code int,
    response_body text,                      -- tra lai NGUYEN VAN khi replay
    created_at    timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (scope, key)
);

CREATE TABLE IF NOT EXISTS payment (
    id            uuid        PRIMARY KEY,
    scope         text        NOT NULL,
    idem_key      text        NOT NULL,
    amount_minor  bigint      NOT NULL CHECK (amount_minor > 0),   -- tien = so nguyen don vi nho nhat
    currency      text        NOT NULL,
    psp_charge_id text        NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),
    UNIQUE (scope, idem_key)                 -- luoi an toan cuoi: mot key, mot payment
);
