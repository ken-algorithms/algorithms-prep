"""Module 09 — tool: dinh nghia, validate, dispatch, va xu ly khi tool HONG.

Phan lon huong dan ve agent chi day "dang ky tool roi goi". Phan kho — va phan
interviewer se dao — la **khi moi thu khong dien ra nhu y**:

  * model goi tool khong ton tai
  * model dua thieu tham so, hoac sai kieu
  * tool nem exception
  * tool tra ve 50.000 dong lam vo cua so context
  * model goi di goi lai dung mot tool voi dung tham so (ket vong lap)

Nguyen tac xuyen suot: **loi cua tool phai quay ve model duoi dang van ban, khong
duoc nem len tren.** Model co the tu sua (goi lai voi tham so dung). Nem len tren
la giet ca phien lam viec vi mot loi ma model tu chua duoc.

Nhung co gioi han: loi HA TANG (het han muc, mat mang) thi phai nem. Phan biet duoc
"loi model tu sua duoc" va "loi model khong the tu sua" la ranh gioi thiet ke chinh
cua file nay.
"""

from __future__ import annotations

import json
from collections.abc import Callable, Mapping
from dataclasses import dataclass, field
from typing import Any

MAX_TOOL_RESULT_CHARS = 4_000


class ToolFailure(Exception):
    """Loi model TU SUA DUOC -> tra ve cho model duoi dang text, khong nem len tren."""


class FatalToolError(Exception):
    """Loi model KHONG tu sua duoc (het han muc, mat ket noi) -> phai nem len tren.

    Neu gop chung voi `ToolFailure`, agent se lap lai mot loi ha tang cho toi khi het
    so vong — dot tien cho mot thu khong bao gio thanh cong. Day la mot trong nhung
    cach dot han muc nhanh nhat, va rat kho thay khi doc log.
    """


@dataclass(frozen=True, slots=True)
class Tool:
    name: str
    description: str
    parameters: Mapping[str, str]
    required: frozenset[str]
    handler: Callable[..., Any] = field(repr=False)

    def schema(self) -> dict[str, Any]:
        return {
            "name": self.name,
            "description": self.description,
            "input_schema": {
                "type": "object",
                "properties": {k: {"type": v} for k, v in self.parameters.items()},
                "required": sorted(self.required),
            },
        }


@dataclass(frozen=True, slots=True)
class ToolResult:
    call_id: str
    name: str
    content: str
    is_error: bool = False
    truncated: bool = False


class ToolRegistry:
    """Chan trung ten ngay luc dang ky — cung triet ly voi `HealthCheckRegistry`
    o module 01: loi cau hinh phai lo ra luc khoi dong, khong phai luc chay.
    """

    def __init__(self, tools: tuple[Tool, ...] = ()) -> None:
        self._tools: dict[str, Tool] = {}
        for t in tools:
            self.register(t)

    def register(self, tool: Tool) -> None:
        if tool.name in self._tools:
            raise ValueError(f"tool trung ten: {tool.name!r}")
        unknown = tool.required - set(tool.parameters)
        if unknown:
            # Bat cau hinh sai ngay: khai bao bat buoc mot tham so khong ton tai thi
            # model khong bao gio goi dung duoc, va loi se rat kho hieu.
            raise ValueError(f"tool {tool.name!r} bat buoc tham so khong khai bao: {sorted(unknown)}")
        self._tools[tool.name] = tool

    def schemas(self) -> list[dict[str, Any]]:
        return [t.schema() for t in self._tools.values()]

    def names(self) -> frozenset[str]:
        return frozenset(self._tools)

    def __len__(self) -> int:
        return len(self._tools)

    def dispatch(self, name: str, arguments: Mapping[str, Any], call_id: str = "call-0") -> ToolResult:
        """Goi tool. Loi model tu sua duoc -> ToolResult(is_error=True), KHONG nem."""
        tool = self._tools.get(name)
        if tool is None:
            # Goi y ten gan dung: giup model tu sua o luot sau thay vi doan mo.
            hint = _closest(name, self._tools)
            suffix = f" Y ban la {hint!r} phai khong?" if hint else ""
            return ToolResult(
                call_id,
                name,
                f"Loi: khong co tool ten {name!r}. Cac tool co: {sorted(self._tools)}.{suffix}",
                is_error=True,
            )

        missing = tool.required - set(arguments)
        if missing:
            return ToolResult(
                call_id, name, f"Loi: thieu tham so bat buoc {sorted(missing)}.", is_error=True
            )
        extra = set(arguments) - set(tool.parameters)
        if extra:
            return ToolResult(
                call_id,
                name,
                f"Loi: tham so khong hop le {sorted(extra)}. Chi nhan {sorted(tool.parameters)}.",
                is_error=True,
            )

        try:
            raw = tool.handler(**arguments)
        except FatalToolError:
            raise  # ha tang hong -> len tren, model khong tu chua duoc
        except Exception as ex:
            # Tra thong bao loi VE CHO MODEL. Day la cho agent tot khac agent te:
            # model doc duoc loi thi thuong tu sua duoc o luot sau.
            return ToolResult(call_id, name, f"Loi khi chay {name}: {ex}", is_error=True)

        text = raw if isinstance(raw, str) else json.dumps(raw, ensure_ascii=False, default=str)
        if len(text) > MAX_TOOL_RESULT_CHARS:
            # Cat bot: mot tool tra ve ca DOM 2MB se lam vo cua so context va lam
            # moi luot sau do dat gap boi. Cat co kem THONG BAO de model biet la
            # con thieu — cat im lang thi model tuong da co du du lieu roi ket luan sai.
            return ToolResult(
                call_id,
                name,
                text[:MAX_TOOL_RESULT_CHARS]
                + f"\n\n[da cat bot: con {len(text) - MAX_TOOL_RESULT_CHARS} ky tu. "
                "Hay thu hep truy van neu can phan con lai.]",
                truncated=True,
            )
        return ToolResult(call_id, name, text)


