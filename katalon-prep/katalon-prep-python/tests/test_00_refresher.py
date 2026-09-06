"""Module 00 — on Python bang test tu kiem tra.

CACH DUNG DUNG: doc ten test, TU DOAN ket qua, roi moi chay. Cho nao doan sai la
cho ban thuc su co lo hong. Doc thang assert thi khong hoc duoc gi.

Phan lon test o day la BAY — nhung bay ma neu gap luc live coding se lam ban dung hinh.
"""

from __future__ import annotations

import copy
import hashlib
import math
import subprocess
import sys
from dataclasses import dataclass
from datetime import timedelta

import pytest

from prep.refresher.results import (
    DurationSource,
    Failed,
    Flaky,
    MutableRun,
    Passed,
    Skipped,
    Status,
    build_report,
    classify,
    describe,
    duration_of,
    percentile,
)

# ---------------------------------------------------------------------------
# Phan A — pattern matching & domain
# ---------------------------------------------------------------------------


def ms(n: int) -> timedelta:
    return timedelta(milliseconds=n)


def test_flaky_phai_duoc_tinh_rieng_khong_gop_vao_passed():
    assert classify(Flaky("login", ms(500), attempts=3)) is Status.FLAKY
    assert classify(Passed("login", ms(500))) is Status.PASSED


def test_flaky_voi_1_lan_chay_la_vo_nghia_nen_bi_chan_ngay_luc_tao():
    with pytest.raises(ValueError):
        Flaky("login", ms(10), attempts=1)


def test_guarded_pattern_message_rong_van_phai_ra_cau_co_nghia():
    assert describe(Failed("checkout", ms(10), "   ")) == "checkout: that bai khong ro ly do"
    assert describe(Failed("checkout", ms(10), "timeout")) == "checkout: timeout"


def test_match_thieu_nhanh_thi_NEM_chu_khong_im_lang():
    """Day la cai gia phai tra khi Python khong co `sealed`.

    Java compiler bat loi ngay luc BUILD. Python chi phat hien luc CHAY, va chi khi
    ban chu dong viet `case _: raise`. Bo nhanh do di thi ham tra None am tham.
    """

    class Aborted:  # loai ket qua moi, quen cap nhat `classify`
        pass

    with pytest.raises(TypeError, match="chua xu ly"):
        classify(Aborted())  # type: ignore[arg-type]


def test_skipped_khong_co_duration_tra_None_chu_khong_phai_zero():
    assert duration_of(Skipped("payment", "chua co env")) is None
    assert duration_of(Passed("login", ms(120))) == ms(120)


def test_percentile_nearest_rank_luon_tra_ve_mot_lan_do_CO_THAT():
    data = [ms(10), ms(20), ms(30), ms(40), ms(100)]
    assert percentile(data, 95) == ms(100)
    assert percentile(data, 50) == ms(30)
    # statistics.quantiles NOI SUY -> tra ve so khong ton tai trong du lieu.
    # Voi bao cao SLA thi day la khac biet that, khong phai chi tiet vun.
    assert percentile([], 95) == timedelta(0)


def test_bao_cao_khong_bao_gio_chia_cho_0():
    empty = build_report([])
    assert empty.total == 0
    assert empty.flakiness_rate == 0.0
    assert not math.isnan(empty.flakiness_rate)


def test_bao_cao_bo_qua_skipped_khi_tinh_slowest():
    report = build_report(
        [
            Passed("a", ms(100)),
            Skipped("b", "no env"),
            Flaky("c", ms(900), attempts=2),
            Failed("d", ms(50), "boom"),
        ]
    )
    assert report.total == 4
    assert report.slowest == "c"
    assert report.by_status[Status.FLAKY] == 1
    assert report.flakiness_rate == 0.25
    # BAY doi xung voi Java `groupingBy`: dict KHONG tao key cho nhom rong.
    assert Status.PASSED in report.by_status
    assert len(report.by_status) == 4


def test_generator_chi_duyet_duoc_MOT_lan():
    """Ly do `build_report` goi `list(results)` ngay dong dau.

    Ben Java, `Stream` da tieu thu ma dung lai thi NEM IllegalStateException — on ao.
    Python thi im lang tra ve rong. Bug im lang nguy hiem hon nhieu.
    """
    gen = (Passed(f"t{i}", ms(i)) for i in range(3))
    assert build_report(gen).total == 3
    assert list(gen) == []  # da can kiet, khong con gi
    assert build_report(gen).total == 0  # <-- neu build_report khong list() thi day la bug


