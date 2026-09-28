"""Bài 37 — Continuous Subarray Sum (#523). Code hiển thị được đọc trực tiếp từ file nguồn Python/Java.

Trọng tâm: 2 tiền tố cùng số dư % k → đoạn giữa chia hết cho k; map lưu chỉ số ĐẦU TIÊN; {0: −1}; độ dài ≥ 2.

    ./build.sh            # video đầy đủ (LessonVideo) + GIF (LessonGif)
"""

import sys
from contextlib import nullcontext
from pathlib import Path

from manim import (
    BOLD, DOWN, LEFT, RIGHT, UL, UP,
    Create, FadeIn, FadeOut, Indicate, Line, Rectangle, RoundedRectangle,
    SurroundingRectangle, Text, Transform, VGroup, Write,
)

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / "common"))
from kit import (  # noqa: E402
    ACCENT, BAD, CURRENT, FG, JAVA_DIR, MONO, MUTED, OK, PY_DIR, Cell, Lesson,
    highlight, java_solution, line_of, make_array, make_code, python_solution,
)

PY_FILE = PY_DIR / "lc523-continuous-subarray-sum.py"
JAVA_FILE = JAVA_DIR / "groupc" / "ContinuousSubarraySum.java"
EXAMPLE, WALK, K = [23, 2, 4, 6, 7], [5, 6, 1, 3], 6


def span(row: VGroup, first: int, last: int, color: str = OK, level: int = 0) -> Line:
    y = row.get_top()[1] + 0.2 + 0.2 * level
    return Line([row[first].get_left()[0] + 0.06, y, 0], [row[last].get_right()[0] - 0.06, y, 0],
                color=color, stroke_width=7)


def under_row(row: VGroup, values: list, ghost, y: float, color: str, side: float = 0.66) -> VGroup:
    """Một hàng ô nằm thẳng dưới các ô của `row`; ô đầu tiên (vị trí −1) nằm bên trái, dùng cho giá trị khởi tạo."""
    step = row[1].get_x() - row[0].get_x()
    cells = VGroup()
    for j, v in enumerate([ghost] + values):
        cell = Cell(v, side=side)
        cell.box.set_stroke(color, width=2)
        x = row[0].get_x() - step if j == 0 else row[j - 1].get_x()
        cells.add(cell.move_to([x, y, 0]))
    return cells


class IndexMap(VGroup):
    """remainder_index vẽ thành bảng 'số dư → chỉ số đầu tiên'."""

    def __init__(self, title: str, width: float = 2.6, height: float = 2.3):
        super().__init__()
        self.box = RoundedRectangle(width=width, height=height, corner_radius=0.18, stroke_color=CURRENT, stroke_width=3)
        self.title = Text(title, font=MONO, font_size=22, color=CURRENT).next_to(self.box, UP, buff=0.12)
        self.rows: dict[int, Text] = {}
        self.add(self.box, self.title)

    def put(self, key: int, index: int):
        text = Text(f"{key} → {index}", font=MONO, font_size=28)
        text.move_to(self.box.get_top() + DOWN * (0.4 + 0.46 * len(self.rows)))
        self.rows[key] = text
        self.add(text)
        return FadeIn(text, shift=DOWN * 0.15)


