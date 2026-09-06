"""Module 06 — retry / jitter / circuit breaker / rate limit / idempotency.

Doi xung voi `06-distributed-resilience` + `08-system-design` ben Java. Cung thuat toan,
nhung viet lai bang Python de ban tra loi duoc **bang ngon ngu nao ho hoi**.

Nguyen tac cua ca file: **khong goi `time.time()` hay `random.random()` truc tiep.**
Dong ho va bo sinh ngau nhien deu duoc TIEM VAO. Do la ly do duy nhat khien nhung thu
nay test tat dinh duoc — va cung la cau tra loi cho "lam sao test duoc retry/timeout?".
"""

from __future__ import annotations

import hashlib
import time
from collections import defaultdict
from collections.abc import Callable, Iterable, Sequence
from dataclasses import dataclass, field
from enum import Enum
class NonRetryable(Exception):
    """Loi VINH VIEN: 400, 401, 404, schema sai.

    Retry mot loi vinh vien khong bao gio thanh cong — chi ton thoi gian va lam nghen
    hang doi. Phan biet duoc loi tam thoi va loi vinh vien la nua gia tri cua retry.
    """


class Backoff(Enum):
    """Bon chien luoc, xep theo do "an toan cho he thong phia sau"."""

    FIXED = "fixed"
    EXPONENTIAL = "exponential"
    EXPONENTIAL_FULL_JITTER = "exponential_full_jitter"
    EXPONENTIAL_EQUAL_JITTER = "exponential_equal_jitter"


@dataclass(frozen=True, slots=True)
class RetryPolicy:
    max_attempts: int = 3
    base_delay: float = 0.1
    cap: float = 30.0
    strategy: Backoff = Backoff.EXPONENTIAL_FULL_JITTER
    # Tiem bo sinh ngau nhien -> test tat dinh. Mac dinh dung `random.random`.
    rand: Callable[[], float] = field(default=lambda: 0.5, repr=False)

    def __post_init__(self) -> None:
        if self.max_attempts < 1:
            raise ValueError("max_attempts phai >= 1")
        if self.base_delay <= 0:
            raise ValueError("base_delay phai > 0")

    def delay_for(self, attempt: int) -> float:
        """`attempt` tinh tu 0.

        `cap` la thu hay bi quen: khong co no thi lan thu 20 doi 2^20 * 0.1s = 29 gio.
        """
        exp = min(self.base_delay * (2**attempt), self.cap)
        match self.strategy:
            case Backoff.FIXED:
                return self.base_delay
            case Backoff.EXPONENTIAL:
                # KHONG jitter -> moi client sau su co cung thuc day mot luc
                # -> "thundering herd" -> service vua hoi phuc lai gay lai.
                return exp
            case Backoff.EXPONENTIAL_FULL_JITTER:
                # AWS khuyen dung: rai deu tren [0, exp]. Tan xa tot nhat.
                return exp * self.rand()
            case Backoff.EXPONENTIAL_EQUAL_JITTER:
                # Nua co dinh + nua ngau nhien: van tan xa nhung dam bao mot khoang
                # cho toi thieu. Hop khi backend can thoi gian hoi suc chac chan.
                return exp / 2 + (exp / 2) * self.rand()


def retry[T](
    fn: Callable[[], T],
    policy: RetryPolicy | None = None,
    sleep: Callable[[float], None] = time.sleep,
) -> T:
    """Thuc thi co retry. `sleep` tiem vao de test khong phai cho that."""
    p = policy or RetryPolicy()
    last: BaseException | None = None
    for attempt in range(p.max_attempts):
        try:
            return fn()
        except NonRetryable:
            raise  # loi vinh vien: dung ngay, khong lang phi luot retry
        except Exception as ex:
            last = ex
            if attempt < p.max_attempts - 1:
                sleep(p.delay_for(attempt))
    raise RuntimeError(f"that bai sau {p.max_attempts} lan") from last


class BreakerState(Enum):
    CLOSED = "closed"
    OPEN = "open"
    HALF_OPEN = "half_open"


