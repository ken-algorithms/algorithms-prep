"""Module 09 — mien nghiep vu: tu USER JOURNEY sinh ra TEST CASE.

Day chinh la vong doi cua TrueTest noi gon: **discover -> model -> generate -> maintain**.
Ban chi lam khuc `generate`, nhung lam den noi den chon, va do la du de noi chuyen
duoc ca vong.

Cai dang chu y o file nay khong phai prompt — ma la `grounding_score`: **cach do xem
dau ra co that su bam vao du lieu that hay khong.** Do la thu bien "AI sinh test case"
tu mot demo thanh mot san pham dam giao cho khach.
"""

from __future__ import annotations

import json
import re
from collections.abc import Mapping, Sequence
from dataclasses import dataclass

from prep.agent.guardrails import (
    GuardrailReport,
    OutputPolicy,
    decide,
    scan_for_injection,
    wrap_untrusted,
)
from prep.agent.structured import Field, Schema, ValidationFailed, extract_json

# HAI regex rieng, KHONG gop thanh `(?:id|data-testid)`.
# Toi gop lan dau va no SAI: trong `data-testid="email-input"`, nhanh `id` khop vao
# hai chu cuoi cua "testid" -> ra `#email-input` thay vi `[data-testid='email-input']`.
# `\bid` khong khop duoc nhu vay vi "testid" la mot tu lien, khong co ranh gioi tu o giua.
_ID_ATTR = re.compile(r"""\bid\s*=\s*["']([^"']+)["']""")
_TESTID_ATTR = re.compile(r"""\bdata-testid\s*=\s*["']([^"']+)["']""")

ACTIONS = frozenset({"navigate", "click", "type", "assert_text", "assert_visible", "wait"})


@dataclass(frozen=True, slots=True)
class TestStep:
    # pytest gom moi class ten `Test*` lam test class -> canh bao va bo qua nham.
    # Day la class NGHIEP VU, khong phai test.
    __test__ = False

    action: str
    selector: str = ""
    value: str = ""
    url: str = ""

    def as_dict(self) -> dict[str, str]:
        return {k: v for k, v in
                {"action": self.action, "selector": self.selector,
                 "value": self.value, "url": self.url}.items() if v}


@dataclass(frozen=True, slots=True)
class GeneratedTestCase:
    __test__ = False

    name: str
    steps: tuple[TestStep, ...]
    source_journey: str


def selectors_in_dom(html: str) -> frozenset[str]:
    """Rut cac selector ON DINH ra khoi DOM: `#id` va `[data-testid=...]`.

    CO Y chi lay hai loai nay chu khong lay class. Ly do giong het bang do ben
    locator o module 01 (Java): class doi moi lan refactor CSS, con `data-testid`
    do QA/dev chu dong dat nen gan nhu khong doi. Sinh test bam vao class la sinh ra
    test se hong trong hai tuan — tuc la tai san am.
    """
    found = {f"#{m.group(1)}" for m in _ID_ATTR.finditer(html)}
    found |= {f"[data-testid='{m.group(1)}']" for m in _TESTID_ATTR.finditer(html)}
    return frozenset(found)


TEST_CASE_SCHEMA = Schema(
    fields=(
        Field("name", str, check=lambda v: None if v.strip() else "ten khong duoc rong"),
        Field("steps", list, check=lambda v: None if v else "phai co it nhat mot buoc"),
    )
)


def parse_test_case(raw: str, journey_id: str) -> GeneratedTestCase:
    data = TEST_CASE_SCHEMA.validate(extract_json(raw))
    steps: list[TestStep] = []
    problems: list[str] = []
    for i, s in enumerate(data["steps"]):
        if not isinstance(s, dict):
            problems.append(f"buoc {i} phai la object")
            continue
        action = s.get("action", "")
        if action not in ACTIONS:
            # Thong bao loi viet CHO MODEL doc: liet ke lua chon hop le de no sua duoc
            # ngay o luot sau, thay vi doan.
            problems.append(f"buoc {i}: action {action!r} khong hop le, chon trong {sorted(ACTIONS)}")
            continue
        steps.append(
            TestStep(
                action=action,
                selector=str(s.get("selector", "")),
                value=str(s.get("value", "")),
                url=str(s.get("url", "")),
            )
        )
    if problems:
        raise ValidationFailed(problems)
    return GeneratedTestCase(str(data["name"]).strip(), tuple(steps), journey_id)


