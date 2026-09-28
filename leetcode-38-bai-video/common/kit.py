"""Phần dùng chung cho mọi video bài: màu, ô số, khung code, tô sáng dòng, phụ đề, giọng đọc.

Mỗi bài có `scene.py` định nghĩa `LessonVideo(Lesson)` và `LessonGif(Lesson)`, cùng thư mục với
`narration.py` (SEGMENTS) và `audio/` (do make_voice.py sinh ra).
"""

import json
import sys
import textwrap
from contextlib import contextmanager
from pathlib import Path

from manim import DOWN, RIGHT, Code, Rectangle, RoundedRectangle, Scene, Square, Text, VGroup, config

ALGO_ROOT = Path(__file__).resolve().parents[2]
PY_DIR = ALGO_ROOT / "leetcode-38-bai"
JAVA_DIR = ALGO_ROOT / "leetcode-38-bai-java" / "src" / "main" / "java" / "com" / "motives" / "leetcode"

BG, FG, MUTED = "#0f172a", "#e2e8f0", "#94a3b8"
ACCENT, CURRENT, OK, BAD = "#38bdf8", "#facc15", "#22c55e", "#ef4444"
FONT, MONO = "Arial", "Menlo"

config.background_color = BG
Text.set_default(font=FONT, color=FG)


# ---------- code nguồn: luôn đọc từ file thật ----------
def python_solution(py_file: Path) -> str:
    lines = py_file.read_text(encoding="utf-8").splitlines()
    start = next(i for i, l in enumerate(lines) if l.startswith("class Solution"))
    end = next(i for i, l in enumerate(lines) if l.startswith("if __name__"))
    return "\n".join(lines[start:end]).rstrip()


def python_body(py_file: Path, def_name: str) -> str:
    lines = python_solution(py_file).splitlines()
    start = next(i for i, l in enumerate(lines) if f"def {def_name}" in l) + 1
    return textwrap.dedent("\n".join(lines[start:]))


def java_solution(java_file: Path) -> str:
    lines = java_file.read_text(encoding="utf-8").splitlines()
    start = next(i for i, l in enumerate(lines) if l.startswith("public class"))
    return "\n".join(lines[start:]).rstrip()


def line_of(code_string: str, needle: str) -> int:
    return next(i for i, l in enumerate(code_string.splitlines()) if needle in l)


def make_code(code_string: str, language: str, font_size: int = 24) -> Code:
    return Code(
        code_string=code_string,
        language=language,
        formatter_style="monokai",
        background="window",
        paragraph_config={"font": MONO, "font_size": font_size},
    )


def highlight(code: Code, first: int, last: int | None = None, color: str = CURRENT) -> Rectangle:
    lines = code.code_lines
    top = lines[first].get_top()[1]
    bottom = lines[last if last is not None else first].get_bottom()[1]
    rect = Rectangle(
        width=code.background.width - 0.2,
        height=top - bottom + 0.14,
        fill_color=color, fill_opacity=0.22, stroke_width=0,
    )
    return rect.move_to([code.background.get_center()[0], (top + bottom) / 2, 0])


# ---------- hình khối ----------
class Cell(VGroup):
    def __init__(self, value, index: int | None = None, side: float = 1.1):
        super().__init__()
        self.value = value
        self.box = Square(side_length=side, stroke_color=ACCENT, stroke_width=3)
        self.box.set_fill(BG, opacity=1)
        self.label = Text(str(value), font_size=int(side * 36)).move_to(self.box)
        self.add(self.box, self.label)
        if index is not None:
            self.index = Text(str(index), font_size=20 if side >= 1 else int(side * 20), color=MUTED)
            self.index.next_to(self.box, DOWN, buff=0.15)
            self.add(self.index)


def make_array(values, side: float = 1.1, buff: float = 0.25, indexed: bool = True) -> VGroup:
    return VGroup(*[Cell(v, i if indexed else None, side) for i, v in enumerate(values)]).arrange(RIGHT, buff=buff)


def wrap(caption: str, width: int = 62) -> str:
    return "\n".join(textwrap.wrap(caption, width))


# ---------- scene nền: giọng đọc + phụ đề ----------
class Lesson(Scene):
    """Lớp con đặt LESSON_DIR = thư mục bài (chứa narration.py và audio/)."""

    LESSON_DIR: Path
    VOICE = True
    CAPTIONS = True
    HOLD_WITHOUT_VOICE = 0.7

    def setup(self):
        sys.path.insert(0, str(self.LESSON_DIR))
        from narration import SEGMENTS

        self.segments = SEGMENTS
        self.audio_dir = self.LESSON_DIR / "audio"
        durations_file = self.audio_dir / "durations.json"
        self.durations = json.loads(durations_file.read_text()) if durations_file.exists() else {}
        self.caption = None

    @contextmanager
    def voice(self, key: str, pad: float = 0.35):
        """Phát đoạn giọng `key`, hiện phụ đề, và giữ cảnh cho tới khi đọc xong."""
        start = self.renderer.time
        wav = self.audio_dir / f"{key}.wav"
        if self.VOICE and wav.exists():
            self.add_sound(str(wav))
        if self.CAPTIONS:
            self.set_caption(self.segments[key][0])
        yield
        target = self.durations.get(key, 0) + pad if self.VOICE else self.HOLD_WITHOUT_VOICE
        rest = target - (self.renderer.time - start)
        if rest > 0.05:
            self.wait(rest)

    def set_caption(self, text: str):
        if self.caption is not None:
            self.remove(self.caption)
        label = Text(wrap(text), font_size=26, line_spacing=0.8)
        plate = RoundedRectangle(
            width=label.width + 0.6, height=label.height + 0.35, corner_radius=0.12,
            fill_color="#020617", fill_opacity=0.85, stroke_width=0,
        )
        self.caption = VGroup(plate, label.move_to(plate)).to_edge(DOWN, buff=0.25)
        self.add(self.caption)
