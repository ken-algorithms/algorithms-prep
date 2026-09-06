"""Module 06 — test tat dinh cho thu von di khong tat dinh.

Ky thuat chinh: **tiem dong ho va bo sinh ngau nhien**. Khong test nao o day goi
`time.sleep` that. Do la cau tra loi cho "lam sao ban test duoc retry/timeout/breaker?"
— cau hoi gan nhu chac chan se co khi ban noi minh viet code phan tan.
"""

from __future__ import annotations

import pytest

from prep.distributed.resilience import (
    Backoff,
    BreakerOpen,
    BreakerState,
    CircuitBreaker,
    IdempotencyStore,
    NonRetryable,
    PerTenantRateLimiter,
    RetryPolicy,
    TokenBucket,
    hot_key_report,
    partition_sizes,
    retry,
    round_robin_partition,
    salted_partition,
    skew_ratio,
    stable_partition,
)


class FakeClock:
    """Dong ho gia — cho phep 'nhay 10 giay' ma khong phai cho 10 giay."""

    def __init__(self, now: float = 0.0) -> None:
        self.now = now

    def __call__(self) -> float:
        return self.now

    def advance(self, seconds: float) -> None:
        self.now += seconds


# ---------------------------------------------------------------------------
# Retry & backoff
# ---------------------------------------------------------------------------


def test_loi_VINH_VIEN_khong_duoc_retry():
    calls = 0

    def always_400():
        nonlocal calls
        calls += 1
        raise NonRetryable("400 bad request")

    with pytest.raises(NonRetryable):
        retry(always_400, RetryPolicy(max_attempts=5), sleep=lambda _: None)
    assert calls == 1, "retry loi vinh vien chi ton thoi gian, khong bao gio thanh cong"


def test_thanh_cong_o_lan_thu_3_thi_dung_lai_o_do():
    calls = 0

    def flaky():
        nonlocal calls
        calls += 1
        if calls < 3:
            raise ConnectionError("tam thoi")
        return "ok"

    assert retry(flaky, RetryPolicy(max_attempts=5), sleep=lambda _: None) == "ok"
    assert calls == 3


def test_het_luot_thi_nem_nhung_VAN_giu_nguyen_nhan_goc():
    def always_fails():
        raise ConnectionError("dut mang")

    with pytest.raises(RuntimeError, match="that bai sau 3 lan") as ex:
        retry(always_fails, RetryPolicy(max_attempts=3), sleep=lambda _: None)
    # `raise ... from last` -> khong mat dau vet nguyen nhan that.
    assert isinstance(ex.value.__cause__, ConnectionError)


def test_khong_jitter_thi_MOI_client_thuc_day_cung_mot_luc():
    """Chung minh thundering herd bang so, khong bang loi noi."""
    no_jitter = RetryPolicy(strategy=Backoff.EXPONENTIAL, base_delay=1.0)
    delays = {no_jitter.delay_for(2) for _ in range(100)}
    assert delays == {4.0}, "100 client -> chi mot moc thoi gian -> cung dap vao backend"

    # Full jitter: moi client mot moc khac nhau.
    seq = iter([i / 100 for i in range(100)])
    jittered = RetryPolicy(strategy=Backoff.EXPONENTIAL_FULL_JITTER, base_delay=1.0,
                           rand=lambda: next(seq))
    spread = {round(jittered.delay_for(2), 4) for _ in range(100)}
    assert len(spread) == 100
    assert min(spread) >= 0.0
    assert max(spread) <= 4.0


def test_equal_jitter_dam_bao_mot_khoang_cho_TOI_THIEU():
    p = RetryPolicy(strategy=Backoff.EXPONENTIAL_EQUAL_JITTER, base_delay=1.0, rand=lambda: 0.0)
    assert p.delay_for(2) == 2.0  # nua co dinh: khong bao gio ve 0
    p_max = RetryPolicy(strategy=Backoff.EXPONENTIAL_EQUAL_JITTER, base_delay=1.0, rand=lambda: 1.0)
    assert p_max.delay_for(2) == 4.0


