"""Module 01 — ban "SAU". Day moi la mau.

Doi xung voi ``01-clean-code-solid/.../after/`` ben Java, nhung dung dung cong cu cua
Python. Cho nao Python lam GON HON Java, cho nao Python KHONG BAO VE duoc — deu ghi ro,
vi do la thu interviewer se hoi khi biet ban lam ca hai ngon ngu.
"""

from __future__ import annotations

import asyncio
import logging
from collections.abc import Mapping, Sequence
from dataclasses import dataclass
from enum import Enum
from types import MappingProxyType
from typing import Protocol

log = logging.getLogger(__name__)

# Hang so thay cho chuoi roi rac (Sonar S1192). Sai chinh ta o day thi compile/lint bat,
# con sai chinh ta trong chuoi roi rac thi khong ai bat.
_UNKNOWN_REASON = "het thoi gian cho"


class State(Enum):
    """UP / DOWN / UNKNOWN — ba trang thai, KHONG phai hai.

    Day la quyet dinh thiet ke quan trong nhat file nay. Gop UNKNOWN vao DOWN nghia la
    mot health check bi timeout se bao service da chet, va he thong dieu phoi se xoay
    vong lai mot service dang khoe. Su co lan rong ra tu chinh cong cu giam sat.
    """

    UP = "UP"
    DOWN = "DOWN"
    UNKNOWN = "UNKNOWN"


@dataclass(frozen=True, slots=True)
class Health:
    """Value object bat bien.

    Java can `record` + `Map.copyOf` trong compact constructor.
    Python: `frozen=True` chan gan lai thuoc tinh, nhung KHONG lam dict ben trong
    bat bien — nen van phai boc `MappingProxyType`. `frozen` chi nong mot lop.
    """

    state: State
    details: Mapping[str, str]

    def __post_init__(self) -> None:
        object.__setattr__(self, "details", MappingProxyType(dict(self.details)))

    @property
    def healthy(self) -> bool:
        return self.state is State.UP


class HealthCheck(Protocol):
    """Structural typing — KHONG class nao can viet ``implements HealthCheck``.

    Khac biet that so voi Java, va la cau tra loi cho "SOLID o Python khac gi?":
    DIP o Java can mot interface + module khai bao no; o Python chi can Protocol dat
    tai package CUA NGUOI DUNG. Nguoi cung cap khong can biet Protocol nay ton tai.
    """

    @property
    def service(self) -> str:
        """Chinh strategy TU KHAI BAO no phuc vu service nao.

        Day la cau sua cho bug trong RCI: khi strategy tu khai bao, registry chi viec
        hoi — khong con `if/elif` nao de quen mot nhanh. Them service moi = them mot
        class, KHONG sua code cu. Do la Open/Closed noi bang code chu khong noi bang loi.
        """

    async def probe(self) -> State: ...


class HealthCheckRegistry:
    """Tap hop cac check, chan trung ngay luc dang ky.

    Fail-fast luc khoi tao: hai check cung khai bao ``service == "db"`` la loi CAU HINH.
    Phat hien luc start >> phat hien luc mot trong hai bi ghi de am tham o production.
    """

    def __init__(self, checks: Sequence[HealthCheck]) -> None:
        registry: dict[str, HealthCheck] = {}
        for check in checks:
            if check.service in registry:
                raise ValueError(f"hai check cung dang ky service {check.service!r}")
            registry[check.service] = check
        self._checks = MappingProxyType(registry)

    def __len__(self) -> int:
        return len(self._checks)

    @property
    def services(self) -> frozenset[str]:
        return frozenset(self._checks)

    def get(self, service: str) -> HealthCheck | None:
        return self._checks.get(service)

    def all(self) -> Sequence[HealthCheck]:
        return tuple(self._checks.values())