def _closest(name: str, tools: Mapping[str, Tool]) -> str | None:
    """Goi y ten gan nhat theo do trung ky tu — du dung, khong can thu vien ngoai."""
    best, best_score = None, 0.0
    for candidate in tools:
        common = len(set(name.lower()) & set(candidate.lower()))
        score = common / max(len(set(name)), len(set(candidate)), 1)
        if score > best_score:
            best, best_score = candidate, score
    return best if best_score >= 0.6 else None


# ---------------------------------------------------------------------------
# Tool on-domain Katalon — dung cho agent sinh test case
# ---------------------------------------------------------------------------


def make_default_registry(
    dom_snapshots: Mapping[str, str],
    journeys: Mapping[str, list[str]],
    saved: dict[str, dict[str, Any]] | None = None,
) -> ToolRegistry:
    """Bo tool toi thieu cho mot agent kieu TrueTest.

    `saved` la noi agent ghi ket qua — tiem tu ngoai vao de test kiem tra duoc,
    thay vi de agent ghi thang ra dia.
    """
    store = saved if saved is not None else {}

    def get_dom(url: str) -> str:
        if url not in dom_snapshots:
            raise ToolFailure(f"chua co ban chup DOM cho {url}. Co: {sorted(dom_snapshots)}")
        return dom_snapshots[url]

    def get_journey(journey_id: str) -> list[str]:
        if journey_id not in journeys:
            raise ToolFailure(f"khong co journey {journey_id!r}")
        return journeys[journey_id]

    def save_test_case(name: str, steps: str) -> str:
        if not name.strip():
            raise ToolFailure("ten test case khong duoc rong")
        parsed = json.loads(steps) if isinstance(steps, str) else steps
        if not isinstance(parsed, list) or not parsed:
            raise ToolFailure("steps phai la mang JSON khong rong")
        store[name] = {"name": name, "steps": parsed}
        return f"da luu test case {name!r} voi {len(parsed)} buoc"

    return ToolRegistry(
        (
            Tool(
                "get_dom",
                "Lay ban chup DOM cua mot URL da ghi lai.",
                {"url": "string"},
                frozenset({"url"}),
                get_dom,
            ),
            Tool(
                "get_journey",
                "Lay chuoi hanh dong cua mot user journey da khai pha duoc.",
                {"journey_id": "string"},
                frozenset({"journey_id"}),
                get_journey,
            ),
            Tool(
                "save_test_case",
                "Luu test case sinh ra. steps la chuoi JSON cua mang cac buoc.",
                {"name": "string", "steps": "string"},
                frozenset({"name", "steps"}),
                save_test_case,
            ),
        )
    )