def test_protocol_la_structural_khong_can_khai_bao_implements():
    class MyTiming:  # khong ke thua gi ca
        def duration(self) -> timedelta:
            return ms(5)

    assert isinstance(MyTiming(), DurationSource)


def test_runtime_checkable_CHI_kiem_tra_ten_method_khong_kiem_tra_chu_ky():
    """Lo hong that cua Protocol — phai biet de tra loi khi bi hoi.

    Class duoi day co `duration` nhung tra ve str va can them tham so. `isinstance`
    van bao True. Type checker tinh (mypy) bat duoc; runtime thi khong.
    """

    class Broken:
        def duration(self, unit, scale):  # chu ky hoan toan khac
            return "khong phai timedelta"

    assert isinstance(Broken(), DurationSource)  # <-- True, va do la van de


# ---------------------------------------------------------------------------
# Phan B — BAY. Day moi la phan dang gia.
# ---------------------------------------------------------------------------


def test_bay_modulo_so_am_KHAC_HAN_java():
    """Bay nay truc tiep lien quan toi Kafka partitioner o module 06.

    Java:   -7 % 3  ==  -1   -> dung lam index mang la IndexOutOfBounds -> phai floorMod
    Python: -7 % 3  ==   2   -> Python da floor san, KHONG can floorMod

    Nghia la code partitioner port tu Java sang Python ma buong `floorMod` di thi
    DUNG; port nguoc lai tu Python sang Java ma quen them thi VO. Bat duoc cho nay
    khi review la thu interviewer nho.
    """
    assert -7 % 3 == 2
    assert math.fmod(-7, 3) == -1.0  # fmod moi giong Java
    assert -7 // 2 == -4  # floor, KHONG phai truncate (Java: -7/2 == -3)
    assert int(-7 / 2) == -3  # muon giong Java thi phai ep kieu nhu the nay


def test_bay_hash_cua_str_KHONG_on_dinh_giua_cac_process():
    """Bay nghiem trong nhat khi tu viet partitioner bang Python.

    `hash("abc")` doi moi lan chay Python (PYTHONHASHSEED ngau nhien - chong DoS).
    Dung no de chia partition -> cung mot key roi vao partition KHAC nhau sau khi
    restart consumer -> mat hoan toan dam bao thu tu, va bug chi hien sau deploy.

    Phai dung hash CO DINH: hashlib / zlib.crc32 / mmh3.
    """
    code = "print(hash('tenant-a'))"
    a = subprocess.run([sys.executable, "-c", code], capture_output=True, text=True).stdout
    b = subprocess.run([sys.executable, "-c", code], capture_output=True, text=True).stdout
    assert a != b, "neu bang nhau thi PYTHONHASHSEED dang bi ghim - hiem"

    def stable_partition(key: str, n: int) -> int:
        digest = hashlib.blake2b(key.encode(), digest_size=8).digest()
        return int.from_bytes(digest, "big") % n

    assert stable_partition("tenant-a", 4) == stable_partition("tenant-a", 4)


def test_bay_mutable_default_argument():
    """Default duoc tao MOT LAN luc dinh nghia ham, khong phai moi lan goi."""

    def collect(name: str, acc: list[str] = []):
        acc.append(name)
        return acc

    assert collect("a") == ["a"]
    assert collect("b") == ["a", "b"]  # <-- ro ri giua cac lan goi


def test_dataclass_CHAN_mutable_default_nhung_ham_thuong_thi_khong():
    with pytest.raises(ValueError, match="mutable default"):

        @dataclass
        class Bad:
            items: list = []  # noqa: RUF008

    run = MutableRun("run-1")
    run.add(Passed("a", ms(1)))
    assert len(MutableRun("run-2").results) == 0  # field(default_factory) -> khong ro ri


def test_bay_late_binding_closure():
    """Lambda giu THAM CHIEU toi bien, khong giu gia tri luc tao."""
    wrong = [lambda: i for i in range(3)]
    assert [f() for f in wrong] == [2, 2, 2]

    right = [lambda i=i: i for i in range(3)]  # bind ngay bang default arg
    assert [f() for f in right] == [0, 1, 2]


