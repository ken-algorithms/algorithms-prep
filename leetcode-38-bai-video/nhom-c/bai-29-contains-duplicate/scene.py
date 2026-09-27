"""Bài 29 — Contains Duplicate (#217). Code hiển thị được đọc trực tiếp từ file nguồn Python/Java.

    manim render -r 1920,1080 --fps 30 scene.py ContainsDuplicateVideo   # video đầy đủ có tiếng
    manim render -r 960,540  --fps 24 scene.py ContainsDuplicateGif      # đoạn walkthrough để làm GIF
"""

import json
import sys
import textwrap
from contextlib import contextmanager, nullcontext
from pathlib import Path

from manim import (
    BOLD, DOWN, LEFT, ORIGIN, PI, RIGHT, UL, UP,
    Arrow, ArcBetweenPoints, Code, Create, FadeIn, FadeOut, Indicate, Rectangle,
    RoundedRectangle, Scene, Square, Text, TransformFromCopy, VGroup, Write, config,
)

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from narration import SEGMENTS  # noqa: E402

ALGO_ROOT = HERE.parents[2]
PY_FILE = ALGO_ROOT / "leetcode-38-bai" / "lc217-contains-duplicate.py"
JAVA_FILE = (
    ALGO_ROOT / "leetcode-38-bai-java" / "src" / "main" / "java"
    / "com" / "motives" / "leetcode" / "groupc" / "ContainsDuplicate.java"
)
AUDIO = HERE / "audio"
DURATIONS_FILE = AUDIO / "durations.json"
DURATIONS = json.loads(DURATIONS_FILE.read_text()) if DURATIONS_FILE.exists() else {}

BG, FG, MUTED = "#0f172a", "#e2e8f0", "#94a3b8"
ACCENT, CURRENT, OK, BAD = "#38bdf8", "#facc15", "#22c55e", "#ef4444"
FONT, MONO = "Arial", "Menlo"

config.background_color = BG
Text.set_default(font=FONT, color=FG)


def python_solution() -> str:
    lines = PY_FILE.read_text(encoding="utf-8").splitlines()
    start = next(i for i, l in enumerate(lines) if l.startswith("class Solution"))
    end = next(i for i, l in enumerate(lines) if l.startswith("if __name__"))
    return "\n".join(lines[start:end]).rstrip()


def python_body() -> str:
    lines = python_solution().splitlines()
    start = next(i for i, l in enumerate(lines) if "def contains_duplicate" in l) + 1
    return textwrap.dedent("\n".join(lines[start:]))


def java_solution() -> str:
    lines = JAVA_FILE.read_text(encoding="utf-8").splitlines()
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


def wrap(caption: str, width: int = 62) -> str:
    return "\n".join(textwrap.wrap(caption, width))


class Cell(VGroup):
    def __init__(self, value: int, index: int | None = None, side: float = 1.1):
        super().__init__()
        self.value = value
        self.box = Square(side_length=side, stroke_color=ACCENT, stroke_width=3)
        self.box.set_fill(BG, opacity=1)
        self.label = Text(str(value), font_size=int(side * 36)).move_to(self.box)
        self.add(self.box, self.label)
        if index is not None:
            self.index = Text(str(index), font_size=20, color=MUTED).next_to(self.box, DOWN, buff=0.15)
            self.add(self.index)


def make_array(nums: list[int]) -> VGroup:
    return VGroup(*[Cell(n, i) for i, n in enumerate(nums)]).arrange(RIGHT, buff=0.25)


