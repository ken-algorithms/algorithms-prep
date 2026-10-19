-- Bai 10: so cai kep tren Postgres, dung tung buoc. Tien la so nguyen don vi nho nhat (dong): bigint, khong float.
CREATE TABLE IF NOT EXISTS l_account (
    id              bigint  PRIMARY KEY,
    allow_negative  boolean NOT NULL DEFAULT false,   -- tai khoan he thong (quy, doanh thu) duoc am
    balance         bigint  NOT NULL DEFAULT 0,       -- so du chay = SUM(posting), doi CUNG transaction voi posting
    held            bigint  NOT NULL DEFAULT 0,       -- tong tien dang tam giu (buoc 6): kha dung = balance - held
    last_seq        bigint  NOT NULL DEFAULT 0,       -- seq cua posting cuoi; tang duoi khoa dong
    CHECK (allow_negative OR balance - held >= 0)     -- lop cuoi: DB tu choi neu code co lo hong
);

CREATE TABLE IF NOT EXISTS l_journal (
    id            bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idem_key      text        NOT NULL UNIQUE,         -- khoa nghiep vu cua lenh: retry cung key = cung lenh
    request_hash  text        NOT NULL,                -- cung key ma khac noi dung -> tu choi
    created_at    timestamptz NOT NULL DEFAULT clock_timestamp()
);

CREATE TABLE IF NOT EXISTS l_posting (
    id             bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    journal_id     bigint      NOT NULL REFERENCES l_journal(id),
    account_id     bigint      NOT NULL REFERENCES l_account(id),
    seq            bigint      NOT NULL,               -- 1, 2, 3... rieng tung tai khoan, khong ho
    amount         bigint      NOT NULL CHECK (amount <> 0),
    balance_after  bigint      NOT NULL,               -- so du ngay sau dong nay: sao ke, so du tai thoi diem
    created_at     timestamptz NOT NULL DEFAULT clock_timestamp(),
    UNIQUE (account_id, seq)
);
CREATE INDEX IF NOT EXISTS l_posting_account_time ON l_posting (account_id, created_at);

CREATE TABLE IF NOT EXISTS l_hold (
    id          bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id  bigint      NOT NULL REFERENCES l_account(id),
    amount      bigint      NOT NULL CHECK (amount > 0),
    status      text        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CAPTURED', 'RELEASED')),
    expires_at  timestamptz NOT NULL
);

-- Buoc 7b: ca lenh chuyen trong MOT lan goi toi DB. Khoa dong chi bi giu trong luc ham chay + COMMIT,
-- khong con cho round trip mang giua cac cau lenh. Tra id journal; thieu tien hoac key dung lai thi RAISE (rollback ca lenh).
CREATE OR REPLACE FUNCTION l_transfer(p_key text, p_from bigint, p_to bigint, p_amount bigint)
RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE
    j bigint;
    h text := p_from || '>' || p_to || ':' || p_amount;
    leg record;
    s bigint;
    b bigint;
BEGIN
    INSERT INTO l_journal (idem_key, request_hash) VALUES (p_key, h)
    ON CONFLICT (idem_key) DO NOTHING RETURNING id INTO j;
    IF j IS NULL THEN                                   -- lenh da chay: phat lai
        SELECT id INTO j FROM l_journal WHERE idem_key = p_key AND request_hash = h;
        IF j IS NULL THEN RAISE EXCEPTION 'idempotency key % da dung cho mot lenh KHAC', p_key; END IF;
        RETURN j;
    END IF;
    FOR leg IN SELECT * FROM (VALUES (p_from, -p_amount), (p_to, p_amount)) v(acc, amt) ORDER BY acc LOOP
        UPDATE l_account SET balance = balance + leg.amt, last_seq = last_seq + 1
         WHERE id = leg.acc AND (allow_negative OR balance - held + leg.amt >= 0)
        RETURNING last_seq, balance INTO s, b;
        IF NOT FOUND THEN RAISE EXCEPTION USING ERRCODE = 'P0001', MESSAGE = 'khong du tien'; END IF;
        INSERT INTO l_posting (journal_id, account_id, seq, amount, balance_after) VALUES (j, leg.acc, s, leg.amt, b);
    END LOOP;
    RETURN j;
END $$;