class Base(Lesson):
    LESSON_DIR = HERE

    def remainder_walkthrough(self, nums: list[int], k: int, voiced: bool):
        """Chạy đúng Solution.check_subarray_sum, vẽ từng bước."""
        say = (lambda i: self.voice(f"w{i}")) if voiced else (lambda i: nullcontext())
        row = make_array(nums, side=0.8, buff=0.12).move_to(LEFT * 3.0 + UP * 1.95)
        row_lab = Text("nums", font=MONO, font_size=20, color=MUTED).next_to(row, LEFT, buff=0.95)
        prefixes, p = [], 0
        for x in nums:
            p += x
            prefixes.append(p)
        p_row = under_row(row, prefixes, 0, 0.75, MUTED)
        r_row = under_row(row, [v % k for v in prefixes], 0, -0.15, CURRENT)
        p_lab = Text("P", font=MONO, font_size=22, color=MUTED).next_to(p_row[0], LEFT, buff=0.2)
        r_lab = Text(f"P%{k}", font=MONO, font_size=22, color=CURRENT).next_to(r_row[0], LEFT, buff=0.2)
        ghost_i = Text("i = −1", font=MONO, font_size=18, color=MUTED).next_to(r_row[0], DOWN, buff=0.12)
        for cell in [*p_row[1:], *r_row[1:]]:
            cell.set_opacity(0)

        rmap = IndexMap("remainder_index").move_to(RIGHT * 2.3 + UP * 0.75)
        status = Text("·", font_size=26).set_opacity(0).move_to(RIGHT * 2.3 + DOWN * 1.0)
        running = Text("·", font=MONO, font_size=24).set_opacity(0).move_to(DOWN * 1.9)
        self.play(FadeIn(VGroup(row, row_lab, p_row[0], r_row[0], p_lab, r_lab, ghost_i, rmap)), run_time=0.8)
        self.play(rmap.put(0, -1), run_time=0.4)
        self.add(status, running)
        index = {0: -1}

        def show_line(code: str):
            return Transform(running, Text(f"▶ {code}", font=MONO, font_size=24, color=CURRENT).move_to(running))

        def say_status(text: str, color: str):
            new = Text(text, font_size=26, color=color).move_to(status)
            if new.width > 6.4:
                new.scale_to_fit_width(6.4)
            return Transform(status, new)

        frame, verdict_line = None, None
        for i, num in enumerate(nums):
            r = prefixes[i] % k
            with say(i):
                box = SurroundingRectangle(VGroup(row[i], r_row[i + 1]), color=CURRENT, buff=0.06, stroke_width=4)
                self.play(FadeIn(box) if frame is None else Transform(frame, box),
                          p_row[i + 1].animate.set_opacity(1), r_row[i + 1].animate.set_opacity(1),
                          show_line("remainder = prefix_sum % k"), run_time=0.5)
                frame = frame or box
                if r in index:
                    first = index[r]
                    gap = i - first
                    self.play(show_line("if i - remainder_index[remainder] > 1:"),
                              Indicate(rmap.rows[r], color=CURRENT, scale_factor=1.3),
                              Indicate(r_row[first + 1], color=CURRENT, scale_factor=1.3), run_time=0.55)
                    if gap > 1:
                        line = span(row, first + 1, i)
                        seg = nums[first + 1:i + 1]
                        self.play(Create(line), say_status(f"{i} − ({first}) = {gap} > 1 → {seg} tổng {sum(seg)}", OK),
                                  run_time=0.6)
                        verdict_line = line
                        verdict = Text("return True", font=MONO, font_size=30, color=OK, weight=BOLD)
                        self.play(Transform(status, verdict.move_to(status)), show_line("return True"), run_time=0.5)
                        break
                    self.play(say_status(f"{i} − {first} = {gap} → đoạn dài 1 ✗ · không ghi đè", BAD),
                              Indicate(row[i], color=BAD, scale_factor=1.2), run_time=0.6)
                else:
                    index[r] = i
                    self.play(show_line("remainder_index[remainder] = i"), rmap.put(r, i),
                              say_status(f"dư {r} chưa có → ghi {r} → {i}", FG), run_time=0.55)
        else:
            verdict = Text("return False", font=MONO, font_size=30, color=BAD, weight=BOLD)
            self.play(Transform(status, verdict.move_to(status)), show_line("return False"), run_time=0.5)
        parts = [row, row_lab, p_row, r_row, p_lab, r_lab, ghost_i, rmap, status, running, frame]
        return VGroup(*parts, *([verdict_line] if verdict_line else []))


