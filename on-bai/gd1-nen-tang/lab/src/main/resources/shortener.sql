-- Bai 07: rut gon URL. Ma 7 ky tu base62 = 62^7 ~ 3,5 nghin ty ma.
CREATE SEQUENCE IF NOT EXISTS short_url_seq;
CREATE TABLE IF NOT EXISTS short_url (
    code       text        PRIMARY KEY,           -- unique index: luoi an toan cuoi cung
    long_url   text        NOT NULL,
    custom     boolean     NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now()
);
