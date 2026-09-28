"""Bài 36 — 4Sum II (#454). Code hiển thị được đọc trực tiếp từ file nguồn Python/Java.

Trọng tâm: chia đôi (meet in the middle) — lưới a + b đổ vào HashMap đếm, lưới −(c + d) tra ngược.
Bảng sum_ab vẽ ĐÚNG như defaultdict chạy thật — kể cả key rác 2 → 0 khi tra không thấy.

    ./build.sh            # video đầy đủ (LessonVideo) + GIF (LessonGif)
"""

import sys
from contextlib import nullcontext
from pathlib import Path

from manim import (
    BOLD, DOWN, LEFT, RIGHT, UL, UP,
    FadeIn, FadeOut, Indicate, Rectangle, RoundedRectangle, Square,
    SurroundingRectangle, Text, Transform, VGroup, Write,
)

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / "common"))
from kit import (  # noqa: E402
    ACCENT, BAD, BG, CURRENT, FG, JAVA_DIR, MONO, MUTED, OK, PY_DIR, Lesson,
    highlight, java_solution, line_of, make_array, make_code, python_solution,
)

PY_FILE = PY_DIR / "lc454-4sum-ii.py"
JAVA_FILE = JAVA_DIR / "groupc" / "FourSumII.java"
EXAMPLE = ([1, 2], [-2, -1], [-1, 2], [0, 2])
WALK = ([1, 2], [-2, -1], [-1, 1], [0, -1])


class SumGrid(VGroup):
    """Lưới hàng × cột: tiêu đề hàng bên trái, tiêu đề cột bên trên, ô ở giữa để ghi phép tính."""

    def __init__(self, rows: list[int], cols: list[int], rname: str, cname: str, title: str, cell: float = 0.95):
        super().__init__()
        self.rows, self.cols = rows, cols
        self.cells = [[Square(side_length=cell, stroke_color=ACCENT, stroke_width=3).set_fill(BG, opacity=1)
                       for _ in cols] for _ in rows]
        grid = VGroup(*[VGroup(*r).arrange(RIGHT, buff=0.08) for r in self.cells]).arrange(DOWN, buff=0.08)
        self.row_heads = VGroup(*[Text(f"{rname}={v}", font=MONO, font_size=20, color=MUTED)
                                  .next_to(self.cells[i][0], LEFT, buff=0.18) for i, v in enumerate(rows)])
        self.col_heads = VGroup(*[Text(f"{cname}={v}", font=MONO, font_size=20, color=MUTED)
                                  .next_to(self.cells[0][j], UP, buff=0.14) for j, v in enumerate(cols)])
        self.title = Text(title, font=MONO, font_size=24, color=CURRENT).next_to(self.col_heads, UP, buff=0.2)
        self.values = [[None for _ in cols] for _ in rows]
        self.add(grid, self.row_heads, self.col_heads, self.title)

    def write(self, i: int, j: int, text: str, color: str = FG) -> Text:
        t = Text(text, font=MONO, font_size=26, color=color).move_to(self.cells[i][j])
        self.values[i][j] = t
        self.add(t)
        return t


class CountMap(VGroup):
    def __init__(self, title: str, width: float = 2.3, height: float = 2.6):
        super().__init__()
        self.box = RoundedRectangle(width=width, height=height, corner_radius=0.18, stroke_color=CURRENT, stroke_width=3)
        self.title = Text(title, font=MONO, font_size=22, color=CURRENT).next_to(self.box, UP, buff=0.12)
        self.rows: dict[int, Text] = {}
        self.add(self.box, self.title)

    def put(self, key: int, count: int, color: str = FG):
        text = Text(f"{key:>2} → {count}", font=MONO, font_size=26, color=color)
        if key in self.rows:
            return Transform(self.rows[key], text.move_to(self.rows[key]))
        text.move_to(self.box.get_top() + DOWN * (0.36 + 0.44 * len(self.rows)))
        self.rows[key] = text
        self.add(text)
        return FadeIn(text, shift=DOWN * 0.15)


