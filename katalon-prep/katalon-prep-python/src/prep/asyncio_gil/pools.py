"""Module 02 — GIL, thread, process, asyncio. Doi xung voi `04-concurrency` ben Java.

Day la cho hai ngon ngu KHAC NHAU NHAT, va la cau hoi phong van gan nhu chac chan se
co khi CV ghi ca Java lan Python:

    "Ban chon thread hay async? Vi sao?"

Cau tra loi dung KHONG giong nhau o hai ngon ngu:

  Java 21  -> virtual thread. Code viet kieu dong bo (blocking), JVM tu tha thread
              khi gap I/O. Khong co GIL, nen thread THAT chay CPU song song that.
  Python   -> phai tu chon:
                * I/O-bound  -> asyncio (hoac thread, nhung asyncio re hon nhieu)
                * CPU-bound  -> **process**, vi GIL chan hai thread cung chay bytecode
                * blocking lib khong co ban async -> thread pool trong asyncio

Sai lam pho bien nhat: dung ThreadPoolExecutor cho viec CPU-bound trong Python roi
thac mac vi sao khong nhanh len. Test o day DO that de chung minh.
"""

from __future__ import annotations

import asyncio
import time
from collections.abc import Awaitable, Callable, Iterable, Sequence
from concurrent.futures import ProcessPoolExecutor, ThreadPoolExecutor
from dataclasses import dataclass


def cpu_work(n: int) -> int:
    """Viec THUAN CPU: khong I/O, khong nha GIL.

    Chon vong lap so hoc thuan chu khong dung numpy — numpy NHA GIL trong cac phep
    tinh lon, nen dung numpy se lam demo GIL sai hoan toan. Chi tiet nay dang noi:
    "Python khong song song duoc" la cau noi SAI; dung hon la "bytecode Python khong
    song song duoc, con extension C thi co the".
    """
    total = 0
    for i in range(n):
        total += i * i % 7
    return total


def blocking_io(seconds: float) -> str:
    """Mo phong I/O chan luong (goi HTTP dong bo, driver DB dong bo, `requests`...).

    `time.sleep` NHA GIL — nen thread pool giup duoc o day, khac han `cpu_work`.
    """
    time.sleep(seconds)
    return "done"


@dataclass(frozen=True, slots=True)
class Timing:
    label: str
    seconds: float

    def faster_than(self, other: Timing, factor: float = 1.0) -> bool:
        return self.seconds * factor < other.seconds


def measure(label: str, fn: Callable[[], object]) -> Timing:
    started = time.perf_counter()
    fn()
    return Timing(label, time.perf_counter() - started)


def run_sequential(sizes: Sequence[int]) -> list[int]:
    return [cpu_work(n) for n in sizes]


def run_threads(sizes: Sequence[int]) -> list[int]:
    """CPU-bound + thread = KHONG nhanh len. GIL chi cho mot thread chay bytecode."""
    with ThreadPoolExecutor(max_workers=len(sizes)) as pool:
        return list(pool.map(cpu_work, sizes))


def run_processes(sizes: Sequence[int]) -> list[int]:
    """CPU-bound + process = nhanh len that, moi process co GIL rieng.

    Cai gia: khoi tao process ton ~50-200ms, va moi tham so/ket qua phai `pickle`
    qua lai. Voi viec nho thi chi phi nay LON HON phan tiet kiem duoc.
    """
    with ProcessPoolExecutor(max_workers=min(4, len(sizes))) as pool:
        return list(pool.map(cpu_work, sizes))


# ---------------------------------------------------------------------------
# asyncio — bounded concurrency, cai thuc su dung trong mot test runner
# ---------------------------------------------------------------------------


async def run_bounded(
    tasks: Iterable[Callable[[], Awaitable[str]]], limit: int
) -> list[str | BaseException]:
    """Chay song song nhung CHAN o `limit` — day la thu luon can trong thuc te.

    Khong gioi han thi 10.000 test case tao 10.000 ket noi cung luc -> connection pool
    can, doi phuong bi ban tu choi, va chinh may minh het file descriptor. `gather`
    tran lan la cach de nhat de tu DoS chinh minh.

    Java 21 doi ung: virtual thread + `Semaphore`, hoac `ExecutorService` co gioi han.
    """
    if limit < 1:
        raise ValueError("limit phai >= 1")
    sem = asyncio.Semaphore(limit)

    async def guarded(fn: Callable[[], Awaitable[str]]) -> str:
        async with sem:
            return await fn()

    return await asyncio.gather(*(guarded(t) for t in tasks), return_exceptions=True)


async def offload(fn: Callable[..., object], *args: object) -> object:
    """Day ham CHAN sang thread de khong ket event loop.

    Bug asyncio pho bien nhat: goi mot thu viện dong bo (`requests`, driver DB cu)
    ngay trong `async def`. Khong co loi nao het — chi la TOAN BO event loop dung
    lai trong suot thoi gian do. Mot request cham lam ca service cham.

    `asyncio.to_thread` la cach vá dung. Cai gia: van la thread that, nen CPU-bound
    thi van vo dung vi GIL.
    """
    return await asyncio.to_thread(fn, *args)


async def blocked_loop_demo(block_seconds: float) -> float:
    """Do do TRE ma mot lenh chan gay ra cho cac task khac tren cung event loop."""
    loop = asyncio.get_running_loop()
    lag = 0.0

    async def heartbeat() -> None:
        nonlocal lag
        for _ in range(20):
            before = loop.time()
            await asyncio.sleep(0.005)
            lag = max(lag, loop.time() - before - 0.005)

    hb = asyncio.create_task(heartbeat())
    await asyncio.sleep(0.01)
    time.sleep(block_seconds)  # <-- CO Y goi ham chan trong coroutine
    await hb
    return lag
