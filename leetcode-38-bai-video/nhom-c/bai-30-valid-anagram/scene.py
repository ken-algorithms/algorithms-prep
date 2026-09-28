"""Bài 30 — Valid Anagram (#242). Code hiển thị được đọc trực tiếp từ file nguồn Python/Java.

Python so 2 Counter; Java dùng 1 mảng int[26] cộng/trừ — hai phần minh hoạ riêng, đúng theo từng code.

    ./build.sh            # video đầy đủ (LessonVideo) + GIF (LessonGif)
"""

import sys
from contextlib import nullcontext
from pathlib import Path

from manim import (
    BOLD, DOWN, LEFT, RIGHT, UL, UP,
    Create, FadeIn, FadeOut, Indicate, Line, Rectangle, RoundedRectangle, Square,
    SurroundingRectangle, Text, Transform, TransformFromCopy, VGroup, Write,
)

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / "common"))
from kit import (  # noqa: E402
    ACCENT, BAD, CURRENT, FG, JAVA_DIR, MONO, MUTED, OK, PY_DIR, Lesson,
    highlight, java_solution, line_of, make_array, make_code, python_solution,
)

PY_FILE = PY_DIR / "lc242-valid-anagram.py"
JAVA_FILE = JAVA_DIR / "groupc" / "ValidAnagram.java"
LETTER = 0.62


def letter_row(word: str) -> VGroup:
    return make_array(list(word), side=LETTER, buff=0.1, indexed=False)


class Histogram(VGroup):
    """Counter vẽ bằng khối: mỗi lần đếm một ký tự = xếp thêm một khối lên cột của ký tự đó."""

    def __init__(self, keys: list[str], title: str, max_count: int, block: float = 0.42, gap: float = 0.85):
        super().__init__()
        self.keys, self.block, self.gap = keys, block, gap
        self.counts = {k: 0 for k in keys}
        width = gap * len(keys)
        self.base = Line(LEFT * width / 2, RIGHT * width / 2, color=MUTED, stroke_width=3)
        self.labels = VGroup(*[
            Text(k, font=MONO, font_size=26).move_to(self.base.get_left() + RIGHT * gap * (i + 0.5) + DOWN * 0.3)
            for i, k in enumerate(keys)
        ])
        self.title = Text(title, font=MONO, font_size=24, color=CURRENT)
        self.title.next_to(self.base, UP, buff=block * max_count + 0.75)
        self.add(self.base, self.labels, self.title)

    def slot(self, key: str):
        n = self.counts[key]
        label = self.labels[self.keys.index(key)]
        return [label.get_x(), self.base.get_y() + self.block * (n + 0.5) + 0.03 * n, 0]

    def count_label(self, key: str) -> Text:
        n = self.counts[key]
        label = Text(str(n), font=MONO, font_size=22, color=MUTED)
        return label.move_to(self.slot(key)).shift(DOWN * self.block * 0.25)


class SignedBars(VGroup):
    """Mảng letterCounts của Java: cột lên = dương (ký tự của s nhiều hơn), cột xuống = âm."""

    def __init__(self, keys: list[str], unit: float = 0.55, gap: float = 0.95):
        super().__init__()
        self.keys, self.unit = keys, unit
        self.values = {k: 0 for k in keys}
        width = gap * len(keys)
        self.base = Line(LEFT * width / 2, RIGHT * width / 2, color=MUTED, stroke_width=3)
        self.xs = [self.base.get_left()[0] + gap * (i + 0.5) for i in range(len(keys))]
        self.labels = VGroup(*[
            Text(k, font=MONO, font_size=26).move_to([x, self.base.get_y() - unit * 1.2 - 0.3, 0])
            for k, x in zip(keys, self.xs)
        ])
        self.bars = {k: self._bar(k) for k in keys}
        self.nums = {k: self._num(k) for k in keys}
        self.add(self.base, self.labels, *self.bars.values(), *self.nums.values())

    def _bar(self, k: str) -> Rectangle:
        v = self.values[k]
        h = max(abs(v), 0.04) * self.unit
        bar = Rectangle(width=0.5, height=h, stroke_width=0, fill_opacity=0.65,
                        fill_color=OK if v > 0 else BAD if v < 0 else MUTED)
        x = self.labels[self.keys.index(k)].get_x()  # vị trí hiện tại — nhóm có thể đã bị dời
        return bar.move_to([x, self.base.get_y() + (h / 2 if v >= 0 else -h / 2), 0])

    def _num(self, k: str) -> Text:
        v = self.values[k]
        label = self.labels[self.keys.index(k)]
        return Text(f"{v:+d}" if v else "0", font=MONO, font_size=22,
                    color=OK if v > 0 else BAD if v < 0 else MUTED).next_to(label, DOWN, buff=0.12)

    def change(self, k: str, delta: int):
        self.values[k] += delta
        return [Transform(self.bars[k], self._bar(k)), Transform(self.nums[k], self._num(k))]