class Base(Lesson):
    LESSON_DIR = HERE

    def meet_walkthrough(self, arrays, voiced: bool):
        """Chạy đúng Solution.four_sum_count: lưới a + b → sum_ab, rồi lưới −(c + d) tra sum_ab."""
        n1, n2, n3, n4 = arrays
        say = (lambda k: self.voice(k)) if voiced else (lambda k: nullcontext())
        ab = SumGrid(n1, n2, "a", "b", "a + b").move_to(LEFT * 4.35 + UP * 0.55)
        cd = SumGrid(n3, n4, "c", "d", "−(c + d)").move_to(RIGHT * 2.55 + UP * 0.55)
        smap = CountMap("sum_ab").move_to(LEFT * 1.0 + UP * 0.35)
        result_t = Text("result = 0", font=MONO, font_size=30, color=FG).move_to(RIGHT * 5.55 + UP * 0.55)
        running = Text("·", font=MONO, font_size=24).set_opacity(0).move_to(DOWN * 1.8)
        self.play(FadeIn(ab), FadeIn(cd), FadeIn(smap), FadeIn(result_t), run_time=0.8)
        self.add(running)

        def show_line(code: str):
            return Transform(running, Text(f"▶ {code}", font=MONO, font_size=24, color=CURRENT).move_to(running))

        sum_ab: dict[int, int] = {}
        with say("p1"):
            self.play(show_line("sum_ab[a + b] += 1"), run_time=0.3)
            for i, a in enumerate(n1):
                for j, b in enumerate(n2):
                    s = a + b
                    sum_ab[s] = sum_ab.get(s, 0) + 1
                    self.play(Indicate(ab.row_heads[i], color=CURRENT), Indicate(ab.col_heads[j], color=CURRENT),
                              FadeIn(ab.write(i, j, str(s))), smap.put(s, sum_ab[s]), run_time=0.6)

        result, frame = 0, None
        self.play(show_line("result += sum_ab[-(c + d)]"), run_time=0.3)
        step = 0
        for i, c in enumerate(n3):
            for j, d in enumerate(n4):
                target = -(c + d)
                with say(f"q{step}"):
                    box = SurroundingRectangle(cd.cells[i][j], color=CURRENT, buff=0.05, stroke_width=4)
                    self.play(FadeIn(box) if frame is None else Transform(frame, box),
                              FadeIn(cd.write(i, j, str(target), CURRENT)), run_time=0.45)
                    frame = frame or box
                    found = sum_ab.get(target, 0)
                    if found:
                        hits = [ab.cells[x][y] for x in range(len(n1)) for y in range(len(n2)) if n1[x] + n2[y] == target]
                        self.play(Indicate(smap.rows[target], color=OK, scale_factor=1.3),
                                  *[h.animate.set_stroke(OK, width=6) for h in hits], run_time=0.55)
                        result += found
                        self.play(Transform(result_t, Text(f"result = {result}", font=MONO, font_size=30, color=OK)
                                            .move_to(result_t)),
                                  cd.cells[i][j].animate.set_fill(OK, opacity=0.25), run_time=0.45)
                        self.play(*[h.animate.set_stroke(ACCENT, width=3) for h in hits], run_time=0.2)
                    else:
                        anims = [cd.cells[i][j].animate.set_fill(MUTED, opacity=0.15)]
                        if target not in sum_ab:
                            sum_ab[target] = 0            # defaultdict: đọc [x] là tạo key x = 0
                            anims.append(smap.put(target, 0, MUTED))
                        self.play(*anims, run_time=0.5)
                step += 1

        if voiced:
            with self.voice("why_count"):
                zero = [ab.cells[x][y] for x in range(len(n1)) for y in range(len(n2)) if n1[x] + n2[y] == 0]
                self.play(FadeOut(frame), *[z.animate.set_stroke(OK, width=6) for z in zero],
                          Indicate(smap.rows[0], color=OK, scale_factor=1.4), run_time=0.8)
                pairs = Text("0 = 1 + (−1) = 2 + (−2) → 2 cặp → 2 bộ riêng", font=MONO, font_size=24, color=OK)
                self.play(Transform(running, pairs.move_to(running)))
            frame = None
        parts = [ab, cd, smap, result_t, running]
        return VGroup(*parts, *([frame] if frame else []))