def test_bay_dict_gop_key_trung_thi_GHI_DE_con_java_Map_of_thi_NEM():
    """Doi xung voi bay `Set.of`/`Map.of` o module 00 ben Java.

    Java: `Map.of("a",1,"a",2)` NEM IllegalArgumentException -> loi lo ngay.
    Python: im lang lay gia tri cuoi -> mat du lieu ma khong ai biet.

    Khi gop config/ket qua tu nhieu nguon, cho nay lam mat ban ghi rat am tham.
    """
    merged = {"a": 1, "a": 2}
    assert merged == {"a": 2}

    # Muon on ao nhu Java thi phai TU kiem tra:
    def strict_merge(*ds: dict) -> dict:
        out: dict = {}
        for d in ds:
            dup = out.keys() & d.keys()
            if dup:
                raise ValueError(f"key trung: {sorted(dup)}")
            out |= d
        return out

    with pytest.raises(ValueError, match="key trung"):
        strict_merge({"a": 1}, {"a": 2})


def test_bay_dict_giu_thu_tu_nhung_set_thi_KHONG():
    assert list({"c": 1, "a": 1, "b": 1}) == ["c", "a", "b"]
    # set khong dam bao thu tu -> dung set roi bao cao "danh sach test that bai"
    # se ra thu tu khac nhau moi lan chay -> diff cua CI nhay lung tung.
    assert sorted({"c", "a", "b"}) == ["a", "b", "c"]


def test_bay_shallow_copy_cua_dict_long_nhau():
    original = {"tenant-a": {"failed": ["login"]}}
    shallow = original.copy()
    shallow["tenant-a"]["failed"].append("checkout")
    assert original["tenant-a"]["failed"] == ["login", "checkout"]  # <-- da bi sua

    deep = copy.deepcopy(original)
    deep["tenant-a"]["failed"].append("payment")
    assert original["tenant-a"]["failed"] == ["login", "checkout"]  # lan nay an toan


def test_bay_xoa_phan_tu_trong_luc_dang_duyet_lam_BO_SOT():
    items = ["a", "b", "b", "c"]
    for x in list(items):  # duyet BAN SAO -> dung
        if x == "b":
            items.remove(x)
    assert items == ["a", "c"]

    broken = ["a", "b", "b", "c"]
    for x in broken:  # duyet CHINH no -> nhay index
        if x == "b":
            broken.remove(x)
    assert broken == ["a", "b", "c"]  # <-- con sot mot "b"


def test_bay_except_Exception_KHONG_bat_het_moi_thu():
    """`except Exception` bo qua BaseException (KeyboardInterrupt, SystemExit).

    Do la thiet ke DUNG: Ctrl-C phai thoat duoc. Nguoi ta hay sua thanh
    `except BaseException` "cho chac" -> khong tat duoc service nua.
    """
    with pytest.raises(SystemExit):
        try:
            raise SystemExit(1)
        except Exception:
            pytest.fail("khong bao gio toi day")


def test_bay_so_thuc_khong_duoc_so_sanh_bang_dau_bang():
    """Ly do `results.py` dung `timedelta` chu khong dung float giay.

    LUU Y — toi da doan sai cho nay khi viet test lan dau: toi tuong
    `sum([0.1] * 10) != 1.0`. Chay ra thi no BANG DUNG 1.0. Cac sai so cong don
    trong truong hop do triet tieu nhau. Bay that nam o cho khac:
    """
    assert 0.1 + 0.2 != 0.3  # <-- day moi la bay that
    assert pytest.approx(0.3) == 0.1 + 0.2
    assert sum([0.1] * 10) == 1.0  # va day thi... khong sao ca

    # `sum` vs `fsum` chi lech khi cac so chenh nhau qua nhieu bac (mat hoan toan
    # phan nho khi cong voi phan lon). Voi duration test thi hiem, nhung day la
    # ly do ky thuat: cong don theo THU TU khac nhau ra ket qua khac nhau.
    assert sum([1, 1e100, 1, -1e100]) == 0.0
    assert math.fsum([1, 1e100, 1, -1e100]) == 2.0

    # `timedelta` luu microsecond dang SO NGUYEN -> cong bao nhieu lan cung chinh xac.
    assert sum([timedelta(seconds=0.1)] * 10, timedelta(0)) == timedelta(seconds=1)


def test_bay_split_rong_khac_split_dau_cach():
    raw = "login   checkout"
    assert raw.split() == ["login", "checkout"]
    assert raw.split(" ") == ["login", "", "", "checkout"]  # <-- 2 chuoi rong
