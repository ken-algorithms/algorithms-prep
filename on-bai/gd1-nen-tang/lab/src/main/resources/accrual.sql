-- Bai 06: khoan vay tren so cai cua bai 05 (chay SAU ledger.sql). Lai ngay tinh tren du no goc.
CREATE TABLE IF NOT EXISTS loan (
    id                bigint        PRIMARY KEY,
    principal_account bigint        NOT NULL UNIQUE REFERENCES account(id),  -- du no goc khach dang no
    interest_account  bigint        NOT NULL UNIQUE REFERENCES account(id),  -- lai phai thu
    daily_rate        numeric(12,8) NOT NULL                                  -- 0.0005 = 0,05%/ngay
);

-- Moi lan tinh lai de lai DAU VET: tinh tren du no nao, chot toi seq nao. Kiem toan se hoi dung cau nay.
CREATE TABLE IF NOT EXISTS accrual (
    journal_id    bigint        PRIMARY KEY REFERENCES journal(id),
    loan_id       bigint        NOT NULL REFERENCES loan(id),
    business_date date          NOT NULL,
    kind          text          NOT NULL CHECK (kind IN ('ACCRUAL', 'ADJUSTMENT')),
    basis_balance numeric(19,4) NOT NULL,   -- du no goc dung de tinh
    basis_seq     bigint        NOT NULL,   -- trang thai account goc toi posting seq nay
    interest      numeric(19,4) NOT NULL
);
CREATE INDEX IF NOT EXISTS accrual_loan_day ON accrual (loan_id, business_date);
