"""Đọc từng câu, ghép tiếng thành một dải, tính khung hình nào hiện trong bao lâu.

Lấy từ `agents/lesson_video/timeline.py` của repo superken-ielts/ielts-target-5-5, bỏ phần phát file nghe của sách.
"""
from __future__ import annotations

import hashlib
import math
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Optional

import numpy as np

from .script import Lesson
from .tts import TTSError

LEAD = 0.5   # im lặng đầu video
TAIL = 1.5   # im lặng cuối video
PEAK = 0.8   # chuẩn hóa đỉnh mỗi câu


@dataclass
class Frame:
    scene: int
    focus: Optional[int]
    revealed: frozenset
    speaker: Optional[str]
    text: str
    countdown: Optional[int] = None
    note: str = ""
    start: float = 0.0
    dur: float = 0.0


@dataclass
class Timeline:
    frames: list[Frame]
    audio: np.ndarray
    rate: int
    chapters: list[tuple[float, str]] = field(default_factory=list)
    scene_starts: list[float] = field(default_factory=list)

    @property
    def total(self) -> float:
        return len(self.audio) / self.rate


def _resample(x: np.ndarray, src: int, dst: int) -> np.ndarray:
    if src == dst or not len(x):
        return x
    n = int(round(len(x) * dst / src))
    return np.interp(np.linspace(0, len(x) - 1, n), np.arange(len(x)), x.astype(np.float32)).astype(np.int16)


def _norm(x: np.ndarray) -> np.ndarray:
    peak = int(np.abs(x.astype(np.int32)).max()) if len(x) else 0
    if peak == 0:
        return x
    return np.clip(x.astype(np.float32) * (PEAK * 32767 / peak), -32768, 32767).astype(np.int16)


def build(lesson: Lesson, engine, say: Callable[[str], None] = print, cache_dir: Optional[Path] = None) -> Timeline:
    """`cache_dir`: lưu tiếng từng câu theo (bộ đọc, giọng, chữ) — dựng lại chỉ đọc những câu đã đổi."""
    voices: dict[str, str] = {}
    for key, sp in lesson.speakers.items():
        try:
            voices[key] = sp.voice_for(engine.name)
        except ValueError as e:
            raise TTSError(str(e)) from None
        engine.check(voices[key])
    memo: dict[tuple[str, str], tuple[np.ndarray, int]] = {}

    def synth(text: str, voice: str) -> tuple[np.ndarray, int]:
        if (voice, text) in memo:
            return memo[(voice, text)]
        path = None
        if cache_dir is not None and engine.name != "silent":
            h = hashlib.sha1(f"{engine.tag}\n{voice}\n{text}".encode("utf-8")).hexdigest()
            path = Path(cache_dir) / h[:2] / f"{h}.npz"
            if path.exists():
                with np.load(path) as z:
                    memo[(voice, text)] = (z["data"], int(z["rate"]))
                return memo[(voice, text)]
        data, r = engine.synth(text, voice)
        if path is not None:
            path.parent.mkdir(parents=True, exist_ok=True)
            np.savez(path, data=data, rate=r)
        memo[(voice, text)] = (data, r)
        return data, r

    rate: Optional[int] = None
    parts: list[np.ndarray] = []
    frames: list[Frame] = []
    chapters: list[tuple[float, str]] = []
    scene_starts: list[float] = []
    pos = 0

    def silence(sec: float) -> np.ndarray:
        return np.zeros(int(round(sec * (rate or 16000))), dtype=np.int16)

    def ensure_rate(r: int) -> None:
        nonlocal rate, pos
        if rate is None:
            rate = r
            parts.append(silence(LEAD))
            pos = len(parts[0])

    def add(samples: np.ndarray, frame: Optional[Frame]):
        nonlocal pos
        parts.append(samples)
        if frame is not None:
            frame.start = pos / rate
            frame.dur = len(samples) / rate
            frames.append(frame)
        elif frames:
            frames[-1].dur += len(samples) / rate
        pos += len(samples)

    spoken = sum(1 for sc in lesson.scenes for ln in sc.lines if ln.speaker)
    done = 0
    for si, sc in enumerate(lesson.scenes):
        focus, revealed = None, set()
        if si and rate:
            add(silence(lesson.scene_gap), None)
        scene_starts.append(pos / rate if rate else 0.0)
        if sc.chapter:
            chapters.append((pos / rate if rate else 0.0, sc.chapter))
        for ln in sc.lines:
            if ln.focus is not None:
                focus = ln.focus
            revealed |= set(ln.reveal)
            if ln.speaker:
                data, r = synth(ln.spoken, voices[ln.speaker])
                ensure_rate(r)
                data = _norm(_resample(data, r, rate))
                fr = Frame(si, focus, frozenset(revealed), ln.speaker, ln.text)
                add(np.concatenate([data, silence(lesson.gap + ln.pause)]), fr)
                done += 1
                if done % 10 == 0 or done == spoken:
                    say(f"  đọc {done}/{spoken} câu")
            else:
                ensure_rate(getattr(engine, "RATE", 24000))
                left = ln.wait
                k = math.ceil(left)
                while left > 1e-6:
                    step = left - (k - 1) if k > 1 else left
                    add(silence(step), Frame(si, focus, frozenset(revealed), None, "", countdown=k, note=ln.note))
                    left -= step
                    k -= 1
                if ln.pause:
                    add(silence(ln.pause), None)
    if rate is None:
        raise ValueError("kịch bản không có câu nào")
    add(silence(TAIL), None)
    if frames:
        frames[0].dur += frames[0].start
        frames[0].start = 0.0
    audio = np.concatenate(parts)
    chapters = [(0.0 if i == 0 else t, name) for i, (t, name) in enumerate(chapters)]
    scene_starts[0] = 0.0
    return Timeline(frames, audio, rate, chapters, scene_starts)