def grounding_score(case: GeneratedTestCase, dom: str, journey: Sequence[str]) -> int:
    """0-100: dau ra bam vao du lieu THAT den muc nao.

    =====================================================================
    GROUNDING LA CONG NHAN, KHONG PHAI DIEM CONG.
    =====================================================================
    Lan dau toi viet ham nay theo kieu cong don: 50 diem selector + 30 diem journey
    + 20 diem assertion. Chay test moi lo ra la SAI han:

        mot test case co selector BIA RA HOAN TOAN van duoc 50/100 —
        vi no dung ten hanh dong hop le va co mot buoc assert.

    50 diem cho mot thu khong the chay duoc. Neu nguong review la 40 thi rac nay
    da di thang toi mat nguoi duyet.

    Cong thuc dung: ty le selector co that la **he so nhan**, khong phai so hang.

        diem = 100 x ty_le_selector_that x (0.5 + 0.3 x bam_journey + 0.2 x co_assert)

    Khong selector nao co that -> nhan 0 -> tong 0, bat ke phan con lai dep the nao.
    Do la dung: mot test khong bam duoc vao DOM that thi moi uu diem khac deu vo nghia.

    Bai hoc chung, dung cho moi he thong cham diem: **dieu kien CAN phai nhan, dieu
    kien TOT moi duoc cong.** Tron hai loai vao mot phep cong la cach de nhat de
    tao ra mot con diem trong co ve hop ly ma hoan toan vo dung.

    Ba thanh phan:
      * he so — moi selector deu ton tai trong DOM (chong hallucination VA injection)
      * 50% — co san
      * 30% — cac hanh dong bam theo journey that (chong "sang tao" luong khong ai di)
      * 20% — co assertion. Nghe vun nhung khong he: LLM rat hay sinh chuoi thao tac
        dep de ma khong kiem tra gi. Test do LUON xanh — loai test te nhat, vi no tao
        cam giac an toan gia.

    Va mot quyet dinh thiet ke nua: cham diem bang CODE, khong hoi LLM. Lay chinh
    model tu cham diem minh la mot vong lap kin — no tu tin nhat dung luc no sai nhat.
    """
    if not case.steps:
        return 0
    known = selectors_in_dom(dom)

    used = [s.selector for s in case.steps if s.selector]
    # Khong dung selector nao (vi du chi co buoc `navigate`) -> khong co gi de bia,
    # nen he so la 1.0. Khong phai 0: "khong co bang chung sai" khac "co bang chung sai".
    selector_ratio = 1.0 if not used else sum(s in known for s in used) / len(used)

    journey_actions = {a.lower() for a in journey}
    matched = sum(1 for s in case.steps if s.action.lower() in journey_actions
                  or any(s.action.lower() in a or a in s.action.lower() for a in journey_actions))
    journey_ratio = matched / len(case.steps)
    has_assert = any(s.action.startswith("assert") for s in case.steps)

    quality = 0.5 + 0.3 * journey_ratio + 0.2 * has_assert
    return max(0, min(100, int(100 * selector_ratio * quality)))


def build_prompt(journey_id: str, journey: Sequence[str], dom: str) -> str:
    """Dung prompt voi ranh gioi tin cay ro rang.

    Y quan trong: DOM duoc boc trong `wrap_untrusted`, journey thi khong — vi journey
    do CHINH HE THONG CUA TA khai pha ra tu telemetry, con DOM la cua nguoi ngoai.
    Phan biet duoc hai nguon nay ngay trong prompt la mot phan cua thiet ke bao mat,
    khong phai chi la trinh bay cho dep.
    """
    return (
        f"Journey can phu: {journey_id}\n"
        f"Cac buoc da quan sat duoc (nguon tin cay - telemetry cua ta): {json.dumps(list(journey))}\n\n"
        + wrap_untrusted(dom, "dom-snapshot")
        + "\n\nSinh mot test case JSON: {\"name\": ..., \"steps\": [{\"action\", \"selector\", "
        '"value", "url"}]}.\n'
        f"action chi duoc dung trong: {sorted(ACTIONS)}.\n"
        "Chi dung selector CO THAT trong DOM tren. Phai co it nhat mot buoc assert."
    )


def review(
    case: GeneratedTestCase,
    dom: str,
    journey: Sequence[str],
    policy: OutputPolicy,
) -> GuardrailReport:
    """Chay het moi lop kiem tra roi ra MOT quyet dinh: auto / review / reject."""
    report = GuardrailReport()
    report.findings = scan_for_injection(dom)
    report.policy_problems = policy.check_test_case([s.as_dict() for s in case.steps])
    report.decision = decide(report.policy_problems, report.findings, grounding_score(case, dom, journey))
    return report


def policy_for(dom: str, allowed_domains: Mapping[str, None] | Sequence[str]) -> OutputPolicy:
    return OutputPolicy(
        allowed_domains=frozenset(allowed_domains),
        known_selectors=selectors_in_dom(dom),
    )
