"""Module 00 — on Python 3.12 bang chinh domain cua Katalon (ket qua test run).

Doi xung voi ``00-refresher-java21`` ben Java: CUNG mot domain, cung mot bo cau hoi,
de ban thay ro cho nao hai ngon ngu GIONG nhau va cho nao KHAC nhau. Cai "khac nhau"
moi la cho phong van dao.

Doc kem: ``../../katalon-prep-common/02-design-patterns.md`` (bang doi chieu Java <-> Python).
"""

from __future__ import annotations

import math
from collections.abc import Iterable
from dataclasses import dataclass, field
from datetime import timedelta
from enum import Enum
from typing import Protocol, runtime_checkable

# ---------------------------------------------------------------------------
# 1. Sum type: Java dung `sealed interface` + record. Python dung gi?
# ---------------------------------------------------------------------------
#
# Java 21:
#     sealed interface TestResult permits Passed, Failed, Skipped, Flaky {}
#     record Passed(String name, Duration duration) implements TestResult {}
#
# Python KHONG co sealed. Co 3 lua chon, va biet chon cai nao la cau tra loi hay:
#
#   (a) frozen dataclass + `match` -> gan Java nhat, giu duoc du lieu tung nhanh.  <-- dung o day
#   (b) Enum + payload roi rac      -> mat type safety, hay dung sai.
#   (c) `typing.Union` alias        -> chi la nhan, khong ep exhaustive.
#
# Diem KHAC quan trong: Java compiler BAT LOI khi `switch` thieu nhanh (exhaustiveness).
# Python KHONG. `match` thieu nhanh chi im lang roi xuong duoi. Vi vay o duoi ta phai
# TU them `case _:` nem loi — day chinh la cai gia phai tra khi khong co sealed type.


@dataclass(frozen=True, slots=True)
class Passed:
    name: str
    duration: timedelta


@dataclass(frozen=True, slots=True)
class Failed:
    name: str
    duration: timedelta
    message: str


@dataclass(frozen=True, slots=True)
class Skipped:
    name: str
    reason: str


@dataclass(frozen=True, slots=True)
class Flaky:
    """PASSED nhung phai chay lai. Che con so nay = test suite muc dan ma khong ai biet."""

    name: str
    duration: timedelta
    attempts: int

    def __post_init__(self) -> None:
        if self.attempts < 2:
            raise ValueError("flaky nghia la >= 2 lan chay")


TestResult = Passed | Failed | Skipped | Flaky


class Status(Enum):
    PASSED = "passed"
    FAILED = "failed"
    SKIPPED = "skipped"
    FLAKY = "flaky"


def classify(result: TestResult) -> Status:
    """Pattern matching — tuong duong `switch` pattern cua Java 21.

    ``case Flaky()`` phai dat TRUOC ``case Passed()`` neu chung co quan he ke thua.
    O day chung doc lap nen thu tu khong quan trong — nhung THOI QUEN dat nhanh hep
    truoc nhanh rong la thu can giu, vi Python khong canh bao khi mot nhanh bi che khuat.
    """
    match result:
        case Flaky():
            return Status.FLAKY
        case Passed():
            return Status.PASSED
        case Failed():
            return Status.FAILED
        case Skipped():
            return Status.SKIPPED
        case _:
            # Python KHONG co exhaustiveness check. Nhanh nay la thu duy nhat
            # bien "quen mot loai ket qua moi" tu bug im lang thanh loi on ao.
            raise TypeError(f"loai ket qua chua xu ly: {type(result).__name__}")


def describe(result: TestResult) -> str:
    """Guarded pattern — Java 21 viet ``case Failed f when f.message().isBlank()``."""
    match result:
        case Failed(name=n, message=msg) if not msg.strip():
            return f"{n}: that bai khong ro ly do"
        case Failed(name=n, message=msg):
            return f"{n}: {msg}"
        case Flaky(name=n, attempts=a):
            return f"{n}: pass sau {a} lan"
        case Passed(name=n, duration=d):
            return f"{n}: pass trong {d.total_seconds():.3f}s"
        case Skipped(name=n, reason=r):
            return f"{n}: bo qua ({r})"
        case _:
            raise TypeError(f"loai ket qua chua xu ly: {type(result).__name__}")