class LessonVideo(Base):
    def construct(self):
        self.intro()
        header = Text("#454 · 4Sum II", font_size=22, color=MUTED).to_corner(UL, buff=0.35)
        self.add(header)
        self.problem()
        self.costs()

        run = self.meet_walkthrough(WALK, voiced=True)
        self.wait(0.6)
        self.play(FadeOut(run), run_time=0.5)

        self.code_python()
        self.code_java()
        self.ksum_and_complexity()

    def intro(self):
        with self.voice("intro"):
            tag = Text("Bài 36 / 38 · Nhóm C — HashSet / Dictionary", font_size=26, color=ACCENT)
            title = Text("4Sum II", font_size=80, weight=BOLD)
            sub = Text("LeetCode #454 · Medium", font_size=32, color=MUTED)
            group = VGroup(tag, title, sub).arrange(DOWN, buff=0.35).shift(UP * 0.4)
            self.play(FadeIn(tag), Write(title), run_time=1.4)
            self.play(FadeIn(sub, shift=UP * 0.2), run_time=0.6)
        self.play(FadeOut(group), run_time=0.5)

    def problem(self):
        names = ["nums1", "nums2", "nums3", "nums4"]
        rows = VGroup()
        for name, values in zip(names, EXAMPLE):
            arr = make_array(values, side=0.85, buff=0.12, indexed=False)
            lab = Text(name, font=MONO, font_size=22, color=MUTED).next_to(arr, LEFT, buff=0.3)
            rows.add(VGroup(lab, arr))
        rows.arrange(DOWN, buff=0.25, aligned_edge=LEFT).move_to(LEFT * 1.8 + DOWN * 0.1)
        with self.voice("problem"):
            statement = Text("Chọn 1 số ở mỗi mảng — bao nhiêu cách để tổng = 0?", font_size=34).to_edge(UP, buff=0.9)
            self.play(FadeIn(statement, shift=DOWN * 0.2), FadeIn(rows), run_time=0.8)
        with self.voice("example"):
            answers = [(0, 0, 0, 1), (1, 1, 0, 0)]
            texts = VGroup()
            for n, tup in enumerate(answers):
                color = OK if n == 0 else CURRENT
                cells = [rows[k][1][tup[k]] for k in range(4)]
                vals = [EXAMPLE[k][tup[k]] for k in range(4)]
                eq = " + ".join(f"({v})" if v < 0 else str(v) for v in vals) + " = 0"
                t = Text(eq, font=MONO, font_size=26, color=color)
                texts.add(t)
                self.play(*[c.box.animate.set_stroke(color, width=6) for c in cells], run_time=0.5)
                self.wait(0.3)
                self.play(*[c.box.animate.set_stroke(ACCENT, width=3) for c in cells], run_time=0.2)
            texts.add(Text("→ 2", font=MONO, font_size=34, color=OK))
            texts.arrange(DOWN, buff=0.25, aligned_edge=LEFT).next_to(rows, RIGHT, buff=0.9)
            self.play(FadeIn(texts))
        self.play(FadeOut(VGroup(statement, rows, texts)), run_time=0.5)

    def costs(self):
        rows_spec = [
            ("brute", "4 vòng lặp", "O(n⁴)", "1 600 000 000", BAD),
            ("three", "3 vòng + HashMap nums4", "O(n³)", "8 000 000", CURRENT),
            ("idea", "chia đôi: 2 + 2", "O(n²)", "40 000", OK),
        ]
        head = VGroup(Text("Cách", font_size=26, color=MUTED), Text("Time", font_size=26, color=MUTED),
                      Text("n = 200", font_size=26, color=MUTED))
        for item, x in zip(head, (-3.6, 1.2, 4.3)):
            item.move_to([x, 2.0, 0])
        rule = Rectangle(width=12.4, height=0.02, stroke_width=0, fill_color=MUTED, fill_opacity=0.6).move_to([0.3, 1.62, 0])
        table = VGroup(head, rule)
        eq = None
        for r, (key, name, big_o, count, color) in enumerate(rows_spec):
            with self.voice(key):
                row = VGroup(Text(name, font_size=30, color=color), Text(big_o, font_size=30, color=color),
                             Text(count, font=MONO, font_size=28, color=color))
                for item, x in zip(row, (-3.6, 1.2, 4.3)):
                    item.move_to([x, 1.2 - r * 0.7, 0])
                table.add(row)
                anims = [FadeIn(row, shift=RIGHT * 0.2)]
                if r == 0:
                    anims.append(FadeIn(head))
                    anims.append(FadeIn(rule))
                self.play(*anims, run_time=0.6)
                if key == "idea":
                    eq = VGroup(
                        Text("(a + b) + (c + d) = 0", font=MONO, font_size=30, color=FG),
                        Text("⇔  a + b = −(c + d)", font=MONO, font_size=30, color=OK),
                        Text("đếm a + b vào HashMap · tra −(c + d)", font_size=26, color=MUTED),
                    ).arrange(DOWN, buff=0.18).move_to(DOWN * 1.6)
                    self.play(FadeIn(eq, shift=UP * 0.2))
        self.play(FadeOut(VGroup(table, eq)), run_time=0.5)

    def code_python(self):
        src = python_solution(PY_FILE)
        py = make_code(src, "python", font_size=26)
        py.scale_to_fit_width(min(py.width, 11))
        if py.height > 5.4:
            py.scale_to_fit_height(5.4)
        py.move_to(UP * 0.45)
        path = Text("leetcode-38-bai/lc454-4sum-ii.py", font=MONO, font_size=20, color=MUTED).next_to(py, DOWN, buff=0.15)
        with self.voice("py_intro"):
            self.play(FadeIn(py), FadeIn(path))
        ln = {k: line_of(src, s) for k, s in {
            "p1": "for a in nums1", "p1_end": "sum_ab[a + b] += 1",
            "p2": "for c in nums3", "p2_end": "result += sum_ab[-(c + d)]",
        }.items()}
        cursor = highlight(py, ln["p1"], ln["p1_end"])
        with self.voice("py_phase1"):
            self.play(FadeIn(cursor))
        with self.voice("py_phase2"):
            self.play(cursor.animate.become(highlight(py, ln["p2"], ln["p2_end"])))
        with self.voice("py_default"):
            self.play(cursor.animate.become(highlight(py, ln["p2_end"], color=BAD)))
        self.wait(0.3)
        self.play(FadeOut(VGroup(py, path, cursor)), run_time=0.5)

    def code_java(self):
        src = java_solution(JAVA_FILE)
        java = make_code(src, "java", font_size=24)
        java.scale_to_fit_width(8.7)
        if java.height > 5.5:
            java.scale_to_fit_height(5.5)
        java.move_to(LEFT * 2.25 + UP * 0.45)
        path = Text(".../groupc/FourSumII.java", font=MONO, font_size=18, color=MUTED).next_to(java, DOWN, buff=0.1)
        ln = {k: line_of(src, s) for k, s in {
            "merge": "countByPairSum.merge(a + b, 1, Integer::sum)", "get": "getOrDefault(-(c + d), 0)",
        }.items()}
        right_x = RIGHT * 4.85
        with self.voice("java_intro"):
            cursor = highlight(java, ln["merge"])
            self.play(FadeIn(java), FadeIn(path), run_time=0.8)
            notes = VGroup(
                Text("merge(...)", font=MONO, font_size=22, color=CURRENT), Text("≈ sum_ab[a+b] += 1", font=MONO, font_size=20),
                Text("getOrDefault(...)", font=MONO, font_size=22, color=CURRENT), Text("không tạo key rác", font_size=22, color=OK),
            ).arrange(DOWN, buff=0.14, aligned_edge=LEFT).move_to(right_x + UP * 1.3)
            self.play(FadeIn(cursor), FadeIn(notes), run_time=0.5)
            self.play(cursor.animate.become(highlight(java, ln["get"])), run_time=0.5)
        with self.voice("java_int"):
            ov = VGroup(
                Text("|nums[i]| ≤ 2²⁸", font=MONO, font_size=22, color=FG),
                Text("|a + b| ≤ 2²⁹", font=MONO, font_size=22, color=FG),
                Text("< 2³¹ → int an toàn", font=MONO, font_size=22, color=OK),
            ).arrange(DOWN, buff=0.14, aligned_edge=LEFT)
            plate = RoundedRectangle(width=ov.width + 0.5, height=ov.height + 0.4, corner_radius=0.15,
                                     fill_color="#020617", fill_opacity=0.95, stroke_color=CURRENT, stroke_width=2)
            box = VGroup(plate, ov.move_to(plate)).move_to(right_x + DOWN * 0.9)
            self.play(FadeIn(box, shift=UP * 0.2))
        self.wait(0.4)
        self.play(FadeOut(VGroup(java, path, cursor, notes, box)), run_time=0.5)

    def ksum_and_complexity(self):
        with self.voice("ksum"):
            title = Text("Meet in the middle", font_size=40, color=CURRENT, weight=BOLD).to_edge(UP, buff=0.9)
            lines = VGroup(
                Text("k = 4:  [A B] | [C D]      → O(n²)", font=MONO, font_size=28, color=OK),
                Text("k = 6:  [A B C] | [D E F]  → O(n³)", font=MONO, font_size=28, color=FG),
                Text("k mảng → O(n^(k/2)) thay vì O(n^k)", font=MONO, font_size=28, color=CURRENT),
            ).arrange(DOWN, buff=0.35, aligned_edge=LEFT).move_to(DOWN * 0.1)
            self.play(FadeIn(title), FadeIn(lines, lag_ratio=0.3), run_time=1.2)
        self.play(FadeOut(VGroup(title, lines)), run_time=0.4)

        with self.voice("complexity"):
            cards = VGroup()
            for label, value, note in (("Time", "O(n²)", "n² cặp a+b + n² cặp c+d"), ("Space", "O(n²)", "≤ n² tổng a + b")):
                box = RoundedRectangle(width=4.6, height=2.5, corner_radius=0.25, stroke_color=ACCENT, stroke_width=3)
                content = VGroup(Text(label, font_size=30, color=MUTED), Text(value, font_size=60, color=FG, weight=BOLD),
                                 Text(note, font_size=22, color=MUTED)).arrange(DOWN, buff=0.2).move_to(box)
                cards.add(VGroup(box, content))
            cards.arrange(RIGHT, buff=0.8).move_to(UP * 0.3)
            self.play(FadeIn(cards, shift=UP * 0.3))
        self.play(FadeOut(cards), run_time=0.4)

        with self.voice("outro", pad=1.2):
            nxt = VGroup(
                Text("Tiếp theo", font_size=28, color=MUTED),
                Text("Bài 37 · Continuous Subarray Sum", font_size=50, weight=BOLD),
                Text("LeetCode #523 · Prefix Sum % k + HashMap", font_size=28, color=ACCENT),
            ).arrange(DOWN, buff=0.3)
            self.play(FadeIn(nxt, shift=UP * 0.2))


class LessonGif(Base):
    VOICE = False

    def construct(self):
        title = Text("4Sum II · chia đôi: a + b = −(c + d)", font_size=24, color=MUTED).to_corner(UL, buff=0.35)
        self.add(title)
        run = self.meet_walkthrough(WALK, voiced=False)
        self.wait(1.5)
        self.play(FadeOut(run), run_time=0.4)
