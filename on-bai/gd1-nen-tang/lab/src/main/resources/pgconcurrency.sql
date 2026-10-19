-- Bai 04: cac di thuong dong thoi tren Postgres that.
CREATE TABLE IF NOT EXISTS counter_account (
    id      int PRIMARY KEY,
    balance int NOT NULL,
    version int NOT NULL DEFAULT 0          -- cho optimistic locking (compare-and-set)
);

-- Write skew: tai khoan chung cua mot ho gia dinh. Quy tac: TONG so du cua ho >= 0.
-- Quy tac nam tren NHIEU dong -> khong CHECK constraint nao cua mot dong bieu dien duoc.
CREATE TABLE IF NOT EXISTS household (id int PRIMARY KEY);   -- dong "vat chi diem" de khoa ca ho
CREATE TABLE IF NOT EXISTS member_account (
    id        int PRIMARY KEY,
    household int NOT NULL REFERENCES household(id),
    balance   int NOT NULL
);
