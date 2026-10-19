-- Bai 05 + 06: so cai kep (double-entry ledger) cho vi va khoan vay. PostgreSQL 16 thuan.
-- Quy uoc dau: amount co dau, so du cua mot account = SUM(amount) cac posting cua no.
-- Moi journal (but toan) co >= 2 posting va tong amount = 0: tien khong tu sinh ra, khong tu mat.

-- ══ ACCOUNT ═══════════════════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS account (
    id             bigint        PRIMARY KEY,
    code           text          NOT NULL UNIQUE,       -- 'wallet:alice', 'cash', 'loan:42:principal'
    allow_negative boolean       NOT NULL DEFAULT false, -- vi khach: false. Tai khoan he thong: true
    hot            boolean       NOT NULL DEFAULT false, -- tai khoan nong: KHONG khoa, KHONG giu so du chay
    balance        numeric(19,4) NOT NULL DEFAULT 0,     -- = SUM(posting), cap nhat TRONG CUNG transaction
    last_seq       bigint        NOT NULL DEFAULT 0,     -- seq cua posting cuoi; tang duoi khoa dong nay
    CHECK (NOT hot OR allow_negative)                    -- khong khoa thi khong chan am duoc
);

-- ══ JOURNAL: mot but toan = mot nghiep vu ════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS journal (
    id              bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idempotency_key text        NOT NULL UNIQUE,  -- 'transfer:<id client>', 'accrual:42:2026-10-18'
    request_hash    text        NOT NULL,         -- cung key ma khac noi dung -> tu choi
    kind            text        NOT NULL,         -- DEPOSIT, TRANSFER, PAYMENT, ACCRUAL, ADJUSTMENT, REVERSAL
    effective_date  date        NOT NULL,         -- THOI GIAN NGHIEP VU: but toan thuoc ve ngay nao
    recorded_at     timestamptz NOT NULL DEFAULT clock_timestamp(),  -- THOI GIAN HE THONG: luc ghi
    reverses        bigint      REFERENCES journal(id)
);

-- ══ POSTING: append-only ═════════════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS posting (
    id             bigint        GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    journal_id     bigint        NOT NULL REFERENCES journal(id),
    account_id     bigint        NOT NULL REFERENCES account(id),
    seq            bigint,                         -- 1,2,3.. theo account, khong ho; NULL voi account nong
    amount         numeric(19,4) NOT NULL CHECK (amount <> 0),
    balance_after  numeric(19,4),                  -- so du chay ngay sau posting nay; NULL voi account nong
    effective_date date          NOT NULL,         -- chep tu journal de index (account, ngay)
    tx_id          xid8          NOT NULL DEFAULT pg_current_xact_id(),  -- transaction da ghi dong nay
    UNIQUE (account_id, seq),
    CHECK ((seq IS NULL) = (balance_after IS NULL))
);
CREATE INDEX IF NOT EXISTS posting_journal     ON posting (journal_id);
CREATE INDEX IF NOT EXISTS posting_account_eff ON posting (account_id, effective_date);
CREATE INDEX IF NOT EXISTS posting_hot_tx      ON posting (account_id, tx_id) WHERE seq IS NULL;

-- Bat bien 1: journal can bang. Kiem tra LUC COMMIT (deferred) vi posting duoc chen tung dong.
CREATE OR REPLACE FUNCTION journal_must_balance() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    total numeric;
BEGIN
    SELECT coalesce(sum(amount), 0) INTO total FROM posting WHERE journal_id = NEW.journal_id;
    IF total <> 0 THEN
        RAISE EXCEPTION 'journal % khong can bang: tong = %', NEW.journal_id, total
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END $$;
DROP TRIGGER IF EXISTS posting_balanced ON posting;
CREATE CONSTRAINT TRIGGER posting_balanced AFTER INSERT ON posting
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION journal_must_balance();

-- Bat bien 2: append-only. Sai thi ghi but toan dao (REVERSAL), khong UPDATE, khong DELETE.
CREATE OR REPLACE FUNCTION ledger_is_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'bang % chi cho INSERT: % bi chan', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'restrict_violation';
END $$;
DROP TRIGGER IF EXISTS posting_append_only ON posting;
CREATE TRIGGER posting_append_only BEFORE UPDATE OR DELETE ON posting
    FOR EACH ROW EXECUTE FUNCTION ledger_is_append_only();
DROP TRIGGER IF EXISTS journal_append_only ON journal;
CREATE TRIGGER journal_append_only BEFORE UPDATE OR DELETE ON journal
    FOR EACH ROW EXECUTE FUNCTION ledger_is_append_only();

-- ══ DOC NHANH ════════════════════════════════════════════════════════════════════════════════
-- Phuong an B (khong giu so du chay): snapshot + phan duoi, chot theo seq cua account.
CREATE TABLE IF NOT EXISTS balance_snapshot (
    account_id   bigint        PRIMARY KEY REFERENCES account(id),
    balance      numeric(19,4) NOT NULL,
    up_to_seq    bigint        NOT NULL,
    refreshed_at timestamptz   NOT NULL DEFAULT clock_timestamp()
);

-- Account nong: so du tong hop bat dong bo, chot theo moc transaction (xid), khong theo posting.id.
CREATE TABLE IF NOT EXISTS hot_balance (
    account_id   bigint        PRIMARY KEY REFERENCES account(id),
    balance      numeric(19,4) NOT NULL,
    up_to_xid    xid8          NOT NULL,  -- da cong moi posting cua transaction co xid < moc nay
    refreshed_at timestamptz   NOT NULL DEFAULT clock_timestamp()
);
-- Ban SAI de doi chieu: chot theo posting.id. Thu tu id != thu tu commit.
CREATE TABLE IF NOT EXISTS hot_balance_by_id (
    account_id      bigint        PRIMARY KEY REFERENCES account(id),
    balance         numeric(19,4) NOT NULL,
    up_to_posting   bigint        NOT NULL
);