# ---------------------------------------------------------------------------
# 2. Bao cao — cho nay chua nhieu bay nhat
# ---------------------------------------------------------------------------


@dataclass(frozen=True, slots=True)
class Report:
    total: int
    by_status: dict[Status, int]
    slowest: str | None
    p95: timedelta
    flakiness_rate: float


def duration_of(result: TestResult) -> timedelta | None:
    """Skipped khong co duration. Tra None chu KHONG tra timedelta(0).

    ``timedelta(0)`` la noi doi: no keo trung binh xuong va lam p95 sai.
    "Khong co du lieu" khac "co du lieu bang 0" — nham cho nay lam hong moi dashboard.
    """
    match result:
        case Passed(duration=d) | Failed(duration=d) | Flaky(duration=d):
            return d
        case _:
            return None


def build_report(results: Iterable[TestResult]) -> Report:
    items = list(results)  # BAY: iterable co the la generator -> duyet lan 2 se RONG.
    if not items:
        return Report(0, {}, None, timedelta(0), 0.0)

    by_status: dict[Status, int] = {}
    for r in items:
        s = classify(r)
        by_status[s] = by_status.get(s, 0) + 1

    timed = [(r, d) for r in items if (d := duration_of(r)) is not None]
    slowest = max(timed, key=lambda t: t[1])[0].name if timed else None

    flaky = by_status.get(Status.FLAKY, 0)
    return Report(
        total=len(items),
        by_status=by_status,
        slowest=slowest,
        p95=percentile([d for _, d in timed], 95),
        # Chia 0 -> 0.0, KHONG de ZeroDivisionError va cung khong de ra nan.
        flakiness_rate=flaky / len(items) if items else 0.0,
    )


def percentile(durations: list[timedelta], pct: float) -> timedelta:
    """Percentile kieu "nearest-rank" (giong ``percentile_disc`` cua PostgreSQL).

    Co CHU Y dung nearest-rank chu khong noi suy: gia tri tra ve luon la mot lan do CO
    THAT. Khi bao cao SLA, "p95 = 1.234s" phai la mot request co that, khong phai so
    trung binh cua hai request. ``statistics.quantiles`` cua Python noi suy — khac ket qua.
    """
    if not durations:
        return timedelta(0)
    ordered = sorted(durations)
    rank = math.ceil(pct / 100 * len(ordered))
    return ordered[max(0, min(rank, len(ordered)) - 1)]


# ---------------------------------------------------------------------------
# 3. Protocol — cho Python KHAC Java ro nhat
# ---------------------------------------------------------------------------


@runtime_checkable
class DurationSource(Protocol):
    """Structural typing: KHONG can class nao "implements" cai nay.

    Java: class phai khai bao ``implements DurationSource`` -> nominal typing.
    Python: chi can CO ``def duration(self) -> timedelta`` la khop -> structural typing.

    He qua that cho thiet ke: ISP va DIP o Python gan nhu mien phi. Ban dinh nghia
    Protocol HEP ngay tai cho DUNG no (package cua consumer), khong phai sua class
    goc. Do la ly do o module 01 ta khong can mot interface "HealthCheck" khong lo.

    Cai mat di: khong con danh sach "ai implement cai nay" de IDE truy nguoc, va
    ``isinstance`` voi ``@runtime_checkable`` CHI kiem tra ten method co ton tai —
    khong kiem tra chu ky ham. Do la lo hong that, phai biet de tra loi khi bi hoi.
    """

    def duration(self) -> timedelta: ...


@dataclass(slots=True)
class MutableRun:
    """CO Y de mutable + co default kieu list — de day bay ``field(default_factory=...)``."""

    run_id: str
    results: list[TestResult] = field(default_factory=list)
    # Viet ``results: list = []`` o dataclass -> ValueError NGAY luc dinh nghia class.
    # Day la mot trong rat it cho Python chu dong chan bay "mutable default".
    # Ham thuong (``def f(x, acc=[])``) thi KHONG duoc bao ve — xem test tuong ung.

    def add(self, result: TestResult) -> None:
        self.results.append(result)