def test_cap_chan_backoff_khoi_no_ra_hang_gio():
    p = RetryPolicy(strategy=Backoff.EXPONENTIAL, base_delay=1.0, cap=30.0)
    assert p.delay_for(20) == 30.0  # khong cap thi 2^20 giay = 12 ngay


# ---------------------------------------------------------------------------
# Circuit breaker
# ---------------------------------------------------------------------------


def test_breaker_mo_sau_du_so_lan_hong_va_KHONG_goi_backend_nua():
    clock = FakeClock()
    cb = CircuitBreaker(failure_threshold=3, reset_timeout=10.0, clock=clock)
    backend_calls = 0

    def failing():
        nonlocal backend_calls
        backend_calls += 1
        raise ConnectionError("chet")

    for _ in range(3):
        with pytest.raises(ConnectionError):
            cb.call(failing)
    assert cb.state is BreakerState.OPEN
    assert backend_calls == 3

    with pytest.raises(BreakerOpen):
        cb.call(failing)
    assert backend_calls == 3, "OPEN phai chan HOAN TOAN, backend khong duoc goi them"


def test_HALF_OPEN_chi_cho_MOT_request_thu_di_qua():
    """Cho ca lo di qua la loi lam sai breaker pho bien nhat.

    Backend vua ngoi day ma bi dap ca ngan request thi chet lai ngay lap tuc, va vong
    lap OPEN -> HALF_OPEN -> OPEN keo dai vo han.
    """
    clock = FakeClock()
    cb = CircuitBreaker(failure_threshold=1, reset_timeout=5.0, success_threshold=2, clock=clock)

    with pytest.raises(ConnectionError):
        cb.call(lambda: (_ for _ in ()).throw(ConnectionError()))
    assert cb.state is BreakerState.OPEN

    clock.advance(5.0)
    assert cb.state is BreakerState.HALF_OPEN

    calls = 0

    def slow_probe():
        nonlocal calls
        calls += 1
        raise ConnectionError("van chua khoe")

    with pytest.raises(ConnectionError):
        cb.call(slow_probe)
    assert calls == 1
    # Mot lan hong o HALF_OPEN -> mo lai NGAY, khong doi du nguong.
    assert cb.state is BreakerState.OPEN


def test_breaker_dong_lai_sau_du_so_lan_thanh_cong_lien_tiep():
    clock = FakeClock()
    cb = CircuitBreaker(failure_threshold=1, reset_timeout=5.0, success_threshold=2, clock=clock)
    with pytest.raises(ConnectionError):
        cb.call(lambda: (_ for _ in ()).throw(ConnectionError()))
    clock.advance(5.0)

    assert cb.call(lambda: "ok") == "ok"
    assert cb.state is BreakerState.HALF_OPEN, "mot lan thanh cong chua du"
    assert cb.call(lambda: "ok") == "ok"
    assert cb.state is BreakerState.CLOSED


# ---------------------------------------------------------------------------
# Rate limit
# ---------------------------------------------------------------------------


def test_token_bucket_cho_buc_nhung_khong_cho_vuot_tong():
    clock = FakeClock()
    b = TokenBucket(capacity=10, refill_per_second=1.0, clock=clock)
    assert all(b.try_acquire() for _ in range(10))  # buc 10 phat lien
    assert not b.try_acquire()  # het

    clock.advance(3.0)
    assert b.tokens == pytest.approx(3.0)
    assert b.try_acquire(3)
    assert not b.try_acquire()


def test_token_bucket_KHONG_tich_qua_capacity():
    clock = FakeClock()
    b = TokenBucket(capacity=5, refill_per_second=1.0, clock=clock)
    clock.advance(1000.0)
    assert b.tokens == 5.0, "de tich vo han thi mot client im lang 1 ngay se ban chet backend"


