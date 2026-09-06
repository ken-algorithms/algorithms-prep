"""Module 09 — VONG LAP AGENT. File quan trong nhat cua ca module.

Vong lap agent chi co 20 dong. Cai lam no khac nhau giua do choi va san pham la
**cac dieu kien dung**. Day la danh sach day du cac cach mot agent that bai trong
san xuat, va cach chan tung cai:

  | Cach that bai              | Trieu chung                       | Chan bang            |
  |----------------------------|-----------------------------------|----------------------|
  | Lap vo han                 | hoa don tang, khong bao gio xong  | `max_steps`          |
  | Lap MOT hanh dong          | goi y het tool + tham so mai      | phat hien chu trinh  |
  | Phinh context              | luot sau dat gap boi luot dau     | cat ket qua tool     |
  | Chay tran han muc          | het tien giua ca                  | `BudgetedLLM`        |
  | Loi ha tang bi retry       | dot han muc vao viec vo vong      | `FatalToolError`     |
  | Dung khi chua xong         | tra ket qua thieu, im lang        | kiem tra dieu kien   |
  | Khong truy nguyen duoc     | khong biet no da lam gi           | `trace`              |

Bang nay chinh la cau tra loi cho "ban dua agent len san xuat the nao?". Hoc thuoc
mot cai (thuong la `max_steps`) thi de; noi duoc ca bay va noi duoc CAI GIA cua tung
bien phap moi la muc Lead.
"""

from __future__ import annotations

import json
from collections.abc import Sequence
from dataclasses import dataclass, field
from enum import Enum
from typing import Any

from prep.agent.llm import BudgetExceeded, LLMClient, LLMResponse, Usage
from prep.agent.tools import ToolRegistry, ToolResult


class StopReason(Enum):
    DONE = "done"
    MAX_STEPS = "max_steps"
    BUDGET = "budget"
    LOOP_DETECTED = "loop_detected"
    FATAL = "fatal"


@dataclass(slots=True)
class Step:
    index: int
    response: LLMResponse
    tool_results: tuple[ToolResult, ...] = ()


@dataclass(slots=True)
class AgentRun:
    """Ket qua mot phien. **Luon tra ve, khong bao gio nem** cho cac ket thuc du kien.

    Ly do: "het so vong" hay "het han muc" khong phai loi cua chuong trinh — do la
    ket qua hop le ma nguoi goi phai xu ly (bao cho nguoi dung, thu lai voi tran cao
    hon, hoac chap nhan ket qua tung phan). Nem exception cho nhung cai nay lam nguoi
    goi buoc phai `try/except` cho luong BINH THUONG.
    """

    text: str
    stop_reason: StopReason
    steps: list[Step] = field(default_factory=list)
    usage: Usage = field(default_factory=Usage)
    error: str | None = None

    @property
    def succeeded(self) -> bool:
        return self.stop_reason is StopReason.DONE

    @property
    def step_count(self) -> int:
        return len(self.steps)

    def tool_names_called(self) -> list[str]:
        return [r.name for s in self.steps for r in s.tool_results]

    def trace(self) -> str:
        """Vet chay doc duoc bang mat. Khong co cai nay thi khong debug duoc gi.

        Log cua agent phai tra loi duoc: no goi tool nao, voi tham so nao, nhan lai
        gi, va vi sao no dung. Thieu bat ky manh nao thi su co tro thanh doan mo.
        """
        lines = []
        for s in self.steps:
            if s.response.text:
                lines.append(f"[{s.index}] noi: {s.response.text[:120]}")
            for call in s.response.tool_calls:
                lines.append(f"[{s.index}] goi {call.name}({json.dumps(call.arguments, ensure_ascii=False)})")
            for r in s.tool_results:
                tag = "LOI" if r.is_error else "ok"
                lines.append(f"[{s.index}] <- {r.name} [{tag}] {r.content[:120]}")
        lines.append(f"=> dung vi: {self.stop_reason.value}")
        return "\n".join(lines)


class AgentLoop:
    def __init__(
        self,
        llm: LLMClient,
        registry: ToolRegistry,
        system: str,
        max_steps: int = 8,
        loop_window: int = 3,
    ) -> None:
        if max_steps < 1:
            raise ValueError("max_steps phai >= 1")
        self._llm = llm
        self._registry = registry
        self._system = system
        self._max_steps = max_steps
        self._loop_window = loop_window

    def run(self, user_message: str) -> AgentRun:
        messages: list[dict[str, Any]] = [{"role": "user", "content": user_message}]
        run = AgentRun(text="", stop_reason=StopReason.MAX_STEPS)
        recent_calls: list[str] = []

        for index in range(self._max_steps):
            try:
                response = self._llm.complete(self._system, messages, self._registry.schemas())
            except BudgetExceeded as ex:
                run.stop_reason = StopReason.BUDGET
                run.error = str(ex)
                # Van tra ve nhung gi da lam duoc. Ket qua tung phan van co gia tri —
                # vut het di la lang phi ca tien lan thoi gian da bo ra.
                return run

            run.usage = run.usage + response.usage
            step = Step(index=index, response=response)
            run.steps.append(step)

            if not response.wants_tool:
                run.text = response.text
                run.stop_reason = StopReason.DONE
                return run

            # --- phat hien lap: cung tool + cung tham so lap lai ---
            signature = "|".join(
                f"{c.name}:{json.dumps(c.arguments, sort_keys=True, default=str)}"
                for c in response.tool_calls
            )
            recent_calls.append(signature)
            if len(recent_calls) >= self._loop_window and len(set(recent_calls[-self._loop_window:])) == 1:
                # Chi `max_steps` thoi la khong du: mot agent ket o buoc 2 van se dot
                # het `max_steps` luot roi moi dung. Phat hien chu trinh cat som hon
                # nhieu, va cho ra ly do dung ro rang de bao cao.
                run.stop_reason = StopReason.LOOP_DETECTED
                run.error = f"lap lai cung mot loi goi {self._loop_window} lan: {signature[:120]}"
                run.text = response.text
                return run

            messages.append({"role": "assistant", "content": _assistant_content(response)})

            results: list[ToolResult] = []
            try:
                for call in response.tool_calls:
                    results.append(self._registry.dispatch(call.name, call.arguments, call.call_id))
            except Exception as ex:  # FatalToolError va moi loi ha tang khac
                step.tool_results = tuple(results)
                run.stop_reason = StopReason.FATAL
                run.error = f"{type(ex).__name__}: {ex}"
                return run

            step.tool_results = tuple(results)
            messages.append({"role": "user", "content": _tool_result_content(results)})

        run.error = f"khong hoan thanh trong {self._max_steps} buoc"
        return run


def _assistant_content(response: LLMResponse) -> list[dict[str, Any]]:
    blocks: list[dict[str, Any]] = []
    if response.text:
        blocks.append({"type": "text", "text": response.text})
    blocks.extend(
        {"type": "tool_use", "id": c.call_id, "name": c.name, "input": c.arguments}
        for c in response.tool_calls
    )
    return blocks


def _tool_result_content(results: Sequence[ToolResult]) -> list[dict[str, Any]]:
    return [
        {
            "type": "tool_result",
            "tool_use_id": r.call_id,
            "content": r.content,
            "is_error": r.is_error,
        }
        for r in results
    ]
