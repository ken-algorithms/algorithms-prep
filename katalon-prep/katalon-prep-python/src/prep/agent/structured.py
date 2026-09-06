"""Module 09 — ep LLM tra ve du lieu CO CAU TRUC, va sua khi no tra sai.

Cau hoi phong van gan nhu chac chan: *"lam sao ban dam bao LLM tra ve dung dinh dang?"*

Cau tra loi te: "toi viet trong prompt la hay tra ve JSON".
Cau tra loi dung: **ba tang, tang sau bat cai tang truoc lot.**

  1. **Tool/schema** — bat model tra ve qua tool co input_schema. Manh nhat, vi rang
     buoc nam o tang giai ma chu khong phai o loi de nghi.
  2. **Validate** — van phai kiem tra. Schema ep duoc KIEU, khong ep duoc Y NGHIA
     ("selector phai ton tai trong DOM" thi khong schema nao bat duoc).
  3. **Sua-va-thu-lai co gioi han** — dua CHINH thong bao loi cho model va bao no sua.
     Ty le thanh cong rat cao o lan 2, nhung phai co tran, khong thi lap vo han.

Va mot tang thu tu it nguoi noi: **ghi nhan ty le hong**. Neu 30% phan hoi phai sua
thi van de nam o prompt/schema, khong phai o model. Khong do thi khong biet.
"""

from __future__ import annotations

import json
import re
from collections.abc import Callable, Mapping, Sequence
from dataclasses import dataclass, field
from typing import Any

_FENCE = re.compile(r"```(?:json)?\s*(.*?)```", re.DOTALL)


class ValidationFailed(Exception):
    def __init__(self, problems: Sequence[str]) -> None:
        super().__init__("; ".join(problems))
        self.problems = list(problems)


def extract_json(text: str) -> Any:
    """Rut JSON ra khoi van ban tu do.

    Model rat hay boc JSON trong ```json ... ``` hoac them mot cau dan truoc no, du
    prompt da bao dung. Doi thuc tai thay vi tranh cai voi no la re hon nhieu.

    Van co gioi han va PHAI biet: neu model tra ve hai khoi JSON, ham nay lay khoi
    dau tien. Khong doan mo them — mo ho thi nem, de tang tren xu ly.
    """
    fenced = _FENCE.search(text)
    candidate = fenced.group(1).strip() if fenced else text.strip()
    try:
        return json.loads(candidate)
    except json.JSONDecodeError:
        pass
    # Thu tim khoi { } hoac [ ] ngoai cung.
    for opener, closer in (("{", "}"), ("[", "]")):
        start, end = candidate.find(opener), candidate.rfind(closer)
        if start != -1 and end > start:
            try:
                return json.loads(candidate[start : end + 1])
            except json.JSONDecodeError:
                continue
    raise ValidationFailed([f"khong tim thay JSON hop le trong: {text[:200]!r}"])


@dataclass(frozen=True, slots=True)
class Field:
    name: str
    type: type | tuple[type, ...]
    required: bool = True
    check: Callable[[Any], str | None] | None = None
    """Tra ve None neu hop le, hoac CAU MO TA LOI de dua lai cho model.

    Thong bao loi la mot phan cua giao dien voi model. "khong hop le" thi model doan
    mo; "selector '#foo' khong co trong DOM, hay dung mot trong: #bar, #baz" thi model
    sua duoc ngay. Viet thong bao loi cho MODEL doc, khong phai cho log.
    """


@dataclass(frozen=True, slots=True)
class Schema:
    fields: tuple[Field, ...]
    allow_extra: bool = False

    def validate(self, data: Any) -> Mapping[str, Any]:
        if not isinstance(data, dict):
            raise ValidationFailed([f"can mot object JSON, nhan duoc {type(data).__name__}"])
        problems: list[str] = []
        for f in self.fields:
            if f.name not in data:
                if f.required:
                    problems.append(f"thieu truong bat buoc {f.name!r}")
                continue
            value = data[f.name]
            if not isinstance(value, f.type):
                expected = (
                    f.type.__name__ if isinstance(f.type, type) else "/".join(t.__name__ for t in f.type)
                )
                problems.append(f"{f.name!r} phai la {expected}, nhan duoc {type(value).__name__}")
                continue
            if f.check and (msg := f.check(value)):
                problems.append(f"{f.name!r}: {msg}")
        if not self.allow_extra and (extra := set(data) - {f.name for f in self.fields}):
            problems.append(f"truong khong duoc phep: {sorted(extra)}")
        if problems:
            raise ValidationFailed(problems)
        return data


@dataclass
class RepairStats:
    """Do ty le hong. Neu no cao thi loi o PROMPT, khong o model."""

    attempts: int = 0
    first_try_ok: int = 0
    repaired: int = 0
    gave_up: int = 0
    problems_seen: list[str] = field(default_factory=list)

    @property
    def first_try_rate(self) -> float:
        return self.first_try_ok / self.attempts if self.attempts else 0.0


def parse_with_repair(
    generate: Callable[[str | None], str],
    schema: Schema,
    max_attempts: int = 3,
    stats: RepairStats | None = None,
) -> Mapping[str, Any]:
    """Sinh -> validate -> neu hong thi dua LOI CU THE lai cho model va thu lai.

    `generate(feedback)` nhan `None` o lan dau, va nhan mo ta loi o cac lan sau.

    Vi sao gioi han so lan: mot model khong hieu schema se hong mai. Khong co tran thi
    day la vong lap vo han co tra phi. Ba lan la du — hong lan 3 thi van de la o
    schema/prompt cua ban, va luc do phai bao loi len tren de NGUOI xem.
    """
    if max_attempts < 1:
        raise ValueError("max_attempts phai >= 1")
    s = stats or RepairStats()
    s.attempts += 1
    feedback: str | None = None
    last: ValidationFailed | None = None

    for attempt in range(max_attempts):
        raw = generate(feedback)
        try:
            result = schema.validate(extract_json(raw))
        except ValidationFailed as ex:
            last = ex
            s.problems_seen.extend(ex.problems)
            feedback = (
                "Phan hoi truoc khong hop le. Loi cu the:\n"
                + "\n".join(f"- {p}" for p in ex.problems)
                + "\nHay tra ve LAI toan bo JSON da sua. Chi JSON, khong giai thich."
            )
            continue
        if attempt == 0:
            s.first_try_ok += 1
        else:
            s.repaired += 1
        return result

    s.gave_up += 1
    raise ValidationFailed(
        [f"khong hop le sau {max_attempts} lan", *(last.problems if last else [])]
    )