def test_fixed_window_co_bug_bien_cua_so___token_bucket_thi_khong():
    """Chung minh bang so vi sao khong dung fixed window."""

    class FixedWindow:
        def __init__(self, limit: int, window: float, clock):
            self.limit, self.window, self.clock = limit, window, clock
            self.count, self.start = 0, clock()

        def try_acquire(self) -> bool:
            if self.clock() - self.start >= self.window:
                self.count, self.start = 0, self.clock()
            if self.count < self.limit:
                self.count += 1
                return True
            return False

    clock = FakeClock()
    fw = FixedWindow(limit=100, window=60.0, clock=clock)
    clock.advance(59.0)
    burst_1 = sum(fw.try_acquire() for _ in range(100))
    clock.advance(1.1)  # sang cua so moi
    burst_2 = sum(fw.try_acquire() for _ in range(100))
    assert burst_1 + burst_2 == 200, "200 request trong ~1.1 giay ma van 'dung luat'"

    clock2 = FakeClock()
    tb = TokenBucket(capacity=100, refill_per_second=100 / 60, clock=clock2)
    clock2.advance(59.0)
    got_1 = sum(tb.try_acquire() for _ in range(100))
    clock2.advance(1.1)
    got_2 = sum(tb.try_acquire() for _ in range(100))
    assert got_1 + got_2 < 110, f"token bucket khong co lo bien cua so (cho qua {got_1 + got_2})"


def test_mot_tenant_on_ao_KHONG_lam_tenant_khac_bi_tu_choi():
    clock = FakeClock()
    limiter = PerTenantRateLimiter(capacity=5, refill_per_second=1.0, clock=clock)
    for _ in range(20):
        limiter.try_acquire("khach-on-ao")
    assert limiter.try_acquire("khach-hien-lanh"), "cach ly nhieu on bi vo"
    assert len(limiter) == 2


# ---------------------------------------------------------------------------
# Idempotency
# ---------------------------------------------------------------------------


def test_message_giao_lai_chi_duoc_xu_ly_MOT_lan():
    store = IdempotencyStore(ttl=3600, clock=FakeClock())
    assert store.mark_if_new("evt-1")
    assert not store.mark_if_new("evt-1")


def test_BAY_TTL_ngan_hon_do_tre_giao_lai_lam_MAT_tinh_idempotent():
    """Bay quan trong nhat cua idempotency store, va rat kho phat hien o production.

    TTL 60s nhung Kafka giu message va giao lai sau 90s -> ban ghi da bi xoa ->
    xu ly LAI. Test van xanh, dashboard van xanh, chi so lieu la sai.
    """
    clock = FakeClock()
    store = IdempotencyStore(ttl=60.0, clock=clock)
    assert store.mark_if_new("evt-1")

    clock.advance(90.0)  # giao lai muon hon TTL
    assert store.mark_if_new("evt-1"), "da het han -> bi coi la moi -> xu ly hai lan"

    # TTL phai >= do tre TOI DA cua he thong (retention x so lan retry x backoff).
    clock2 = FakeClock()
    safe = IdempotencyStore(ttl=86_400.0, clock=clock2)
    assert safe.mark_if_new("evt-2")
    clock2.advance(90.0)
    assert not safe.mark_if_new("evt-2")


# ---------------------------------------------------------------------------
# Partition & skew — noi thang sang module 09 (Spark)
# ---------------------------------------------------------------------------