class Base(Lesson):
    LESSON_DIR = HERE

    def count_word(self, row: VGroup, hist: Histogram, color: str, speed: float):
        for cell in row:
            block = Square(side_length=hist.block * 0.92, stroke_color=color, stroke_width=2)
            block.set_fill(color, opacity=0.35).move_to(hist.slot(cell.value))
            hist.counts[cell.value] += 1
            self.play(cell.box.animate.set_stroke(color), TransformFromCopy(cell.box, block), run_time=0.42 * speed)
            hist.add(block)

    def counter_walkthrough(self, s: str, t: str, voices: dict | None, fast: bool = False):
        """Đúng theo lc242: kiểm tra độ dài → Counter(s) → Counter(t) → so hai Counter."""
        speed = 0.5 if fast else 1.0
        say = (lambda k: self.voice(voices[k])) if voices else (lambda k: nullcontext())
        keys = sorted(set(s) | set(t))
        max_count = max(max(s.count(k), t.count(k)) for k in keys)

        s_row = letter_row(s).move_to(LEFT * 3.55 + UP * 2.25)
        t_row = letter_row(t).move_to(RIGHT * 3.55 + UP * 2.25)
        s_label = Text(f's = "{s}"', font=MONO, font_size=22, color=MUTED).next_to(s_row, UP, buff=0.15)
        t_label = Text(f't = "{t}"', font=MONO, font_size=22, color=MUTED).next_to(t_row, UP, buff=0.15)
        hs = Histogram(keys, "Counter(s)", max_count).move_to(LEFT * 3.55, coor_mask=[1, 0, 0])
        ht = Histogram(keys, "Counter(t)", max_count).move_to(RIGHT * 3.55, coor_mask=[1, 0, 0])
        for h in (hs, ht):
            h.shift(UP * (-2.0 - h.base.get_y()))
        self.play(FadeIn(s_row), FadeIn(t_row), FadeIn(s_label), FadeIn(t_label), run_time=0.6 * speed)

        with say("len"):
            ok = len(s) == len(t)
            check = Text(f"len(s) = {len(s)}   len(t) = {len(t)}   {'✓' if ok else '✗ → False'}",
                         font=MONO, font_size=26, color=OK if ok else BAD).move_to(UP * 1.2)
            self.play(FadeIn(check), run_time=0.5 * speed)
        self.play(FadeOut(check), FadeIn(hs), FadeIn(ht), run_time=0.5 * speed)

        with say("count_s"):
            self.count_word(s_row, hs, ACCENT, speed)
        with say("count_t"):
            self.count_word(t_row, ht, ACCENT, speed * (0.8 if not fast else 1))

        nums = VGroup(*[h.count_label(k) for h in (hs, ht) for k in keys])
        self.play(FadeIn(nums), run_time=0.3 * speed)

        verdict = None
        with say("compare"):
            lines = VGroup()
            all_equal = True
            for k in keys:
                a, b = s.count(k), t.count(k)
                eq = a == b
                all_equal &= eq
                lines.add(Text(f"{k}: {a} {'=' if eq else '≠'} {b}  {'✓' if eq else '✗'}",
                               font=MONO, font_size=24, color=OK if eq else BAD))
            lines.arrange(DOWN, buff=0.16, aligned_edge=LEFT).move_to(DOWN * 0.95)
            for k, line in zip(keys, lines):
                i = keys.index(k)
                self.play(
                    FadeIn(line, shift=RIGHT * 0.15),
                    Indicate(hs.labels[i], color=line.get_color(), scale_factor=1.4),
                    Indicate(ht.labels[i], color=line.get_color(), scale_factor=1.4),
                    run_time=0.45 * speed,
                )
            verdict = Text("return True" if all_equal else "return False", font=MONO, font_size=30,
                           weight=BOLD, color=OK if all_equal else BAD).move_to(UP * 1.15)
            self.play(FadeIn(verdict, scale=1.2), run_time=0.5 * speed)
        return VGroup(s_row, t_row, s_label, t_label, hs, ht, nums, lines, verdict)


