"""Module 09 — EVAL: kiem soat chat luong cua mot he thong khong tat dinh.

===========================================================================
CAU HOI PHONG VAN: "he thong cua ban khong tat dinh thi test kieu gi?"
===========================================================================
Cau tra loi day du gom **bon tang**, va cai lam nen muc Lead la biet tang nao dung
cho viec gi — cung nhu biet gia phai tra cua tung tang.

  1. **Unit test tat dinh** (LLM gia) — kiem tra CODE quanh model: vong lap, dispatch
     tool, validate, guardrail. Nhanh, chay moi commit. Day la phan LON NHAT va la
     phan hau het nguoi ta bo qua vi tuong "co LLM thi khong test duoc".
     -> chinh la `test_09_agent.py`.

  2. **Test tinh chat** (property) — voi MOI dau ra, bat bien nao phai luon dung?
     "moi selector phai co trong DOM", "khong buoc nao tro ra ngoai mien cho phep".
     Cai nay chay duoc CA voi model that: bat bien khong phu thuoc dau ra cu the.

  3. **Bo vang (golden set)** — N ca kem dau ra mong doi. Cham diem bang do do co
     phan bac (khong phai dung/sai), theo doi theo thoi gian. Cong CI la **khong
     duoc TUT so voi lan truoc**, khong phai "phai dat 100%".

  4. **LLM lam giam khao** — cho nhung thu khong cham may moc duoc ("test case nay co
     hop ly khong?"). Yeu nhat va dat nhat. Bay chinh: giam khao **thien vi dau ra
     cua chinh model do**, nen phai dung model khac va phai kiem tra lai giam khao
     bang chinh bo vang.

Va mot dieu it ai noi: **eval phai co ca ca AM.** Bo vang toan ca dep thi diem cao
ma he thong van vo o san xuat. Bo vang phai co: DOM bi injection, journey cut, DOM
khong co selector on dinh nao.
"""

from __future__ import annotations

from collections.abc import Callable, Sequence
from dataclasses import dataclass, field
from statistics import mean


@dataclass(frozen=True, slots=True)
class EvalCase:
    """Mot ca danh gia.

    `must_not_contain` la phan hay bi thieu: eval thuong chi kiem tra "co dung khong",
    khong kiem tra "co lam dieu nguy hiem khong". Voi agent thi ca AM quan trong ngang
    ca duong — mot agent tra loi dung 95% nhung 5% con lai gui du lieu ra ngoai thi
    van khong dung duoc.
    """

    case_id: str
    inputs: dict[str, object]
    expected_contains: tuple[str, ...] = ()
    must_not_contain: tuple[str, ...] = ()
    min_score: int = 0
    tags: tuple[str, ...] = ()


@dataclass(frozen=True, slots=True)
class CaseResult:
    case_id: str
    passed: bool
    score: int
    problems: tuple[str, ...] = ()


@dataclass
class EvalReport:
    results: list[CaseResult] = field(default_factory=list)

    @property
    def pass_rate(self) -> float:
        return sum(r.passed for r in self.results) / len(self.results) if self.results else 0.0

    @property
    def mean_score(self) -> float:
        return mean(r.score for r in self.results) if self.results else 0.0

    def failures(self) -> list[CaseResult]:
        return [r for r in self.results if not r.passed]

    def regressed_against(self, baseline: EvalReport) -> list[str]:
        """Ca nao TRUOC DAY dat ma BAY GIO hong.

        Day moi la cong CI dung, chu khong phai "ty le pass >= 90%". Ly do: mot thay
        doi co the lam ty le tong tang len ma van pha vo dung nhung ca quan trong nhat.
        So sanh TUNG ca moi phat hien duoc dieu do.
        """
        was_ok = {r.case_id for r in baseline.results if r.passed}
        return sorted(r.case_id for r in self.results if not r.passed and r.case_id in was_ok)

    def summary(self) -> str:
        return (
            f"{sum(r.passed for r in self.results)}/{len(self.results)} dat "
            f"({self.pass_rate:.0%}), diem trung binh {self.mean_score:.1f}"
        )


def run_eval(
    cases: Sequence[EvalCase],
    run_one: Callable[[EvalCase], tuple[str, int]],
) -> EvalReport:
    """`run_one` tra ve (van ban dau ra, diem).

    Mot ca NEM khong lam sap ca lan chay — no la mot ca HONG. Eval bi dung giua chung
    vi mot ca loi la ly do rat pho bien khien nguoi ta bo han viec chay eval.
    """
    report = EvalReport()
    for case in cases:
        try:
            output, score = run_one(case)
        except Exception as ex:
            report.results.append(CaseResult(case.case_id, False, 0, (f"nem: {type(ex).__name__}: {ex}",)))
            continue

        problems: list[str] = []
        lowered = output.lower()
        problems.extend(f"thieu {e!r}" for e in case.expected_contains if e.lower() not in lowered)
        problems.extend(f"CHUA {b!r} (ca am)" for b in case.must_not_contain if b.lower() in lowered)
        if score < case.min_score:
            problems.append(f"diem {score} < nguong {case.min_score}")
        report.results.append(CaseResult(case.case_id, not problems, score, tuple(problems)))
    return report


def pass_at_k(run: Callable[[], bool], k: int) -> tuple[bool, int]:
    """Chay `k` lan, tra ve (co lan nao dat khong, so lan dat).

    Vi sao can: mot he thong khong tat dinh dat 1/5 lan **khong phai** la he thong dat.
    Chay mot lan roi ket luan la sai lam do luong pho bien nhat khi danh gia LLM.

    Bao cao phai co ca `pass@1` (ty le dat khi chi duoc mot lan — sat thuc te nguoi
    dung nhat) lan `pass@k` (con thay duoc gi neu cho thu lai).
    """
    if k < 1:
        raise ValueError("k phai >= 1")
    successes = sum(run() for _ in range(k))
    return successes > 0, successes


def judge_agreement(
    judge_verdicts: Sequence[bool], human_labels: Sequence[bool]
) -> tuple[float, int, int]:
    """Do do khop giua LLM-giam-khao va nhan cua NGUOI. Tra ve (do khop, so duong gia, so am gia).

    **Buoc bat buoc truoc khi tin vao LLM-lam-giam-khao.** Giam khao chua duoc kiem
    dinh chi la mot y kien dat tien. Duoi ~0.8 do khop thi diem so cua no khong dung
    de ra quyet dinh duoc.

    Trong hai loai sai, **duong gia nguy hiem hon**: giam khao cho qua mot dau ra hong
    nghia la hong lot toi khach hang. Am gia chi lam ban mat cong xem lai.
    """
    if len(judge_verdicts) != len(human_labels):
        raise ValueError("hai danh sach phai cung do dai")
    if not judge_verdicts:
        return 1.0, 0, 0
    agree = sum(j == h for j, h in zip(judge_verdicts, human_labels, strict=True))
    false_pos = sum(j and not h for j, h in zip(judge_verdicts, human_labels, strict=True))
    false_neg = sum(not j and h for j, h in zip(judge_verdicts, human_labels, strict=True))
    return agree / len(judge_verdicts), false_pos, false_neg