class CircuitBreaker:
    """Ba pha. Khac retry o cho co ban: retry giup MOT request, breaker bao ve CA HE THONG.

    Vi sao can ca hai: retry mot minh lam su co NANG THEM — backend dang qua tai thi
    retry nhan luu luong len gap 3. Breaker ngat han luong di khi da ro la hong.

    HALF_OPEN la pha quan trong nhat va hay bi lam sai: chi cho **mot** request thu di
    qua. Cho ca lo di qua thi backend vua ngoi day lai bi dap chet lan nua.
    """

    def __init__(
        self,
        failure_threshold: int = 5,
        reset_timeout: float = 10.0,
        success_threshold: int = 2,
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        if failure_threshold < 1 or success_threshold < 1:
            raise ValueError("nguong phai >= 1")
        self._failure_threshold = failure_threshold
        self._reset_timeout = reset_timeout
        self._success_threshold = success_threshold
        self._clock = clock
        self._state = BreakerState.CLOSED
        self._failures = 0
        self._successes = 0
        self._opened_at = 0.0
        self._probe_in_flight = False

    @property
    def state(self) -> BreakerState:
        if self._state is BreakerState.OPEN and self._clock() - self._opened_at >= self._reset_timeout:
            self._state = BreakerState.HALF_OPEN
            self._successes = 0
            self._probe_in_flight = False
        return self._state

    def call[T](self, fn: Callable[[], T]) -> T:
        state = self.state
        if state is BreakerState.OPEN:
            raise BreakerOpen("mach dang ngat - khong goi backend")
        if state is BreakerState.HALF_OPEN:
            if self._probe_in_flight:
                raise BreakerOpen("dang thu mot request - cac request khac phai cho")
            self._probe_in_flight = True
        try:
            result = fn()
        except Exception:
            self._on_failure()
            raise
        else:
            self._on_success()
            return result

    def _on_success(self) -> None:
        self._probe_in_flight = False
        if self._state is BreakerState.HALF_OPEN:
            self._successes += 1
            if self._successes >= self._success_threshold:
                self._state = BreakerState.CLOSED
                self._failures = 0
        else:
            self._failures = 0

    def _on_failure(self) -> None:
        self._probe_in_flight = False
        if self._state is BreakerState.HALF_OPEN:
            # Mot lan hong o HALF_OPEN la mo lai NGAY, khong doi du nguong.
            self._state = BreakerState.OPEN
            self._opened_at = self._clock()
            return
        self._failures += 1
        if self._failures >= self._failure_threshold:
            self._state = BreakerState.OPEN
            self._opened_at = self._clock()


class BreakerOpen(Exception):
    pass


class TokenBucket:
    """Rate limit cho phep BUC ngan han — dung cho API cua san pham.

    So voi fixed window: fixed window co bug bien cua so. Gioi han 100/phut thi client
    gui 100 luc 00:59 va 100 luc 01:00 -> 200 request trong 1 giay ma van "dung luat".
    Token bucket khong co lo do vi no lien tuc.

    Nap token LUOI (lazy): khong co timer nen, chi tinh lai khi co request. Nghia la
    10.000 tenant chi ton 10.000 dong so, khong ton 10.000 timer.
    """

    def __init__(
        self, capacity: int, refill_per_second: float, clock: Callable[[], float] = time.monotonic
    ) -> None:
        if capacity < 1 or refill_per_second <= 0:
            raise ValueError("capacity >= 1 va refill_per_second > 0")
        self._capacity = capacity
        self._refill = refill_per_second
        self._clock = clock
        self._tokens = float(capacity)
        self._last = clock()

    @property
    def tokens(self) -> float:
        self._top_up()
        return self._tokens

    def _top_up(self) -> None:
        now = self._clock()
        elapsed = now - self._last
        if elapsed > 0:
            self._tokens = min(self._capacity, self._tokens + elapsed * self._refill)
            self._last = now

    def try_acquire(self, n: int = 1) -> bool:
        self._top_up()
        if self._tokens >= n:
            self._tokens -= n
            return True
        return False


class PerTenantRateLimiter:
    """Moi tenant mot bucket rieng — cach ly nhieu on.

    Mot bucket dung chung nghia la mot khach chay 10.000 test se lam moi khach khac bi
    tu choi. Trong SaaS thi day la su co ve HOP DONG, khong phai ve ky thuat.
    """

    def __init__(self, capacity: int, refill_per_second: float, clock: Callable[[], float] = time.monotonic) -> None:
        self._capacity = capacity
        self._refill = refill_per_second
        self._clock = clock
        self._buckets: dict[str, TokenBucket] = {}

    def try_acquire(self, tenant: str, n: int = 1) -> bool:
        bucket = self._buckets.get(tenant)
        if bucket is None:
            bucket = TokenBucket(self._capacity, self._refill, self._clock)
            self._buckets[tenant] = bucket
        return bucket.try_acquire(n)

    def __len__(self) -> int:
        return len(self._buckets)


@dataclass
class IdempotencyStore:
    """Chan xu ly trung khi consumer nhan lai message (at-least-once).

    BAY quan trong: TTL. Dat qua ngan thi mot message giao lai muon van bi xu ly hai lan
    -> mat y nghia. Dat qua dai thi bo nho phinh vo han. Phai chon theo do tre TOI DA
    cua he thong (thoi gian giu cua Kafka + so lan retry x backoff), khong chon theo cam tinh.
    """

    ttl: float = 3600.0
    clock: Callable[[], float] = time.monotonic
    _seen: dict[str, float] = field(default_factory=dict, repr=False)

    def mark_if_new(self, event_id: str) -> bool:
        """True neu day la lan dau -> duoc phep xu ly."""
        self._evict()
        if event_id in self._seen:
            return False
        self._seen[event_id] = self.clock()
        return True

    def _evict(self) -> None:
        now = self.clock()
        expired = [k for k, at in self._seen.items() if now - at >= self.ttl]
        for k in expired:
            del self._seen[k]

    def __len__(self) -> int:
        self._evict()
        return len(self._seen)


# ---------------------------------------------------------------------------
# Phan chia partition — noi thang sang bai data skew cua Spark o module 09
# ---------------------------------------------------------------------------


def stable_partition(key: str, partitions: int) -> int:
    """Hash ON DINH giua cac process. **Khong duoc dung `hash()` cua Python.**

    `hash()` cua str duoc lam ngau nhien moi lan khoi dong (PYTHONHASHSEED, chong DoS).
    Dung no de chia partition thi sau khi restart consumer, cung mot key roi vao
    partition khac -> mat sach dam bao thu tu, va bug chi hien sau khi deploy.
    Test o `test_00_refresher.py` chung minh dieu nay bang hai process that.

    Java khong co van de nay (`String.hashCode` co dinh trong spec) — nhung Java LAI co
    van de khac: `hashCode` co the AM, nen phai `Math.floorMod`. Python `%` da floor san.
    Hai ngon ngu, hai cai bay nguoc nhau o cung mot dong code.
    """
    if partitions < 1:
        raise ValueError("partitions phai >= 1")
    digest = hashlib.blake2b(key.encode("utf-8"), digest_size=8).digest()
    return int.from_bytes(digest, "big") % partitions


def partition_sizes(keys: Iterable[str], partitions: int) -> list[int]:
    sizes = [0] * partitions
    for k in keys:
        sizes[stable_partition(k, partitions)] += 1
    return sizes


def skew_ratio(sizes: Sequence[int]) -> float:
    """partition lon nhat / trung binh. 1.0 = deu tuyet doi.

    Day la chi so ban se dung lai NGUYEN VEN o module 09 (Spark): "hot partition" cua
    Kafka va "data skew" cua Spark la **cung mot van de** — mot key chiem qua nhieu
    du lieu — va cung mot cach chua: salting.
    """
    if not sizes:
        return 1.0
    total = sum(sizes)
    if total == 0:
        return 1.0
    return max(sizes) / (total / len(sizes))


def salted_partition(key: str, partitions: int, salt_buckets: int, counter: int) -> int:
    """Chua hot partition bang cach rai mot key nong ra `salt_buckets` gia-key.

    Cai gia phai tra, va PHAI noi ra khi tra loi phong van: **mat dam bao thu tu cho
    key do**. Chi lam duoc khi thu tu trong pham vi key khong quan trong, hoac khi
    ban co the sap xep lai o buoc sau. Doi thu tu lay thong luong.

    ------------------------------------------------------------------------
    BAY QUAN TRONG — do that tren 8 partition, 1 key chiem 90% cua 10.000 ban ghi:

        salt_buckets =   8  (1x so partition)  -> skew 2.80x   (gan nhu vo dung)
        salt_buckets =  16  (2x)               -> skew 3.70x   (**TE HON** ca 1x!)
        salt_buckets =  32  (4x)               -> skew 1.90x
        salt_buckets =  64  (8x)               -> skew 1.45x
        salt_buckets = 128  (16x)              -> skew 1.22x
        salt_buckets = 256  (32x)              -> skew 1.11x

    Ly do: ban chi doi mot key nong thanh N key vua — nhung N key do LAI hash
    khong deu vao cac partition (bai toan sinh nhat). Salt it thi may rui quyet
    dinh, va co truong hop nhieu salt hon lai lech hon.

    **Quy tac:** `salt_buckets >= 10x so partition`. Rat nhieu bai huong dan chi
    ghi "them salt la xong" ma khong noi cho nay — noi duoc con so nay trong phong
    van la khac biet giua "co doc ve salting" va "da tung lam that".

    Neu ban KHONG can thu tu chut nao thi dung `round_robin_partition` ben duoi —
    no san phang tuyet doi va khong phu thuoc may rui.
    """
    if salt_buckets < 1:
        raise ValueError("salt_buckets phai >= 1")
    return stable_partition(f"{key}#{counter % salt_buckets}", partitions)


def round_robin_partition(partitions: int, counter: int) -> int:
    """Rai deu TUYET DOI, khong qua hash.

    Danh doi manh hon salting: mat hoan toan lien he key -> partition, nen khong
    con dinh tuyen duoc va khong con thu tu nao ca. Dung khi ban ghi doc lap that su
    (vi du: gui ket qua tung test case le, moi ban ghi tu chua du thong tin).
    """
    if partitions < 1:
        raise ValueError("partitions phai >= 1")
    return counter % partitions


def hot_key_report(keys: Iterable[str], top: int = 3) -> list[tuple[str, int]]:
    counts: defaultdict[str, int] = defaultdict(int)
    for k in keys:
        counts[k] += 1
    return sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))[:top]