class LessonVideo(Base):
    def construct(self):
        self.intro()
        header = Text("#242 · Valid Anagram", font_size=22, color=MUTED).to_corner(UL, buff=0.35)
        self.add(header)
        self.problem_and_sort()

        with self.voice("idea"):
            idea = VGroup(
                Text("Cách 2 — đếm tần suất", font_size=44, color=CURRENT, weight=BOLD),
                Text("Anagram ⇔ mọi ký tự có cùng số lần xuất hiện ở s và t", font_size=32),
                Text("Counter(s) == Counter(t)", font=MONO, font_size=32, color=ACCENT),
            ).arrange(DOWN, buff=0.4)
            self.play(FadeIn(idea, shift=UP * 0.3))
        self.play(FadeOut(idea), run_time=0.4)

        first = self.counter_walkthrough(
            "anagram", "nagaram", {"len": "len", "count_s": "count_s", "count_t": "count_t", "compare": "compare"})
        self.wait(0.6)
        self.play(FadeOut(first), run_time=0.5)
        with self.voice("ex2"):
            second = self.counter_walkthrough("rat", "car", None, fast=True)
        self.wait(0.8)
        self.play(FadeOut(second), run_time=0.5)

        self.code_python()
        self.java_counts()
        self.complexity_and_tradeoff()

    def intro(self):
        with self.voice("intro"):
            tag = Text("Bài 30 / 38 · Nhóm C — HashSet / Dictionary", font_size=26, color=ACCENT)
            title = Text("Valid Anagram", font_size=72, weight=BOLD)
            sub = Text("LeetCode #242 · Easy", font_size=32, color=MUTED)
            group = VGroup(tag, title, sub).arrange(DOWN, buff=0.35).shift(UP * 0.4)
            self.play(FadeIn(tag), Write(title), run_time=1.4)
            self.play(FadeIn(sub, shift=UP * 0.2), run_time=0.6)
        self.play(FadeOut(group), run_time=0.5)

    def problem_and_sort(self):
        with self.voice("problem"):
            statement = VGroup(
                Text("t có phải anagram của s?", font_size=40),
                Text("cùng ký tự · cùng số lần xuất hiện · chỉ khác thứ tự", font_size=28, color=MUTED),
            ).arrange(DOWN, buff=0.25).to_edge(UP, buff=0.9)
            self.play(FadeIn(statement, shift=DOWN * 0.2))

        s_row = letter_row("anagram").scale(1.25).move_to(UP * 0.35)
        t_row = letter_row("nagaram").scale(1.25).move_to(DOWN * 0.95)
        s_label = Text("s", font=MONO, font_size=30, color=MUTED).next_to(s_row, LEFT, buff=0.4)
        t_label = Text("t", font=MONO, font_size=30, color=MUTED).next_to(t_row, LEFT, buff=0.4)
        with self.voice("example"):
            self.play(FadeIn(s_row), FadeIn(t_row), FadeIn(s_label), FadeIn(t_label), run_time=0.8)
            answer = Text("→ true", font=MONO, font_size=34, color=OK).next_to(VGroup(s_row, t_row), RIGHT, buff=0.6)
            self.play(FadeIn(answer))

        with self.voice("naive"):
            self.play(FadeOut(answer), run_time=0.3)
            moves = []
            for row in (s_row, t_row):
                slots = [cell.get_center() for cell in row]
                order = sorted(range(len(row)), key=lambda i: (row[i].value, i))
                moves += [row[i].animate.move_to(slots[j]) for j, i in enumerate(order)]
            self.play(*moves, run_time=1.6)
            same = Text('sorted: "aaagmnr" == "aaagmnr"', font=MONO, font_size=28, color=OK)
            cost = Text("O(n log n)", font=MONO, font_size=32, color=CURRENT)
            VGroup(same, cost).arrange(DOWN, buff=0.25).next_to(t_row, DOWN, buff=0.45)
            self.play(FadeIn(same), FadeIn(cost))
        self.play(FadeOut(VGroup(statement, s_row, t_row, s_label, t_label, same, cost)), run_time=0.5)

    def code_python(self):
        src = python_solution(PY_FILE)
        py = make_code(src, "python", font_size=28)
        py.scale_to_fit_width(min(py.width, 10.5)).move_to(UP * 0.5)
        path = Text("leetcode-38-bai/lc242-valid-anagram.py", font=MONO, font_size=20, color=MUTED)
        path.next_to(py, DOWN, buff=0.2)
        with self.voice("py_intro"):
            self.play(FadeIn(py), FadeIn(path))
        ln_len = line_of(src, "if len(s) != len(t)")
        ln_counter = line_of(src, "Counter(s) == Counter(t)")
        cursor = highlight(py, ln_len, ln_len + 1)
        with self.voice("py_len"):
            self.play(FadeIn(cursor))
        with self.voice("py_counter"):
            self.play(cursor.animate.become(highlight(py, ln_counter)))
        self.wait(0.3)
        self.play(FadeOut(VGroup(py, path, cursor)), run_time=0.5)

    def java_counts(self):
        """Đúng theo ValidAnagram.java: một vòng lặp, s[i] → +1, t[i] → −1, rồi kiểm tra mọi ô = 0."""
        s, t = "anagram", "nagaram"
        src = java_solution(JAVA_FILE)
        java = make_code(src, "java", font_size=24)
        java.scale_to_fit_height(min(java.height, 5.5))
        if java.width > 6.5:
            java.scale_to_fit_width(6.5)
        java.move_to(RIGHT * 3.5 + UP * 0.4)
        path = Text("leetcode-38-bai-java/.../groupc/ValidAnagram.java", font=MONO, font_size=18, color=MUTED)
        path.next_to(java, DOWN, buff=0.15)
        ln = {
            "arr": line_of(src, "int[] letterCounts"),
            "inc": line_of(src, "s.charAt(i) - 'a']++"),
            "dec": line_of(src, "t.charAt(i) - 'a']--"),
            "check": line_of(src, "for (int count : letterCounts)"),
            "true": line_of(src, "return true;"),
        }

        s_row = letter_row(s).move_to(LEFT * 3.3 + UP * 2.35)
        t_row = letter_row(t).move_to(LEFT * 3.3 + UP * 1.4)
        s_label = Text("s", font=MONO, font_size=26, color=MUTED).next_to(s_row, LEFT, buff=0.3)
        t_label = Text("t", font=MONO, font_size=26, color=MUTED).next_to(t_row, LEFT, buff=0.3)
        bars = SignedBars(sorted(set(s))).move_to(LEFT * 3.3 + DOWN * 1.05)
        bars_title = Text("letterCounts (vẽ 5 / 26 ô đang dùng)", font=MONO, font_size=20, color=CURRENT)
        bars_title.next_to(bars.base, UP, buff=0.85)

        with self.voice("java_intro"):
            cursor = highlight(java, ln["arr"])
            self.play(FadeIn(java), FadeIn(path), run_time=0.8)
            self.play(FadeIn(cursor), FadeIn(VGroup(s_row, t_row, s_label, t_label)),
                      FadeIn(bars), FadeIn(bars_title), run_time=0.8)

        column = None
        with self.voice("java_loop"):
            for i in range(len(s)):
                frame = SurroundingRectangle(VGroup(s_row[i], t_row[i]), color=CURRENT, buff=0.08, stroke_width=3)
                self.play(
                    Create(frame) if column is None else Transform(column, frame),
                    cursor.animate.become(highlight(java, ln["inc"])),
                    *bars.change(s[i], +1), run_time=0.38,
                )
                column = column or frame
                self.play(cursor.animate.become(highlight(java, ln["dec"])), *bars.change(t[i], -1), run_time=0.38)

        with self.voice("java_zero"):
            self.play(FadeOut(column), cursor.animate.become(highlight(java, ln["check"], ln["check"] + 4)), run_time=0.5)
            self.play(*[Indicate(bars.nums[k], color=OK, scale_factor=1.5) for k in bars.keys], run_time=0.7)
            done = Text("mọi ô = 0 → return true", font=MONO, font_size=24, color=OK, weight=BOLD)
            done.move_to(bars_title)
            self.play(cursor.animate.become(highlight(java, ln["true"])), FadeOut(bars_title), FadeIn(done), run_time=0.5)

        with self.voice("java_unicode"):
            note = VGroup(
                Text("int[26] chỉ đúng với 'a'–'z'", font_size=28, color=CURRENT),
                Text('"café", "tiếng Việt" → sai', font=MONO, font_size=22, color=BAD),
                Text("→ dùng HashMap<Character, Integer>", font=MONO, font_size=22, color=OK),
            ).arrange(DOWN, buff=0.18)
            plate = RoundedRectangle(width=note.width + 0.6, height=note.height + 0.45, corner_radius=0.15,
                                     fill_color="#020617", fill_opacity=0.95, stroke_color=CURRENT, stroke_width=2)
            box = VGroup(plate, note.move_to(plate)).move_to(LEFT * 3.3 + DOWN * 0.95)
            if box.width > 6.4:
                box.scale_to_fit_width(6.4)
            self.play(FadeOut(VGroup(bars, done)), FadeIn(box, shift=UP * 0.2))
        self.wait(0.4)
        self.play(FadeOut(VGroup(java, path, cursor, s_row, t_row, s_label, t_label, box)), run_time=0.5)

    def complexity_and_tradeoff(self):
        with self.voice("complexity"):
            cards = VGroup()
            for label, value, note in (("Time", "O(n)", "duyệt mỗi chuỗi 1 lần"), ("Space", "O(1)", "tối đa 26 ký tự")):
                box = RoundedRectangle(width=4.2, height=2.5, corner_radius=0.25, stroke_color=ACCENT, stroke_width=3)
                content = VGroup(
                    Text(label, font_size=30, color=MUTED),
                    Text(value, font_size=60, color=FG, weight=BOLD),
                    Text(note, font_size=22, color=MUTED),
                ).arrange(DOWN, buff=0.2).move_to(box)
                cards.add(VGroup(box, content))
            cards.arrange(RIGHT, buff=0.8).move_to(UP * 0.3)
            self.play(FadeIn(cards, shift=UP * 0.3))
        self.play(FadeOut(cards), run_time=0.4)

        with self.voice("tradeoff"):
            rows = [
                ("Cách", "Time", "Dùng được cho", FG),
                ("Sort rồi so", "O(n log n)", "mọi ký tự", BAD),
                ("Counter / HashMap", "O(n)", "mọi ký tự (Unicode)", OK),
                ("int[26] (Java)", "O(n)", "chỉ 'a'–'z', nhanh nhất", CURRENT),
            ]
            table = VGroup()
            col_x = [-3.8, 0.4, 4.0]
            for r, (name, time, scope, color) in enumerate(rows):
                row = VGroup(Text(name, font_size=30, color=color), Text(time, font_size=30, color=color),
                             Text(scope, font_size=28, color=color))
                for c, item in enumerate(row):
                    item.move_to([col_x[c], 1.6 - r * 0.95, 0])
                table.add(row)
            rule = Rectangle(width=12, height=0.02, stroke_width=0, fill_color=MUTED, fill_opacity=0.6)
            rule.move_to([0.2, 1.6 - 0.48, 0])
            self.play(FadeIn(table[0]), FadeIn(rule), run_time=0.4)
            for row in table[1:]:
                self.play(FadeIn(row, shift=RIGHT * 0.2), run_time=0.5)
        self.play(FadeOut(VGroup(table, rule)), run_time=0.4)

        with self.voice("outro", pad=1.2):
            nxt = VGroup(
                Text("Tiếp theo", font_size=28, color=MUTED),
                Text("Bài 31 · Isomorphic Strings", font_size=52, weight=BOLD),
                Text("LeetCode #205 · HashMap Bijection", font_size=28, color=ACCENT),
            ).arrange(DOWN, buff=0.3)
            self.play(FadeIn(nxt, shift=UP * 0.2))


class LessonGif(Base):
    VOICE = False
    HOLD_WITHOUT_VOICE = 0.9

    def construct(self):
        title = Text("Valid Anagram · Counter", font_size=26, color=MUTED).to_corner(UL, buff=0.35)
        self.add(title)
        run = self.counter_walkthrough(
            "anagram", "nagaram", {"len": "len", "count_s": "count_s", "count_t": "count_t", "compare": "compare"})
        self.wait(1.5)
        self.play(FadeOut(run), run_time=0.4)