class Base(Scene):
    VOICE = True
    CAPTIONS = True
    HOLD_WITHOUT_VOICE = 0.7

    def setup(self):
        self.caption = None

    @contextmanager
    def voice(self, key: str, pad: float = 0.35):
        start = self.renderer.time
        caption_text, _ = SEGMENTS[key]
        if self.VOICE and (AUDIO / f"{key}.wav").exists():
            self.add_sound(str(AUDIO / f"{key}.wav"))
        if self.CAPTIONS:
            self.set_caption(caption_text)
        yield
        target = DURATIONS.get(key, 0) + pad if self.VOICE else self.HOLD_WITHOUT_VOICE
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

    def walkthrough(self, nums: list[int], step_keys: list[str] | None, fast: bool = False):
        """Chạy đúng vòng lặp của Solution.contains_duplicate, vẽ từng bước."""
        speed = 0.45 if fast else 1.0
        array = make_array(nums).scale(0.9).move_to(LEFT * 3.4 + UP * 1.35)

        body = python_body()
        code = make_code(body, "python", font_size=26)
        code.scale_to_fit_width(min(code.width, 5.8)).move_to(RIGHT * 3.55 + UP * 0.45)
        code_title = Text("Python", font_size=20, color=MUTED).next_to(code, UP, buff=0.15).align_to(code, LEFT)
        ln = {
            "seen": line_of(body, "seen = set()"),
            "for": line_of(body, "for num in"),
            "if": line_of(body, "if num in seen"),
            "true": line_of(body, "return True"),
            "add": line_of(body, "seen.add"),
            "false": line_of(body, "return False"),
        }

        seen_box = RoundedRectangle(width=5.4, height=1.45, corner_radius=0.2, stroke_color=CURRENT, stroke_width=3)
        seen_box.move_to(LEFT * 3.4 + DOWN * 1.35)
        seen_label = Text("seen (HashSet)", font_size=22, color=CURRENT).next_to(seen_box, UP, buff=0.12).align_to(seen_box, LEFT)

        cursor = highlight(code, ln["seen"])
        self.play(
            FadeIn(array), FadeIn(code), FadeIn(code_title),
            Create(seen_box), FadeIn(seen_label), FadeIn(cursor),
            run_time=1.0 * speed,
        )

        def move_cursor(key: str):
            return cursor.animate.become(highlight(code, ln[key]))

        pointer = Arrow(UP * 0.8, ORIGIN, buff=0, color=CURRENT, stroke_width=6)
        pointer.next_to(array[0].box, UP, buff=0.1)
        self.play(FadeIn(pointer), run_time=0.3 * speed)

        seen: set[int] = set()
        slots: dict[int, VGroup] = {}
        query = None
        result = None
        for i, num in enumerate(nums):
            cell = array[i]
            with self.voice(step_keys[i]) if step_keys else nullcontext():
                anims = [pointer.animate.next_to(cell.box, UP, buff=0.1), move_cursor("for")]
                if query is not None:
                    anims.append(FadeOut(query))
                self.play(*anims, run_time=0.45 * speed)
                query = Text(f"{num} ∈ seen ?", font_size=26, color=FG)
                query.next_to(seen_box, UP, buff=0.12).align_to(seen_box, RIGHT)
                self.play(FadeIn(query), move_cursor("if"), cell.box.animate.set_stroke(CURRENT), run_time=0.45 * speed)

                if num in seen:
                    hit = slots[num]
                    self.play(Indicate(hit, color=BAD, scale_factor=1.3), run_time=0.6 * speed)
                    result = Text("return True", font=MONO, font_size=30, color=BAD, weight=BOLD)
                    result.move_to(query).align_to(seen_box, RIGHT)
                    self.play(
                        cell.box.animate.set_fill(BAD, opacity=0.45).set_stroke(BAD),
                        hit[0].animate.set_fill(BAD, opacity=0.45).set_stroke(BAD),
                        move_cursor("true"),
                        FadeOut(query), FadeIn(result),
                        run_time=0.6 * speed,
                    )
                    query = None
                    break

                seen.add(num)
                slot_box = Square(side_length=0.8, stroke_color=OK, stroke_width=3).set_fill(OK, opacity=0.2)
                slot_box.move_to(seen_box.get_left() + RIGHT * (0.65 + 1.0 * (len(seen) - 1)))
                slot_label = Text(str(num), font_size=28).move_to(slot_box)
                slots[num] = VGroup(slot_box, slot_label)
                self.play(
                    move_cursor("add"),
                    cell.box.animate.set_fill(OK, opacity=0.25).set_stroke(OK),
                    Create(slot_box),
                    TransformFromCopy(cell.label, slot_label),
                    run_time=0.6 * speed,
                )
        else:
            if query is not None:
                self.play(FadeOut(query), run_time=0.3 * speed)
            result = Text("return False", font=MONO, font_size=30, color=ACCENT, weight=BOLD)
            result.next_to(seen_box, UP, buff=0.12).align_to(seen_box, RIGHT)
            self.play(move_cursor("false"), FadeOut(pointer), FadeIn(result), run_time=0.6 * speed)
            pointer = None

        leftovers = [array, code, code_title, seen_box, seen_label, cursor, result, *slots.values()]
        if pointer is not None:
            leftovers.append(pointer)
        return VGroup(*leftovers)


