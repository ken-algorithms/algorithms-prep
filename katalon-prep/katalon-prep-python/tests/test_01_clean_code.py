"""Module 01 — chung minh tung van de cua `before/` bang test chay that.

Nguyen tac giong ben Java: KHONG doc `after/` truoc. Chay test o day, xem cai gi vo,
roi moi doi chieu.

Hai test dang gia nhat file nay la:
  * `test_bare_except_nuot_CancelledError_lam_HUY_MAT_TAC_DUNG`
  * `test_gather_KHONG_huy_anh_em_ma_bo_ROI_chung___TaskGroup_thi_huy`
Ca hai la hanh vi **chi co o Python**, khong co doi ung ben Java — noi duoc chung
trong phong van la bang chung ban thuc su viet ca hai ngon ngu, khong phai doc luot.
Cai thu hai chinh la cho toi doan sai va phai chay moi biet; docstring cua no ghi ro.
"""

from __future__ import annotations

import asyncio
import logging
from dataclasses import FrozenInstanceError

import pytest

from prep.clean_code.after.health import (
    FakeCheck,
    Health,
    HealthAggregator,
    HealthCheckRegistry,
    State,
    flakiness_rate,
)
from prep.clean_code.before import aggregator as before

# ---------------------------------------------------------------------------
# Phan A — `before/` sai o dau
# ---------------------------------------------------------------------------


async def test_before_bao_DOWN_cho_service_no_KHONG_HE_KIEM_TRA():
    """Van de nghiem trong nhat, va la bug that trong RCI cua ban.

    5 service duoc khai bao, `if/elif` xu ly 2, 3 cai con lai bi gan thang "DOWN".
    Khong phai "chua biet" — la khang dinh SAI rang chung da chet.
    """
    result = await before.do_health_check(
        ["db", "redis", "kafka", "smtp"], verbose=False, include_details=False, fail_fast=False
    )
    assert result["kafka"] == "DOWN"
    assert result["smtp"] == "DOWN"  # smtp thuc te UP, nhung bi bao chet
    before._CACHE.clear()


async def test_before_TRA_VE_CHINH_cache_noi_bo_nen_caller_sua_duoc():
    r1 = await before.do_health_check(
        ["db"], verbose=False, include_details=False, fail_fast=False
    )
    r1["db"] = "BI SUA TU BEN NGOAI"
    r2 = await before.do_health_check(
        ["db"], verbose=False, include_details=False, fail_fast=False
    )
    assert r2 is r1  # cung mot object -> trang thai toan cuc bi o nhiem
    before._CACHE.clear()


def test_before_chia_cho_0_lam_NO_luc_chay():
    with pytest.raises(ZeroDivisionError):
        before.flakiness(0, 0)
    assert flakiness_rate(0, 0) == 0.0  # ban `after` khong nem


async def test_gather_KHONG_huy_anh_em_ma_bo_ROI_chung___TaskGroup_thi_huy():
    """Toi da DOAN SAI cho nay khi viet test lan dau, chay moi lo ra.

    Toi tuong `gather` huy anh em khi mot task nem. **Khong phai.** Su that:

      * `gather(...)`            -> nem loi DAU TIEN ra ngoai, cac task khac VAN CHAY
                                    tiep nhung khong ai lay ket qua nua -> **task mo coi**.
                                    Chung van an CPU/ket noi, va neu sau do chung cung
                                    nem thi Python in "Task exception was never retrieved".
      * `TaskGroup` (3.11+)      -> HUY tat ca anh em, gom loi lai thanh `ExceptionGroup`.

    Nen chon cai nao la mot cau hoi thiet ke that:
      - Health check  -> `gather(return_exceptions=True)`: ta MUON biet ket qua cua
        moi service, ke ca khi mot cai loi.
      - Mot giao dich nhieu buoc -> `TaskGroup`: mot buoc hong thi lam tiep la vo nghia,
        huy het la dung.

    Ban Java `allOf` gan `gather` hon: khong tu huy anh em.
    """
    finished: list[str] = []

    async def slow_but_healthy():
        await asyncio.sleep(0.05)
        finished.append("slow")
        return "UP"

    async def fails_fast():
        await asyncio.sleep(0.001)
        raise RuntimeError("kafka chet")

    with pytest.raises(RuntimeError):
        await asyncio.gather(slow_but_healthy(), fails_fast())
    await asyncio.sleep(0.1)
    assert finished == ["slow"], "gather bo roi chu KHONG huy - task van chay het"

    # TaskGroup: nguoc lai hoan toan.
    finished.clear()
    with pytest.raises(ExceptionGroup):
        async with asyncio.TaskGroup() as tg:
            tg.create_task(slow_but_healthy())
            tg.create_task(fails_fast())
    await asyncio.sleep(0.1)
    assert finished == [], "TaskGroup huy anh em ngay khi mot task nem"

    # Cach dung dung cho health check: giu lai duoc CA HAI ket qua.
    finished.clear()
    out = await asyncio.gather(slow_but_healthy(), fails_fast(), return_exceptions=True)
    assert finished == ["slow"]
    assert out[0] == "UP"
    assert isinstance(out[1], RuntimeError)


