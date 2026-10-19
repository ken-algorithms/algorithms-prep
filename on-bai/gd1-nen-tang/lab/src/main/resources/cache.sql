-- Bai 08: nguon su that cho cache-aside. version tang moi lan ghi: cache dung no de biet gia tri nao moi hon.
CREATE TABLE IF NOT EXISTS product (
    id      bigint  PRIMARY KEY,
    name    text    NOT NULL,
    price   bigint  NOT NULL,
    version bigint  NOT NULL DEFAULT 1
);
