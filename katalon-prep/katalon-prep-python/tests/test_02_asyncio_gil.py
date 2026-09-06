"""Module 02 — DO that, khong noi ly thuyet.

Cac test co `@pytest.mark.slow` do thoi gian that nen cham hon phan con lai.
Chay rieng:  uv run pytest tests/test_02_asyncio_gil.py -v -s
Con so in ra la con so tren MAY BAN — mang vao phong van duoc.
"""

from __future__ import annotations

import asyncio
import time

import pytest

from prep.asyncio_gil.pools import (
    blocked_loop_demo,
    blocking_io,
    cpu_work,
    measure,
    offload,
    run_bounded,
    run_processes,
    run_sequential,
    run_threads,
)

# Da HIEU CHINH bang do that tren may nay (8 core):
#   900k  -> seq 0.16s, process 0.27s  => process CHAM HON, demo phan tac dung
#   2M    -> seq 0.34s, process 0.24s  => 1.4x, dung huong nhung SAT nguong -> con flake
#   4M    -> seq 0.70s, process 0.28s  => ~2.5x, bien an toan du rong
# Chon 4M: 2M van do lung tung tren may dang ban (mot lan chay bi do that).
# Bai hoc: dataset qua nho lam benchmark noi NGUOC lai su that. Phai hieu chinh
# truoc khi tin vao con so - dung y het loi dataset bin-packing o module 08 ben Java.
SIZES = [4_000_000] * 4


@pytest.mark.slow
def test_CPU_bound_thread_KHONG_nhanh_len___process_thi_co(capsys):
    """Bang chung truc tiep cua GIL. Day la cau tra loi cho cau hoi phong van.

    Ky vong: thread ~= tuan tu (hoac cham hon vi ton chuyen ngu canh),
             process nhanh hon ro rang.
    """
    seq = measure("tuan tu", lambda: run_sequential(SIZES))
    thr = measure("thread ", lambda: run_threads(SIZES))
    prc = measure("process", lambda: run_processes(SIZES))

    with capsys.disabled():
        print()
        for t in (seq, thr, prc):
            print(f"  {t.label}: {t.seconds:.3f}s")
        print(f"  -> thread/tuan tu  = {thr.seconds / seq.seconds:.2f}x (ky vong ~1.0, GIL)")
        print(f"  -> tuan tu/process = {seq.seconds / prc.seconds:.2f}x (ky vong > 1.5)")

    # Thread KHONG duoc nhanh hon dang ke — day la ca diem cua bai.
    assert thr.seconds > seq.seconds * 0.75, "neu thread nhanh han thi GIL da bi nha o dau do"
    assert prc.seconds < seq.seconds * 0.8, "process phai nhanh hon ro ret voi viec CPU-bound"


@pytest.mark.slow
def test_IO_bound_thi_thread_LAI_giup___vi_time_sleep_NHA_GIL(capsys):
    """Cung la thread, nhung ket qua nguoc han test tren.

    Ket luan phai nho: **khong phai "thread vo dung trong Python"**, ma la
    "thread vo dung cho viec CPU-bound trong Python". Noi nham cau nay trong phong
    van la mat diem, vi no lo ra minh hoc thuoc chu khong hieu.
    """
    from concurrent.futures import ThreadPoolExecutor

    seq = measure("tuan tu", lambda: [blocking_io(0.05) for _ in range(4)])

    def with_threads():
        with ThreadPoolExecutor(max_workers=4) as pool:
            return list(pool.map(blocking_io, [0.05] * 4))

    thr = measure("thread ", with_threads)
    with capsys.disabled():
        print(f"\n  I/O: tuan tu {seq.seconds:.3f}s vs thread {thr.seconds:.3f}s "
              f"-> {seq.seconds / thr.seconds:.2f}x")
    assert thr.seconds < seq.seconds * 0.6


async def test_goi_ham_CHAN_trong_coroutine_lam_KET_ca_event_loop():
    """Bug asyncio pho bien nhat trong san xuat, va khong he bao loi.

    Do do tre cua mot task khac tren cung loop trong luc bi chan.
    """
    lag = await blocked_loop_demo(0.20)
    assert lag > 0.10, f"do tre do duoc {lag:.3f}s - le ra phai bi ket ~0.2s"