class HealthAggregator:
    """Chay moi check song song, co timeout, KHONG bao gio nem ra ngoai.

    Dependency dua vao qua constructor (khong phai bien toan cuc) -> test tiem duoc
    ban gia lap ma khong can framework nao.
    """

    def __init__(self, registry: HealthCheckRegistry, timeout: float = 1.0) -> None:
        if timeout <= 0:
            raise ValueError("timeout phai > 0")
        self._registry = registry
        self._timeout = timeout

    async def aggregate(self, services: Sequence[str] | None = None) -> Health:
        targets = (
            self._registry.all()
            if services is None
            else [c for s in services if (c := self._registry.get(s)) is not None]
        )
        unknown_names = (
            []
            if services is None
            else [s for s in services if self._registry.get(s) is None]
        )

        results = await asyncio.gather(
            *(self._probe_one(c) for c in targets),
            # return_exceptions=True: mot check nem KHONG lam huy cac check con lai.
            # Bo co nay di thi `gather` huy anh em ngay lap tuc va ta mat thong tin
            # cua nhung service VAN KHOE. Do la thong tin quan trong nhat luc su co.
            return_exceptions=True,
        )

        details: dict[str, str] = {}
        for check, outcome in zip(targets, results, strict=True):
            if isinstance(outcome, BaseException):  # phong ho, khong bao gio toi day
                log.warning("check %s nem ngoai du kien", check.service, exc_info=outcome)
                details[check.service] = State.UNKNOWN.value
            else:
                details[check.service] = outcome.value
        for name in unknown_names:
            # Service khong dang ky la UNKNOWN, KHONG phai DOWN.
            details[name] = State.UNKNOWN.value

        return Health(self._roll_up(details), details)

    async def _probe_one(self, check: HealthCheck) -> State:
        try:
            async with asyncio.timeout(self._timeout):
                return await check.probe()
        except TimeoutError:
            # Timeout = KHONG BIET. Day la ranh gioi quyet dinh toan bo thiet ke.
            log.info("check %s: %s", check.service, _UNKNOWN_REASON)
            return State.UNKNOWN
        except Exception:
            # `except Exception` co Y — KHONG dung bare `except:`.
            # `asyncio.CancelledError` ke thua BaseException, nen bare except se nuot
            # no va lam huy tac vu khong con tac dung. Xem test chung minh.
            log.warning("check %s that bai", check.service, exc_info=True)
            return State.DOWN

    @staticmethod
    def _roll_up(details: Mapping[str, str]) -> State:
        """Bat ky DOWN nao -> DOWN. Con lai co UNKNOWN -> UNKNOWN. Het -> UP.

        Thu tu uu tien nay la mot quyet dinh san pham, khong phai chi tiet ky thuat:
        DOWN thang UNKNOWN vi mot su co da chac chan quan trong hon mot dieu chua ro.
        """
        values = set(details.values())
        if State.DOWN.value in values:
            return State.DOWN
        if State.UNKNOWN.value in values:
            return State.UNKNOWN
        return State.UP


# ---------------------------------------------------------------------------
# Cac check cu the — moi cai mot class nho, TU khai bao service cua no.
# ---------------------------------------------------------------------------


@dataclass(frozen=True, slots=True)
class FakeCheck:
    """Dung cho test va cho demo. Khong co `implements HealthCheck` — khong can."""

    service: str  # type: ignore[assignment]  # thoa Protocol bang thuoc tinh, khong can property
    result: State = State.UP
    delay: float = 0.0
    raises: BaseException | None = None

    async def probe(self) -> State:
        if self.delay:
            await asyncio.sleep(self.delay)
        if self.raises is not None:
            raise self.raises
        return self.result


def flakiness_rate(passed: int, total: int) -> float:
    """Chia 0 -> 0.0. Khong nem, khong tra nan.

    `nan` lam vo moi bieu do phia sau va rat kho truy nguoc, vi no lan truyen im lang
    qua moi phep tinh.
    """
    if total <= 0:
        return 0.0
    return (total - passed) / total