class LessonVideo(Base):
    def construct(self):
        self.intro()
        header = Text("#523 · Continuous Subarray Sum", font_size=22, color=MUTED).to_corner(UL, buff=0.35)
        self.add(header)
        self.problem_and_idea()

        run = self.remainder_walkthrough(WALK, K, voiced=True)
        self.wait(0.8)
        self.play(FadeOut(run), run_time=0.5)

        self.overwrite_trap()
        self.code_python()
        self.code_java()
        self.compare_and_complexity()

    def intro(self):
        with self.voice("intro"):
            tag = Text("Bài 37 / 38 · Nhóm C — HashSet / Dictionary", font_size=26, color=ACCENT)
            title = Text("Continuous Subarray Sum", font_size=66, weight=BOLD)
            sub = Text("LeetCode #523 · Medium", font_size=32, color=MUTED)
            group = VGroup(tag, title, sub).arrange(DOWN, buff=0.35).shift(UP * 0.4)
            self.play(FadeIn(tag), Write(title), run_time=1.4)
            self.play(FadeIn(sub, shift=UP * 0.2), run_time=0.6)
        self.play(FadeOut(group), run_time=0.5)

    def problem_and_idea(self):
        with self.voice("problem"):
            statement = VGroup(
                Text("Có đoạn con liên tiếp DÀI ≥ 2, tổng là bội của k?", font_size=38),
                Text("bội của k = chia hết cho k (kể cả 0)", font_size=26, color=MUTED),
            ).arrange(DOWN, buff=0.25).to_edge(UP, buff=0.9)
            self.play(FadeIn(statement, shift=DOWN * 0.2))
        row = make_array(EXAMPLE, side=0.95, buff=0.14).move_to(LEFT * 1.4 + UP * 0.75)
        k_lab = Text(f"k = {K}", font=MONO, font_size=30, color=CURRENT).next_to(row, RIGHT, buff=0.6)
        with self.voice("example"):
            self.play(FadeIn(row), FadeIn(k_lab), run_time=0.6)
            hit = span(row, 1, 2)
            ok = Text("[2, 4] → 6 = 1 · 6 → true", font=MONO, font_size=28, color=OK).next_to(row, DOWN, buff=0.6)
            self.play(Create(hit), FadeIn(ok))
        with self.voice("brute"):
            cost = Text("thử mọi đoạn dài ≥ 2 → O(n²)", font=MONO, font_size=28, color=BAD).move_to(ok)
            self.play(Transform(ok, cost))

        prefixes, p = [], 0
        for x in EXAMPLE:
            p += x
            prefixes.append(p)
        p_row = under_row(row, prefixes, 0, -0.5, MUTED, side=0.8)
        r_row = under_row(row, [v % K for v in prefixes], 0, -1.35, CURRENT, side=0.8)
        p_lab = Text("P", font=MONO, font_size=24, color=MUTED).next_to(p_row[0], LEFT, buff=0.2)
        r_lab = Text(f"P%{K}", font=MONO, font_size=24, color=CURRENT).next_to(r_row[0], LEFT, buff=0.2)
        with self.voice("idea"):
            self.play(FadeOut(ok), FadeIn(VGroup(p_row, r_row, p_lab, r_lab)), run_time=0.8)
            pair = [1, 3]                      # P = 23 (sau phần tử 0) và 29 (sau phần tử 2), cùng dư 5
            self.play(*[r_row[j].box.animate.set_stroke(OK, width=6) for j in pair],
                      *[p_row[j].box.animate.set_stroke(OK, width=6) for j in pair], run_time=0.6)
            eq = Text("29 − 23 = 6 = 2 + 4  → chia hết cho 6", font=MONO, font_size=26, color=OK)
            eq.next_to(r_row, DOWN, buff=0.25)
            self.play(FadeIn(eq))
        with self.voice("idea2"):
            card = VGroup(
                Text("remainder_index", font=MONO, font_size=28, color=CURRENT),
                Text("số dư → chỉ số ĐẦU TIÊN gặp", font_size=26),
                Text("khởi tạo {0: −1}", font=MONO, font_size=26, color=OK),
            ).arrange(DOWN, buff=0.15)
            plate = RoundedRectangle(width=card.width + 0.6, height=card.height + 0.45, corner_radius=0.15,
                                     fill_color="#020617", fill_opacity=0.95, stroke_color=CURRENT, stroke_width=2)
            box = VGroup(plate, card.move_to(plate)).move_to(RIGHT * 4.85 + DOWN * 0.7)
            if box.width > 3.9:
                box.scale_to_fit_width(3.9)
            self.play(FadeOut(k_lab), FadeIn(box, shift=UP * 0.2),
                      r_row[0].box.animate.set_stroke(OK, width=6), run_time=0.6)
        self.play(FadeOut(VGroup(statement, row, hit, p_row, r_row, p_lab, r_lab, eq, box)), run_time=0.5)

    def overwrite_trap(self):
        with self.voice("overwrite"):
            title = Text("Vì sao chỉ giữ chỉ số ĐẦU TIÊN?", font_size=36, color=CURRENT).to_edge(UP, buff=0.9)
            row = make_array([6, 6], side=0.9, buff=0.14).move_to(UP * 1.5)
            k_lab = Text("k = 6", font=MONO, font_size=28, color=CURRENT).next_to(row, RIGHT, buff=0.5)
            keep = VGroup(
                Text("giữ chỉ số đầu (code này)", font_size=26, color=OK),
                Text("{0: −1}", font=MONO, font_size=24),
                Text("i=0: dư 0, 0 − (−1) = 1 ✗", font=MONO, font_size=22),
                Text("i=1: dư 0, 1 − (−1) = 2 ✓", font=MONO, font_size=22, color=OK),
                Text("→ True  (6 + 6 = 12)", font=MONO, font_size=24, color=OK),
            ).arrange(DOWN, buff=0.14, aligned_edge=LEFT).move_to(LEFT * 3.4 + DOWN * 0.7)
            over = VGroup(
                Text("ghi đè mỗi lần", font_size=26, color=BAD),
                Text("{0: −1}", font=MONO, font_size=24),
                Text("i=0: 1 ✗ → ghi {0: 0}", font=MONO, font_size=22),
                Text("i=1: 1 − 0 = 1 ✗ → ghi {0: 1}", font=MONO, font_size=22, color=BAD),
                Text("→ False  ✗ sai", font=MONO, font_size=24, color=BAD),
            ).arrange(DOWN, buff=0.14, aligned_edge=LEFT).move_to(RIGHT * 3.4 + DOWN * 0.7)
            self.play(FadeIn(title), FadeIn(row), FadeIn(k_lab), run_time=0.5)
            self.play(FadeIn(keep, lag_ratio=0.25), run_time=1.6)
            self.play(FadeIn(over, lag_ratio=0.25), run_time=1.6)
        self.play(FadeOut(VGroup(title, row, k_lab, keep, over)), run_time=0.5)

    def code_python(self):
        src = python_solution(PY_FILE)
        py = make_code(src, "python", font_size=26)
        py.scale_to_fit_width(min(py.width, 10.5))
        if py.height > 5.4:
            py.scale_to_fit_height(5.4)
        py.move_to(UP * 0.45)
        path = Text("leetcode-38-bai/lc523-continuous-subarray-sum.py", font=MONO, font_size=20, color=MUTED)
        path.next_to(py, DOWN, buff=0.15)
        with self.voice("py_intro"):
            self.play(FadeIn(py), FadeIn(path))
        ln = {k: line_of(src, s) for k, s in {
            "init": "remainder_index = {0: -1}", "rem": "remainder = prefix_sum % k",
            "len": "if i - remainder_index[remainder] > 1", "else": "else:",
        }.items()}
        cursor = highlight(py, ln["init"])
        with self.voice("py_rem"):
            self.play(FadeIn(cursor))
            self.play(cursor.animate.become(highlight(py, ln["rem"])), run_time=0.6)
        with self.voice("py_len"):
            self.play(cursor.animate.become(highlight(py, ln["len"], ln["len"] + 1)))
        with self.voice("py_else"):
            self.play(cursor.animate.become(highlight(py, ln["else"], ln["else"] + 1)))
        self.wait(0.3)
        self.play(FadeOut(VGroup(py, path, cursor)), run_time=0.5)

    def code_java(self):
        src = java_solution(JAVA_FILE)
        java = make_code(src, "java", font_size=24)
        java.scale_to_fit_width(8.4)
        if java.height > 5.5:
            java.scale_to_fit_height(5.5)
        java.move_to(LEFT * 2.4 + UP * 0.45)
        path = Text(".../groupc/ContinuousSubarraySum.java", font=MONO, font_size=18, color=MUTED).next_to(java, DOWN, buff=0.1)
        ln = {k: line_of(src, s) for k, s in {
            "get": "firstIndexByRemainder.get(remainder)", "mod": "int remainder = prefixSum % k",
            "sum": "int prefixSum = 0",
        }.items()}
        right_x = RIGHT * 4.75
        with self.voice("java_intro"):
            cursor = highlight(java, ln["get"], ln["get"] + 1)
            self.play(FadeIn(java), FadeIn(path), run_time=0.8)
            self.play(FadeIn(cursor), run_time=0.4)
        with self.voice("java_mod"):
            mod = VGroup(
                Text("Java:   -1 % 6 = -1", font=MONO, font_size=22, color=BAD),
                Text("Python: -1 % 6 =  5", font=MONO, font_size=22, color=OK),
                Text("đề: nums[i] ≥ 0 → an toàn", font_size=22, color=FG),
                Text("có số âm:", font_size=22, color=MUTED),
                Text("((x % k) + k) % k", font=MONO, font_size=22, color=CURRENT),
            ).arrange(DOWN, buff=0.13, aligned_edge=LEFT).move_to(right_x + UP * 1.2)
            if mod.width > 4.0:
                mod.scale_to_fit_width(4.0)
            self.play(cursor.animate.become(highlight(java, ln["mod"])), FadeIn(mod))
        with self.voice("java_int"):
            ov = VGroup(
                Text("tổng ≤ 2³¹ − 1 → int OK", font=MONO, font_size=20, color=OK),
                Text("k ≥ 1 → không chia 0", font=MONO, font_size=20, color=OK),
            ).arrange(DOWN, buff=0.14, aligned_edge=LEFT)
            plate = RoundedRectangle(width=ov.width + 0.5, height=ov.height + 0.4, corner_radius=0.15,
                                     fill_color="#020617", fill_opacity=0.95, stroke_color=CURRENT, stroke_width=2)
            box = VGroup(plate, ov.move_to(plate)).move_to(right_x + DOWN * 1.1)
            if box.width > 4.1:
                box.scale_to_fit_width(4.1)
            self.play(cursor.animate.become(highlight(java, ln["sum"])), FadeIn(box, shift=UP * 0.2))
        self.wait(0.4)
        self.play(FadeOut(VGroup(java, path, cursor, mod, box)), run_time=0.5)

    def compare_and_complexity(self):
        with self.voice("compare33"):
            rows = [
                ("", "Bài 33 · Subarray Sum = K", "Bài 37 · bội của k", FG),
                ("Câu hỏi", "đếm bao nhiêu đoạn", "có hay không, dài ≥ 2", ACCENT),
                ("Khoá", "tổng tiền tố P", "P % k", ACCENT),
                ("Giá trị", "số lần đã gặp", "chỉ số đầu tiên", OK),
                ("Khởi tạo", "{0: 1}", "{0: −1}", CURRENT),
            ]
            table = VGroup()
            for r, (a, b, c, color) in enumerate(rows):
                row = VGroup(Text(a, font_size=26, color=MUTED), Text(b, font_size=26, color=color),
                             Text(c, font_size=26, color=color))
                for item, x in zip(row, (-4.9, -1.0, 3.6)):
                    item.move_to([x, 2.0 - r * 0.72, 0])
                table.add(row)
            rule = Rectangle(width=12.6, height=0.02, stroke_width=0, fill_color=MUTED, fill_opacity=0.6).move_to([0, 1.63, 0])
            self.play(FadeIn(table[0]), FadeIn(rule), run_time=0.4)
            for row in table[1:]:
                self.play(FadeIn(row, shift=RIGHT * 0.2), run_time=0.45)
        self.play(FadeOut(VGroup(table, rule)), run_time=0.4)

        with self.voice("complexity"):
            cards = VGroup()
            for label, value, note in (("Time", "O(n)", "duyệt 1 lần"), ("Space", "O(min(n, k))", "≤ k số dư khác nhau")):
                box = RoundedRectangle(width=5.0, height=2.5, corner_radius=0.25, stroke_color=ACCENT, stroke_width=3)
                content = VGroup(Text(label, font_size=30, color=MUTED), Text(value, font_size=52, color=FG, weight=BOLD),
                                 Text(note, font_size=22, color=MUTED)).arrange(DOWN, buff=0.2).move_to(box)
                cards.add(VGroup(box, content))
            cards.arrange(RIGHT, buff=0.7).move_to(UP * 0.3)
            self.play(FadeIn(cards, shift=UP * 0.3))
        self.play(FadeOut(cards), run_time=0.4)

        with self.voice("outro", pad=1.2):
            nxt = VGroup(
                Text("Tiếp theo — bài cuối nhóm C", font_size=28, color=MUTED),
                Text("Bài 38 · Design HashMap", font_size=54, weight=BOLD),
                Text("LeetCode #706 · tự cài HashMap", font_size=28, color=ACCENT),
            ).arrange(DOWN, buff=0.3)
            self.play(FadeIn(nxt, shift=UP * 0.2))


class LessonGif(Base):
    VOICE = False

    def construct(self):
        title = Text("Continuous Subarray Sum · số dư % k", font_size=24, color=MUTED).to_corner(UL, buff=0.35)
        self.add(title)
        run = self.remainder_walkthrough(WALK, K, voiced=False)
        self.wait(1.5)
        self.play(FadeOut(run), run_time=0.4)