async def test_to_thread_go_duoc_ket_do():
    started = asyncio.get_running_loop().time()
    result = await asyncio.gather(
        offload(blocking_io, 0.1),
        offload(blocking_io, 0.1),
        offload(blocking_io, 0.1),
    )
    elapsed = asyncio.get_running_loop().time() - started
    assert result == ["done"] * 3
    assert elapsed < 0.25, f"3 x 0.1s song song phai < 0.25s, do duoc {elapsed:.3f}s"


async def test_bounded_concurrency_KHONG_bao_gio_vuot_gioi_han():
    """Khong co semaphore thi 10.000 test case mo 10.000 ket noi cung luc."""
    concurrent = 0
    peak = 0

    async def task() -> str:
        nonlocal concurrent, peak
        concurrent += 1
        peak = max(peak, concurrent)
        await asyncio.sleep(0.01)
        concurrent -= 1
        return "ok"

    out = await run_bounded([task] * 20, limit=4)
    assert out == ["ok"] * 20
    assert peak <= 4, f"dinh dong thoi {peak} - da vuot limit"


async def test_bounded_concurrency_mot_task_LOI_khong_lam_hong_ca_lo():
    async def ok() -> str:
        return "ok"

    async def bad() -> str:
        raise RuntimeError("test crash")

    out = await run_bounded([ok, bad, ok], limit=2)
    assert out[0] == "ok"
    assert isinstance(out[1], RuntimeError)
    assert out[2] == "ok"


def test_limit_khong_hop_le_bi_chan_ngay():
    with pytest.raises(ValueError, match="limit"):
        asyncio.run(run_bounded([], limit=0))


async def test_timeout_HUY_that_su_chu_khong_chi_bo_qua_ket_qua():
    """Khac biet quan trong so voi `CompletableFuture.orTimeout` cua Java.

    Java: `orTimeout` lam future hoan tat bang loi, nhung cong viec ben duoi VAN CHAY
          tiep — khong co co che huy. Ban phai tu quan ly.
    Python: `asyncio.timeout` HUY that coroutine ben trong.

    Nghia la doan code "1 phut se tu het han" o Java co the van dang giu ket noi DB
    sau 10 phut. Day la cau tra loi rat manh khi bi hoi ve timeout.
    """
    cancelled = False

    async def long_running() -> None:
        nonlocal cancelled
        try:
            await asyncio.sleep(5)
        except asyncio.CancelledError:
            cancelled = True
            raise

    with pytest.raises(TimeoutError):
        async with asyncio.timeout(0.05):
            await long_running()
    assert cancelled, "asyncio.timeout phai HUY coroutine, khong chi bo qua no"


@pytest.mark.slow
def test_process_pool_KHONG_phai_luon_nhanh_hon___viec_qua_nho_thi_lo(capsys):
    """Nuance ma cau tra loi "CPU-bound thi dung process" hay bo sot.

    Voi viec nho, chi phi tao process + pickle LON HON phan tiet kiem. Neu tra loi
    phong van ma khong noi duoc nguong nay thi la hoc thuoc.
    """
    tiny = [1_000] * 4
    seq = measure("tuan tu", lambda: [cpu_work(n) for n in tiny])
    prc = measure("process", lambda: run_processes(tiny))
    with capsys.disabled():
        print(f"\n  viec NHO: tuan tu {seq.seconds * 1000:.1f}ms vs "
              f"process {prc.seconds * 1000:.1f}ms -> process cham hon "
              f"{prc.seconds / seq.seconds:.0f}x")
    assert prc.seconds > seq.seconds, "voi viec nho, process pool phai CHAM hon"


def test_time_sleep_KHAC_asyncio_sleep_trong_coroutine():
    """Nham hai cai nay la loi nguoi moi hoc asyncio, va rat kho thay khi doc code."""

    async def wrong() -> float:
        loop = asyncio.get_running_loop()
        t0 = loop.time()
        time.sleep(0.05)  # CHAN loop
        return loop.time() - t0

    async def right() -> float:
        loop = asyncio.get_running_loop()
        t0 = loop.time()
        await asyncio.sleep(0.05)  # NHA loop
        return loop.time() - t0

    # Ca hai deu ton ~0.05s neu do rieng le - nen do RIENG LE khong phat hien duoc bug.
    assert asyncio.run(wrong()) == pytest.approx(0.05, abs=0.05)
    assert asyncio.run(right()) == pytest.approx(0.05, abs=0.05)
    # Chi khi CO TASK KHAC cung chay moi lo ra - xem
    # `test_goi_ham_CHAN_trong_coroutine_lam_KET_ca_event_loop`.