async def test_bare_except_nuot_CancelledError_lam_HUY_MAT_TAC_DUNG():
    """`asyncio.CancelledError` ke thua **BaseException**, khong phai Exception.

    Nen:
      * `except Exception:`  -> KHONG bat -> huy hoat dong dung.
      * `except:` (bare)     -> BAT -> tac vu khong bao gio dung -> timeout vo nghia,
                                shutdown treo, worker khong thoat.

    Day la ly do `after/health.py` viet ro `except Exception` chu khong viet `except:`.
    """
    stop = asyncio.Event()
    swallowed = 0

    async def stubborn():  # bat chuoc `except:` cua before/aggregator.py
        nonlocal swallowed
        while not stop.is_set():
            try:
                await asyncio.sleep(0.01)
            except BaseException:
                swallowed += 1

    task = asyncio.create_task(stubborn())
    await asyncio.sleep(0.02)
    task.cancel()
    _, pending = await asyncio.wait([task], timeout=0.15)
    assert task in pending, "task le ra van chay - vi CancelledError bi nuot"
    assert swallowed >= 1

    stop.set()  # chi con cach nay moi dung duoc no
    await asyncio.wait([task], timeout=0.5)
    assert task.done()

    # Doi chung: `except Exception` -> huy duoc ngay.
    async def well_behaved():
        while True:
            try:
                await asyncio.sleep(0.01)
            except Exception:
                pass

    good = asyncio.create_task(well_behaved())
    await asyncio.sleep(0.02)
    good.cancel()
    with pytest.raises(asyncio.CancelledError):
        await good


# ---------------------------------------------------------------------------
# Phan B — `after/` dung o dau
# ---------------------------------------------------------------------------


async def test_service_chua_dang_ky_la_UNKNOWN_KHONG_phai_DOWN():
    """Ranh gioi thiet ke quan trong nhat cua module nay."""
    agg = HealthAggregator(HealthCheckRegistry([FakeCheck("db")]))
    health = await agg.aggregate(["db", "kafka"])
    assert health.details["db"] == "UP"
    assert health.details["kafka"] == "UNKNOWN"
    assert health.state is State.UNKNOWN  # khong phai DOWN


async def test_timeout_ra_UNKNOWN_va_KHONG_lam_cham_ca_endpoint():
    agg = HealthAggregator(
        HealthCheckRegistry([FakeCheck("db"), FakeCheck("s3", delay=5.0)]), timeout=0.05
    )
    loop = asyncio.get_running_loop()
    started = loop.time()
    health = await agg.aggregate()
    elapsed = loop.time() - started

    assert health.details["s3"] == "UNKNOWN"
    assert health.details["db"] == "UP"
    assert elapsed < 1.0, f"phai tra ve theo timeout, khong doi 5s (do duoc {elapsed:.2f}s)"


async def test_mot_check_NEM_khong_lam_mat_ket_qua_cua_cac_check_khac(caplog):
    agg = HealthAggregator(
        HealthCheckRegistry(
            [FakeCheck("db"), FakeCheck("kafka", raises=RuntimeError("khong ket noi duoc"))]
        )
    )
    with caplog.at_level(logging.WARNING):
        health = await agg.aggregate()

    assert health.details == {"db": "UP", "kafka": "DOWN"}
    assert health.state is State.DOWN
    # Loi phai duoc LOG, khong duoc nuot im lang nhu `before/`.
    assert any("kafka" in r.message for r in caplog.records)


async def test_DOWN_thang_UNKNOWN_khi_gop_trang_thai():
    agg = HealthAggregator(
        HealthCheckRegistry(
            [FakeCheck("db", State.DOWN), FakeCheck("s3", delay=5.0), FakeCheck("redis")]
        ),
        timeout=0.05,
    )
    health = await agg.aggregate()
    assert health.details["s3"] == "UNKNOWN"
    assert health.state is State.DOWN  # mot su co CHAC CHAN thang mot dieu chua ro


def test_registry_CHAN_dang_ky_trung_ngay_luc_khoi_tao():
    with pytest.raises(ValueError, match="cung dang ky service 'db'"):
        HealthCheckRegistry([FakeCheck("db"), FakeCheck("db", State.DOWN)])


def test_them_service_moi_KHONG_phai_sua_dong_code_nao_cu():
    """Open/Closed noi bang code chu khong noi bang loi."""

    class LdapCheck:  # khong ke thua gi, khong dang ky o dau
        service = "ldap"

        async def probe(self) -> State:
            return State.UP

    registry = HealthCheckRegistry([FakeCheck("db"), LdapCheck()])
    assert registry.services == {"db", "ldap"}
    assert len(registry) == 2


def test_Health_bat_bien_ca_o_lop_trong():
    h = Health(State.UP, {"db": "UP"})
    # SonarLint python:S5958 goi y dung: `pytest.raises(Exception)` qua rong, bat ca
    # loi khong lien quan. Phai chi dich danh loai loai muon.
    with pytest.raises(FrozenInstanceError):
        h.state = State.DOWN  # type: ignore[misc]
    with pytest.raises(TypeError):
        h.details["db"] = "DOWN"  # type: ignore[index]

    # `frozen=True` chi nong MOT LOP: khong boc MappingProxyType thi dict van sua duoc.
    source = {"db": "UP"}
    h2 = Health(State.UP, source)
    source["db"] = "DOWN"
    assert h2.details["db"] == "UP"  # da copy nen khong bi anh huong


def test_timeout_am_bi_chan_ngay_luc_tao_khong_doi_toi_luc_chay():
    with pytest.raises(ValueError, match="timeout"):
        HealthAggregator(HealthCheckRegistry([]), timeout=0)


async def test_khong_co_check_nao_thi_la_UP_khong_phai_loi():
    agg = HealthAggregator(HealthCheckRegistry([]))
    health = await agg.aggregate()
    assert health.state is State.UP
    assert health.healthy
    assert dict(health.details) == {}
