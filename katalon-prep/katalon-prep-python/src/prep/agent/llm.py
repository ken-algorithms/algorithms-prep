"""Module 09 — tang LLM. Cai nen de moi thu con lai test duoc.

===========================================================================
QUYET DINH THIET KE QUAN TRONG NHAT CUA CA MODULE
===========================================================================
Khong test nao trong module nay goi mang. Ly do khong phai "cho tien" — ma la:

  1. **Test goi API that thi khong phai test, ma la do luong.** No do ca chat luong
     mang, tinh trang nha cung cap, va tam trang cua model hom do. Do gi cung duoc
     tru "code cua toi dung hay sai".
  2. **Khong tat dinh thi khong dung lam cong CI duoc.** Test do lung tung se bi
     nguoi ta `@skip` sau dung 2 tuan.
  3. Ban van CAN mot it test goi that — nhung do la **eval**, chay theo lich va
     doc ket qua bang tay, khong phai unit test. Xem `evals.py`.

Nen o day: `LLMClient` la Protocol, `ScriptedLLM` la ban gia tat dinh, va
`AnthropicLLM` la adapter that (khong bat buoc cai). Do la Dependency Inversion,
nhung o dang co ly do kinh te ro rang chu khong phai vi sach ve nguyen tac.

Khi phong van hoi "ban test he thong LLM the nao?" — day la cau tra loi.
"""

from __future__ import annotations

import hashlib
import json
from collections.abc import Callable, Sequence
from dataclasses import dataclass, field
from typing import Any, Protocol


@dataclass(frozen=True, slots=True)
class Usage:
    """Dem token. **Phai dem tu dau**, khong phai them vao sau khi thay hoa don.

    Chi phi cua agent la ham cua SO VONG LAP x KICH THUOC CONTEXT. Ca hai deu de
    tang am tham: them mot tool, them mot vi du vao prompt, noi lai lich su hoi thoai.
    Khong dem thi khong ai phat hien cho toi ky thanh toan.
    """

    input_tokens: int = 0
    output_tokens: int = 0
    cached_input_tokens: int = 0

    def __add__(self, other: Usage) -> Usage:
        return Usage(
            self.input_tokens + other.input_tokens,
            self.output_tokens + other.output_tokens,
            self.cached_input_tokens + other.cached_input_tokens,
        )

    def cost_usd(self, in_per_mtok: float, out_per_mtok: float, cache_discount: float = 0.1) -> float:
        """Cache doc re hon nhieu — do la don bay chi phi lon nhat cua agent.

        Giu phan dau prompt (system + dinh nghia tool + tai lieu) CO DINH thi phan do
        duoc cache. Cho mot bien doi (vi du dan timestamp vao system prompt) la cache
        vo sach — day la bug chi phi rat pho bien va hoan toan vo hinh trong code.
        """
        return (
            self.input_tokens * in_per_mtok
            + self.cached_input_tokens * in_per_mtok * cache_discount
            + self.output_tokens * out_per_mtok
        ) / 1_000_000


@dataclass(frozen=True, slots=True)
class ToolCall:
    name: str
    arguments: dict[str, Any]
    call_id: str = "call-0"


@dataclass(frozen=True, slots=True)
class LLMResponse:
    """Model tra ve VAN BAN, hoac YEU CAU GOI TOOL, hoac ca hai."""

    text: str = ""
    tool_calls: tuple[ToolCall, ...] = ()
    usage: Usage = field(default_factory=Usage)
    stop_reason: str = "end_turn"

    @property
    def wants_tool(self) -> bool:
        return bool(self.tool_calls)


class LLMClient(Protocol):
    def complete(
        self,
        system: str,
        messages: Sequence[dict[str, Any]],
        tools: Sequence[dict[str, Any]] = (),
    ) -> LLMResponse: ...


