"""Module 09 — adapter goi Claude THAT. Khong bat buoc cai.

    uv sync --extra llm
    export ANTHROPIC_API_KEY=sk-ant-...
    uv run python -m prep.agent.demo

===========================================================================
VI SAO FILE NAY NAM TACH RIENG
===========================================================================
Toan bo `loop.py`, `tools.py`, `guardrails.py`, `evals.py` **khong import file nay**.
Chung chi biet Protocol `LLMClient`. He qua thuc te, khong phai ly thuyet:

  * 120+ test chay khong can API key, khong can mang, xong trong 3 giay.
  * Doi nha cung cap model = viet mot adapter moi, khong sua mot dong logic nao.
  * Nguoi khac clone repo ve la chay duoc ngay.

Day chinh la Dependency Inversion, nhung tri gia cua no do duoc bang giay chu khong
bang loi khen ve kien truc. Khi phong van hoi "vi sao ban dung interface o day", tra
loi bang ba gach dau dong tren, dung tra loi "de tuan thu SOLID".

Adapter la cho **duy nhat** biet ve `anthropic`. Neu sau nay them OpenAI/Gemini,
moi cai them mot file canh file nay, va khong file nao khac phai doi.
"""

from __future__ import annotations

import os
from collections.abc import Sequence
from typing import Any

from prep.agent.llm import LLMResponse, ToolCall, Usage
from prep.agent.tools import FatalToolError

DEFAULT_MODEL = "claude-sonnet-5"


class AnthropicLLM:
    """Adapter mong. **Khong chua logic nghiep vu nao** — day la co y.

    Adapter chi dich cau truc du lieu. Moi quyet dinh (retry, tran, cache, guardrail)
    nam o cac decorator/lop khac va ap dung duoc cho MOI nha cung cap. Nhet logic vao
    adapter la cach chac chan de sau nay khong doi duoc nha cung cap nua.
    """

    def __init__(
        self,
        model: str = DEFAULT_MODEL,
        max_tokens: int = 4096,
        api_key: str | None = None,
        client: Any = None,
    ) -> None:
        if client is not None:
            self._client = client
        else:
            try:
                import anthropic
            except ImportError as ex:  # pragma: no cover - phu thuoc tuy chon
                raise ImportError("can `uv sync --extra llm` de dung adapter nay") from ex
            key = api_key or os.environ.get("ANTHROPIC_API_KEY")
            if not key:
                raise ValueError("thieu ANTHROPIC_API_KEY")
            self._client = anthropic.Anthropic(api_key=key)
        self._model = model
        self._max_tokens = max_tokens

    def complete(
        self,
        system: str,
        messages: Sequence[dict[str, Any]],
        tools: Sequence[dict[str, Any]] = (),
    ) -> LLMResponse:
        kwargs: dict[str, Any] = {
            "model": self._model,
            "max_tokens": self._max_tokens,
            "system": system,
            "messages": list(messages),
        }
        if tools:
            kwargs["tools"] = list(tools)

        try:
            raw = self._client.messages.create(**kwargs)
        except Exception as ex:
            name = type(ex).__name__
            # Phan biet loi TAM THOI voi loi VINH VIEN — dung ranh gioi da dinh nghia
            # o `tools.py`. Retry mot loi xac thuc thi khong bao gio thanh cong, chi
            # lam su co keo dai va dot them han muc.
            if name in {"AuthenticationError", "PermissionDeniedError", "BadRequestError"}:
                raise FatalToolError(f"{name}: {ex}") from ex
            raise

        text_parts: list[str] = []
        calls: list[ToolCall] = []
        for block in raw.content:
            if getattr(block, "type", None) == "text":
                text_parts.append(block.text)
            elif getattr(block, "type", None) == "tool_use":
                calls.append(ToolCall(block.name, dict(block.input), block.id))

        usage = getattr(raw, "usage", None)
        return LLMResponse(
            text="".join(text_parts),
            tool_calls=tuple(calls),
            usage=Usage(
                input_tokens=getattr(usage, "input_tokens", 0) or 0,
                output_tokens=getattr(usage, "output_tokens", 0) or 0,
                cached_input_tokens=getattr(usage, "cache_read_input_tokens", 0) or 0,
            ),
            stop_reason=getattr(raw, "stop_reason", "end_turn") or "end_turn",
        )
