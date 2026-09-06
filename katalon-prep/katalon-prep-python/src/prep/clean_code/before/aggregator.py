"""Module 01 — ban "TRUOC" cua health aggregator. CO Y viet xau.

Doi xung voi ``01-clean-code-solid/src/main/java/.../before/ServiceHealthAggregatorBefore.java``.
Cung mot lop van de, nhung viet theo dung cach nguoi ta hay viet trong Python — de ban
thay ro: **cung mot loi thiet ke, hai ngon ngu bieu hien khac nhau**.

BAI TAP: tu tim van de TRUOC khi mo ``../after/``. Muc tieu >= 10/13.
Danh sach day du + so hieu rule (ruff/Sonar) o ``../../../../katalon-prep-common/01-clean-code-solid.md``.

>>> KHONG dung code nay lam mau. Dung ``after/`` moi la mau. <<<
"""

from __future__ import annotations

import asyncio
import random

# [1] Trang thai toan cuc, mutable, chia se giua moi request.
#     Tuong duong `@Autowired` field injection ben Java: khong tiem duoc ban gia lap
#     khi test, va hai test chay song song se dam nhau.
_CACHE: dict[str, str] = {}
_CALL_COUNT = 0


async def check_db() -> str:
    await asyncio.sleep(0.01)
    return "UP"


async def check_redis() -> str:
    await asyncio.sleep(0.01)
    return "UP"


async def check_kafka() -> str:
    await asyncio.sleep(0.01)
    raise RuntimeError("kafka khong ket noi duoc")


async def check_s3() -> str:
    await asyncio.sleep(5.0)  # cham kinh khung — mo phong service treo
    return "UP"


async def check_smtp() -> str:
    await asyncio.sleep(0.01)
    return "UP"


# [2] 5 service da khai bao... nhung `if/elif` duoi chi xu ly 2.
#     DAY CHINH LA BUG TRONG RCI CUA BAN: 14 strategy @Autowired, switch xu ly 2.
#     Them service moi = phai sua ham nay = vi pham Open/Closed.
CHECKS = {
    "db": check_db,
    "redis": check_redis,
    "kafka": check_kafka,
    "s3": check_s3,
    "smtp": check_smtp,
}


async def do_health_check(  # [3] qua nhieu tham so, va co ca co boolean
    services: list[str],
    verbose: bool,
    include_details: bool,
    fail_fast: bool,
    tenant: str = "default",
    retries: int = 3,
) -> dict[str, str]:
    global _CALL_COUNT
    _CALL_COUNT += 1

    results = {}
    tasks = []
    for s in services:
        # [4] Chuoi dispatch tren TEN — them service moi la phai sua o day.
        #     Y het `switch` trong Strategy cua RCI.
        if s == "db":
            tasks.append(asyncio.create_task(check_db()))
        elif s == "redis":
            tasks.append(asyncio.create_task(check_redis()))
        else:
            # [5] 3 service con lai roi vao day va bi bao "DOWN" — trong khi
            #     su that la "KHONG BIET". Bao DOWN sai lam ca cum bi xoay vong
            #     lai vo co. UNKNOWN != DOWN.
            results[s] = "DOWN"

    # [6] `gather` KHONG co timeout. `check_s3` treo 5 giay -> ca endpoint health
    #     treo 5 giay. Health check cham hon liveness probe cua K8s thi chinh no
    #     lam pod bi kill — cong cu giam sat tro thanh nguyen nhan su co.
    #
    # [7] Va thieu `return_exceptions=True`: mot check nem thi `gather` HUY TAT CA
    #     anh em con lai roi nem ra ngoai. Ta mat sach thong tin ve nhung service
    #     VAN KHOE — dung thu can nhat luc dang co su co. Ben Java `allOf` khong tu
    #     huy nhu vay; day la khac biet that giua hai ngon ngu, xem test.
    done = await asyncio.gather(*tasks)

    # [8] BIEN CHET: tinh ra roi khong dung. ruff F841 / Sonar S1854.
    error_count = len([d for d in done if isinstance(d, Exception)])  # noqa: F841

    for i, s in enumerate([x for x in services if x in ("db", "redis")]):
        try:
            results[s] = done[i]
        # [9] bare except -> nuot CA BaseException, ke ca asyncio.CancelledError
        #     -> tac vu KHONG con huy duoc nua, timeout mat tac dung.
        #     Xem test `test_bare_except_nuot_CancelledError_lam_HUY_MAT_TAC_DUNG`.
        except:  # noqa: E722
            pass

    # [10] print thay vi logging — khong co level, khong co context,
    #      khong tat duoc o production. Sonar S106 / ruff T201.
    if verbose:
        print(f"health check xong: {results}")

    # [11] Chuoi lap lai khap noi thay vi hang so. Sonar S1192.
    if "db" in results and results["db"] == "DOWN":
        print("DB DOWN")
    if "redis" in results and results["redis"] == "DOWN":
        print("DB DOWN")  # <-- copy-paste sai, va khong ai phat hien vi chuoi roi rac

    # [12] Tra ve dict NOI BO. Caller sua duoc cache cua ta.
    _CACHE.update(results)
    return _CACHE


def flakiness(passed: int, total: int) -> float:
    # [13] Chia 0 -> ZeroDivisionError luc chay. Va so 100 la magic number.
    return (total - passed) / total * 100


def random_delay() -> float:
    # [14] `random` khong tiem duoc -> test khong the tat dinh.
    return random.random()