class ScriptedLLM:
    """Ban gia TAT DINH: tra ve theo kich ban da soan.

    Khong phai `Mock` cua unittest — co chu y. `Mock` de dang mock nham chinh cai
    minh dang muon kiem tra, va khong ep ban nghi ro "model se tra ve gi trong tinh
    huong nay". Soan kich ban tay thi ban buoc phai nghi qua tung nhanh, ke ca nhanh
    xau (model doi tool sai, tra JSON hong, lap vo han).
    """

    def __init__(self, responses: Sequence[LLMResponse | Callable[..., LLMResponse]]) -> None:
        self._responses = list(responses)
        self._index = 0
        self.calls: list[dict[str, Any]] = []

    def complete(
        self,
        system: str,
        messages: Sequence[dict[str, Any]],
        tools: Sequence[dict[str, Any]] = (),
    ) -> LLMResponse:
        self.calls.append({"system": system, "messages": list(messages), "tools": list(tools)})
        if self._index >= len(self._responses):
            raise AssertionError(
                f"ScriptedLLM het kich ban o lan goi thu {self._index + 1}. "
                "Agent goi model nhieu hon ban du tinh - do CHINH LA thu can phat hien."
            )
        item = self._responses[self._index]
        self._index += 1
        return item(system, messages, tools) if callable(item) else item

    @property
    def call_count(self) -> int:
        return self._index


class CachingLLM:
    """Decorator cache theo noi dung — pattern y het `Executors.java` o module 01 Java.

    Vi sao dang lam: trong mot vong lap agent, cung mot cau hoi phu (vi du "tom tat
    trang nay") lap lai rat nhieu lan giua cac buoc. Cache dung cho lam giam chi phi
    hang chuc phan tram ma khong doi mot dong logic nao.

    BAY, va phai noi ra khi phong van: cache khoa theo **toan bo** prompt. Chi can
    mot dau thoi gian, mot uuid, mot dong lich su hoi thoai khac la truot sach.
    Ty le trung cache la mot chi so phai theo doi, khong phai dieu mac nhien co.
    """

    def __init__(self, inner: LLMClient) -> None:
        self._inner = inner
        self._cache: dict[str, LLMResponse] = {}
        self.hits = 0
        self.misses = 0

    def complete(
        self,
        system: str,
        messages: Sequence[dict[str, Any]],
        tools: Sequence[dict[str, Any]] = (),
    ) -> LLMResponse:
        key = hashlib.blake2b(
            json.dumps([system, list(messages), list(tools)], sort_keys=True, default=str).encode(),
            digest_size=16,
        ).hexdigest()
        if key in self._cache:
            self.hits += 1
            return self._cache[key]
        self.misses += 1
        result = self._inner.complete(system, messages, tools)
        self._cache[key] = result
        return result

    @property
    def hit_rate(self) -> float:
        total = self.hits + self.misses
        return self.hits / total if total else 0.0


class BudgetExceeded(Exception):
    """Nem khi agent vuot tran token. Chan bang HANG RAO, khong bang loi nhac nho."""


class BudgetedLLM:
    """Tran token CUNG. Vuot la nem, khong phai canh bao.

    Vi sao phai cung: mot agent bi loi logic co the lap vo han va dot het han muc trong
    vai phut. "Nho de y" khong phai la mot bien phap ky thuat. Dat tran o TANG HA TANG
    thi moi agent deu duoc bao ve, ke ca agent viet sau nay boi nguoi khac.
    """

    def __init__(self, inner: LLMClient, max_tokens: int) -> None:
        if max_tokens < 1:
            raise ValueError("max_tokens phai >= 1")
        self._inner = inner
        self._max = max_tokens
        self.used = Usage()

    def complete(
        self,
        system: str,
        messages: Sequence[dict[str, Any]],
        tools: Sequence[dict[str, Any]] = (),
    ) -> LLMResponse:
        total = self.used.input_tokens + self.used.output_tokens
        if total >= self._max:
            raise BudgetExceeded(f"da dung {total}/{self._max} token")
        response = self._inner.complete(system, messages, tools)
        self.used = self.used + response.usage
        return response

    @property
    def remaining(self) -> int:
        return max(0, self._max - self.used.input_tokens - self.used.output_tokens)


def estimate_tokens(text: str) -> int:
    """Uoc luong THO: ~4 ky tu = 1 token cho van ban tieng Anh.

    CO Y de tho, va phai noi ro khi bao cao: day KHONG phai so token that. Tokenizer
    that khac theo model, va tieng Viet co dau ton nhieu token hon tieng Anh dang ke.
    Dung cai nay de canh bao som ("prompt dang phinh"), tuyet doi khong dung de tinh
    tien hay de dat gioi han cung.
    """
    return max(1, len(text) // 4)