class ContainsDuplicateVideo(Base):
    def construct(self):
        self.intro()
        header = Text("#217 · Contains Duplicate", font_size=22, color=MUTED).to_corner(UL, buff=0.35)
        self.add(header)
        self.problem_and_brute_force()
        with self.voice("idea"):
            idea = VGroup(
                Text("Ý tưởng", font_size=44, color=CURRENT, weight=BOLD),
                Text("Lưu số đã gặp vào HashSet → hỏi \"đã có chưa?\" trong O(1)", font_size=32),
            ).arrange(DOWN, buff=0.4)
            self.play(FadeIn(idea, shift=UP * 0.3))
        self.play(FadeOut(idea), run_time=0.4)

        first = self.walkthrough([1, 2, 3, 1], ["step0", "step1", "step2", "step3"])
        self.wait(0.8)
        self.play(FadeOut(first), run_time=0.5)
        with self.voice("ex2"):
            second = self.walkthrough([1, 2, 3, 4], None, fast=True)
        self.wait(0.8)
        self.play(FadeOut(second), run_time=0.5)

        self.code_python_and_java()
        self.complexity_and_tradeoff()

    def intro(self):
        with self.voice("intro"):
            tag = Text("Bài 29 / 38 · Nhóm C — HashSet / Dictionary", font_size=26, color=ACCENT)
            title = Text("Contains Duplicate", font_size=72, weight=BOLD)
            sub = Text("LeetCode #217 · Easy", font_size=32, color=MUTED)
            group = VGroup(tag, title, sub).arrange(DOWN, buff=0.35).shift(UP * 0.4)
            self.play(FadeIn(tag), Write(title), run_time=1.4)
            self.play(FadeIn(sub, shift=UP * 0.2), run_time=0.6)
        self.play(FadeOut(group), run_time=0.5)

    def problem_and_brute_force(self):
        with self.voice("problem"):
            statement = Text("nums có phần tử nào xuất hiện ≥ 2 lần không?", font_size=38)
            statement.to_edge(UP, buff=1.0)
            self.play(FadeIn(statement, shift=DOWN * 0.2))

        array = make_array([1, 2, 3, 1]).move_to(DOWN * 0.1)
        with self.voice("example"):
            self.play(FadeIn(array, shift=UP * 0.2), run_time=0.8)
            answer = Text("→ true", font=MONO, font_size=36, color=OK).next_to(array, RIGHT, buff=0.6)
            self.play(
                array[0].box.animate.set_stroke(CURRENT, width=6),
                array[3].box.animate.set_stroke(CURRENT, width=6),
                FadeIn(answer),
            )
        self.play(
            array[0].box.animate.set_stroke(ACCENT, width=3),
            array[3].box.animate.set_stroke(ACCENT, width=3),
            FadeOut(answer), run_time=0.4,
        )

        counter_label = Text("số phép so sánh: 0", font_size=28, color=MUTED).next_to(array, DOWN, buff=0.7)
        arcs = VGroup()
        with self.voice("brute"):
            self.play(FadeIn(counter_label), run_time=0.3)
            count = 0
            for i in range(4):
                for j in range(i + 1, 4):
                    count += 1
                    is_match = array[i].value == array[j].value
                    arc = ArcBetweenPoints(
                        array[i].box.get_top() + UP * 0.08, array[j].box.get_top() + UP * 0.08,
                        angle=-PI * 0.7, color=BAD if is_match else MUTED, stroke_width=5 if is_match else 3,
                    )
                    arcs.add(arc)
                    new_label = Text(f"số phép so sánh: {count}", font_size=28, color=MUTED).move_to(counter_label)
                    self.play(Create(arc), counter_label.animate.become(new_label), run_time=0.55)
            formula = Text("n(n−1)/2  →  O(n²)", font=MONO, font_size=34, color=CURRENT)
            formula.next_to(counter_label, DOWN, buff=0.35)
            self.play(FadeIn(formula))
        with self.voice("brute_bad"):
            scale = Text("n = 100.000  →  ≈ 5.000.000.000 phép so sánh", font_size=32, color=BAD)
            scale.move_to(formula)
            self.play(formula.animate.become(scale))
        self.play(FadeOut(VGroup(statement, array, arcs, counter_label, formula)), run_time=0.5)

    def code_python_and_java(self):
        py_src = python_solution()
        java_src = java_solution()

        py = make_code(py_src, "python", font_size=26)
        py.scale_to_fit_width(min(py.width, 11)).move_to(UP * 0.35)
        py_path = Text("leetcode-38-bai/lc217-contains-duplicate.py", font=MONO, font_size=20, color=MUTED)
        py_path.next_to(py, DOWN, buff=0.2)
        with self.voice("py_intro"):
            self.play(FadeIn(py), FadeIn(py_path))

        py_ln = {k: line_of(py_src, n) for k, n in {
            "seen": "seen = set()", "for": "for num in", "if": "if num in seen", "true": "return True",
            "add": "seen.add", "false": "return False",
        }.items()}
        cursor = highlight(py, py_ln["seen"])
        with self.voice("py_seen"):
            self.play(FadeIn(cursor))
        with self.voice("py_loop"):
            self.play(cursor.animate.become(highlight(py, py_ln["for"], py_ln["true"])))
        with self.voice("py_add"):
            self.play(cursor.animate.become(highlight(py, py_ln["add"])))
        with self.voice("py_false"):
            self.play(cursor.animate.become(highlight(py, py_ln["false"])))

        java = make_code(java_src, "java", font_size=26)
        java_path = Text("leetcode-38-bai-java/.../groupc/ContainsDuplicate.java", font=MONO, font_size=20, color=MUTED)
        with self.voice("java_intro"):
            self.play(FadeOut(cursor), FadeOut(py_path), run_time=0.3)
            self.play(py.animate.scale_to_fit_width(6.75).move_to(LEFT * 3.5 + UP * 0.55), run_time=0.8)
            java.scale_to_fit_width(6.75).move_to(RIGHT * 3.5 + UP * 0.55)
            java_path.scale(0.85).next_to(java, DOWN, buff=0.2)
            py_path.scale(0.85).next_to(py, DOWN, buff=0.2)
            self.play(FadeIn(java), FadeIn(java_path), FadeIn(py_path), run_time=0.8)

        java_ln = {k: line_of(java_src, n) for k, n in {
            "seen": "new HashSet", "add": "!seen.add(num)", "true": "return true",
        }.items()}
        with self.voice("java_seen"):
            a, b = highlight(py, py_ln["seen"]), highlight(java, java_ln["seen"])
            self.play(FadeIn(a), FadeIn(b))
        with self.voice("java_add"):
            self.play(
                a.animate.become(highlight(py, py_ln["if"], py_ln["add"], color=ACCENT)),
                b.animate.become(highlight(java, java_ln["add"], java_ln["true"], color=ACCENT)),
            )
        self.wait(0.5)
        self.play(FadeOut(VGroup(py, java, py_path, java_path, a, b)), run_time=0.5)

    def complexity_and_tradeoff(self):
        with self.voice("complexity"):
            cards = VGroup()
            for label, value in (("Time", "O(n)"), ("Space", "O(n)")):
                box = RoundedRectangle(width=4, height=2.2, corner_radius=0.25, stroke_color=ACCENT, stroke_width=3)
                content = VGroup(
                    Text(label, font_size=30, color=MUTED),
                    Text(value, font_size=60, color=FG, weight=BOLD),
                ).arrange(DOWN, buff=0.25).move_to(box)
                cards.add(VGroup(box, content))
            cards.arrange(RIGHT, buff=0.8).move_to(UP * 0.3)
            self.play(FadeIn(cards, shift=UP * 0.3))
        self.play(FadeOut(cards), run_time=0.4)

        with self.voice("tradeoff"):
            rows = [
                ("Cách", "Time", "Bộ nhớ thêm", FG),
                ("So từng cặp", "O(n²)", "O(1)", BAD),
                ("Sort + so kề nhau", "O(n log n)", "O(1) – O(n), tuỳ sort", CURRENT),
                ("HashSet (bài này)", "O(n)", "O(n)", OK),
            ]
            table = VGroup()
            for name, time, space, color in rows:
                row = VGroup(
                    Text(name, font_size=30, color=color),
                    Text(time, font_size=30, color=color),
                    Text(space, font_size=30, color=color),
                )
                table.add(row)
            col_x = [-3.8, 0.4, 4.0]
            for r, row in enumerate(table):
                for c, item in enumerate(row):
                    item.move_to([col_x[c], 1.6 - r * 0.95, 0])
            rule = Rectangle(width=11.5, height=0.02, stroke_width=0, fill_color=MUTED, fill_opacity=0.6)
            rule.move_to([0.3, 1.6 - 0.48, 0])
            self.play(FadeIn(table[0]), FadeIn(rule), run_time=0.4)
            for row in table[1:]:
                self.play(FadeIn(row, shift=RIGHT * 0.2), run_time=0.5)
        self.play(FadeOut(VGroup(table, rule)), run_time=0.4)

        with self.voice("outro", pad=1.2):
            nxt = VGroup(
                Text("Tiếp theo", font_size=28, color=MUTED),
                Text("Bài 30 · Valid Anagram", font_size=52, weight=BOLD),
                Text("LeetCode #242 · HashMap Counting", font_size=28, color=ACCENT),
            ).arrange(DOWN, buff=0.3)
            self.play(FadeIn(nxt, shift=UP * 0.2))


class ContainsDuplicateGif(Base):
    VOICE = False
    HOLD_WITHOUT_VOICE = 0.9

    def construct(self):
        title = Text("Contains Duplicate · HashSet", font_size=26, color=MUTED).to_corner(UL, buff=0.35)
        self.add(title)
        run = self.walkthrough([1, 2, 3, 1], ["step0", "step1", "step2", "step3"])
        self.wait(1.5)
        self.play(FadeOut(run), run_time=0.4)