def test_stable_partition_giong_nhau_qua_moi_lan_goi_va_moi_process():
    """Gia tri GHIM CUNG, khong so sanh ham voi chinh no.

    SonarLint python:S5863 bat dung khi toi viet `f(x) == f(x)`: assert kieu do luon
    dung nen khong chung minh duoc gi. Ghim gia tri that moi co y nghia — doi thuat
    toan hash (blake2b -> md5 chang han) se lam test nay do NGAY, va do dung la
    dieu ta muon: doi hash = moi key nhay partition = vo dam bao thu tu cua he thong
    dang chay.
    """
    assert stable_partition("tenant-a", 4) == 3
    assert stable_partition("tenant-a", 8) == 7
    assert stable_partition("tenant-a", 16) == 15
    with pytest.raises(ValueError):
        stable_partition("x", 0)


def test_key_phan_bo_deu_thi_skew_gan_1():
    keys = [f"tenant-{i}" for i in range(2000)]
    ratio = skew_ratio(partition_sizes(keys, 8))
    assert ratio < 1.15, f"skew {ratio:.2f} - hash le ra phai rai kha deu"


def test_MOT_key_nong_lam_vo_can_bang___salting_chua_duoc(capsys):
    """Day la 'hot partition' cua Kafka, va cung la 'data skew' cua Spark.

    Cung mot van de, cung mot cach chua. Module 09 lam lai dung bai nay tren
    du lieu that bang PySpark.
    """
    PARTITIONS = 8
    # 1 khach lon chiem 90% luu luong — dung nhu thuc te cua mot SaaS.
    keys = ["khach-lon"] * 9000 + [f"khach-nho-{i}" for i in range(1000)]
    before = partition_sizes(keys, PARTITIONS)
    ratio_before = skew_ratio(before)
    assert ratio_before > 5.0, "1 key chiem 90% phai gay lech nang"
    assert hot_key_report(keys, top=1) == [("khach-lon", 9000)]

    def with_salt(salt_buckets: int) -> list[int]:
        sizes = [0] * PARTITIONS
        counter = 0
        for k in keys:
            if k == "khach-lon":
                sizes[salted_partition(k, PARTITIONS, salt_buckets, counter)] += 1
                counter += 1
            else:
                sizes[stable_partition(k, PARTITIONS)] += 1
        return sizes

    with capsys.disabled():
        print(f"\n  KHONG salt          -> skew {ratio_before:5.2f}x  {before}")
        for mult in (1, 2, 4, 8, 16, 32):
            s = with_salt(PARTITIONS * mult)
            print(f"  salt = {mult:>2}x partition -> skew {skew_ratio(s):5.2f}x  {s}")

    # >>> KET QUA BAT NGO, va la diem chinh cua test nay <<<
    # Salt bang dung so partition gan nhu VO DUNG, va 2x con TE HON 1x.
    # Ly do: N gia-key moi tao ra LAI hash khong deu vao partition (bai toan sinh nhat).
    assert skew_ratio(with_salt(PARTITIONS * 1)) > 2.0
    assert skew_ratio(with_salt(PARTITIONS * 2)) > skew_ratio(with_salt(PARTITIONS * 1))

    # Phai salt gap NHIEU LAN so partition moi thuc su san phang.
    assert skew_ratio(with_salt(PARTITIONS * 16)) < 1.3


def test_round_robin_san_phang_TUYET_DOI_nhung_mat_han_thu_tu():
    """Doi chung voi salting: khong qua hash thi khong con may rui nao.

    Danh doi nang hon: mat hoan toan lien he key -> partition. Chi dung khi tung ban
    ghi doc lap that su.
    """
    PARTITIONS = 8
    sizes = [0] * PARTITIONS
    for counter in range(10_000):
        sizes[round_robin_partition(PARTITIONS, counter)] += 1
    assert skew_ratio(sizes) == 1.0
    assert len(set(sizes)) == 1  # deu tuyet doi

    # Nhung: cung mot key khong con ve cung mot cho -> mat thu tu.
    assert round_robin_partition(PARTITIONS, 0) == 0
    assert round_robin_partition(PARTITIONS, 1) == 1  # cung ban ghi, partition khac
    assert stable_partition("k", PARTITIONS) == 3  # hash thi luon ve dung mot cho
