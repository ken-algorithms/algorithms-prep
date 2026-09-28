"""Bài 32 — Longest Consecutive Sequence (#128). Code hiển thị được đọc trực tiếp từ file nguồn Python/Java.

Trọng tâm: chỉ đếm từ ĐẦU dãy (num − 1 ∉ set) → mỗi số được đi qua 1 lần → O(n).
Thứ tự duyệt lấy bằng list(set(nums)) — đúng thứ tự vòng `for num in num_set` chạy thật.

    ./build.sh            # video đầy đủ (LessonVideo) + GIF (LessonGif)
"""

import sys
from contextlib import nullcontext
from pathlib import Path

from manim import (
    BOLD, DOWN, LEFT, RIGHT, UL, UP,
    Create, FadeIn, FadeOut, Indicate, Rectangle, RoundedRectangle,
    SurroundingRectangle, Text, Transform, VGroup, Write,
)

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / "common"))
from kit import (  # noqa: E402
    ACCENT, BAD, CURRENT, FG, JAVA_DIR, MONO, MUTED, OK, PY_DIR, Cell, Lesson,
    highlight, java_solution, line_of, make_array, make_code, python_body, python_solution,
)

PY_FILE = PY_DIR / "lc128-longest-consecutive-sequence.py"
JAVA_FILE = JAVA_DIR / "groupc" / "LongestConsecutiveSequence.java"
NUMS = [100, 4, 200, 1, 3, 2]


def number_line(groups: list[list[int]], present: set[int], side: float = 0.78) -> tuple[VGroup, dict]:
    """Trục số chia đoạn (…); ô mờ = giá trị không có trong set."""
    parts, cells = [], {}
    for g, values in enumerate(groups):
        if g:
            parts.append(Text("…", font_size=30, color=MUTED))
        for v in values:
            cell = Cell(v, side=side)
            cell.label.scale(0.8 if v >= 100 else 1)
            if v not in present:
                cell.box.set_stroke(MUTED, width=2, opacity=0.45)
                cell.label.set_color(MUTED).set_opacity(0.55)
            cells[v] = cell
            parts.append(cell)
    return VGroup(*parts).arrange(RIGHT, buff=0.08), cells


class Base(Lesson):
    LESSON_DIR = HERE

    def consecutive_walkthrough(self, nums: list[int], voiced: bool):
        """Chạy đúng Solution.longest_consecutive trên nums, vẽ từng bước."""
        num_set = set(nums)
        order = list(num_set)
        say = (lambda n: self.voice(f"w{n}")) if voiced else (lambda n: nullcontext())

        line, cells = number_line([[0, 1, 2, 3, 4, 5], [99, 100, 101], [199, 200, 201]], num_set)
        line.move_to(UP * 2.0)
        line_title = Text("num_set trên trục số — ô mờ: không có trong set", font_size=20, color=MUTED)
        line_title.next_to(line, UP, buff=0.18)

        it_row = make_array(order, side=0.62, buff=0.1, indexed=False)
        for c in it_row:
            c.label.scale(0.75 if c.value >= 100 else 1)
        it_row.move_to(LEFT * 3.6 + UP * 0.35)
        it_title = Text("for num in num_set  (thứ tự duyệt thật)", font=MONO, font_size=18, color=MUTED)
        it_title.next_to(it_row, UP, buff=0.15)

        body = python_body(PY_FILE, "longest_consecutive")
        code = make_code(body, "python", font_size=24)
        code.scale_to_fit_width(min(code.width, 5.9)).move_to(RIGHT * 3.65 + DOWN * 0.65)
        ln = {k: line_of(body, n) for k, n in {
            "for": "for num in num_set", "if": "if num - 1 not in num_set", "len1": "length = 1",
            "while": "while num + length in num_set", "inc": "length += 1",
            "best": "best = max(best, length)", "ret": "return best",
        }.items()}
        cursor = highlight(code, ln["for"])

        status = Text("·", font_size=26).set_opacity(0).move_to(LEFT * 3.6 + DOWN * 0.75)
        counters = Text("length = –    best = 0", font=MONO, font_size=26, color=FG).move_to(LEFT * 3.6 + DOWN * 1.6)

        self.play(FadeIn(line), FadeIn(line_title), FadeIn(it_row), FadeIn(it_title),
                  FadeIn(code), FadeIn(cursor), FadeIn(counters), run_time=0.9)

        def goto(key):
            return cursor.animate.become(highlight(code, ln[key]))

        def set_status(text, color):
            new = Text(text, font_size=26, color=color).move_to(status)
            return Transform(status, new)

        def set_counters(length, best):
            new = Text(f"length = {length}    best = {best}", font=MONO, font_size=26, color=FG).move_to(counters)
            return Transform(counters, new)

        best, frame, bands = 0, None, VGroup()
        self.add(status)
        for idx, num in enumerate(order):
            with say(num):
                box = SurroundingRectangle(it_row[idx], color=CURRENT, buff=0.06, stroke_width=4)
                self.play(Create(box) if frame is None else Transform(frame, box), goto("for"),
                          cells[num].box.animate.set_stroke(CURRENT, width=6), run_time=0.4)
                frame = frame or box

                self.play(goto("if"), Indicate(cells[num - 1], color=CURRENT, scale_factor=1.25), run_time=0.45)
                if num - 1 in num_set:
                    self.play(set_status(f"{num - 1} ∈ set → không phải đầu dãy", MUTED),
                              it_row[idx].animate.set_opacity(0.35),
                              cells[num].box.animate.set_stroke(ACCENT, width=3), run_time=0.45)
                    continue

                length = 1
                band = SurroundingRectangle(cells[num], color=OK, buff=0.07, stroke_width=4)
                self.play(set_status(f"{num - 1} ∉ set → {num} là đầu dãy", OK), goto("len1"),
                          set_counters(length, best), Create(band), run_time=0.5)
                while True:
                    nxt = num + length
                    self.play(goto("while"), Indicate(cells[nxt], color=CURRENT, scale_factor=1.25), run_time=0.4)
                    if nxt not in num_set:
                        self.play(cells[nxt].box.animate.set_stroke(BAD, width=3, opacity=1), run_time=0.25)
                        self.play(cells[nxt].box.animate.set_stroke(MUTED, width=2, opacity=0.45), run_time=0.2)
                        break
                    length += 1
                    grown = SurroundingRectangle(VGroup(*[cells[v] for v in range(num, nxt + 1)]),
                                                 color=OK, buff=0.07, stroke_width=4)
                    self.play(goto("inc"), Transform(band, grown), set_counters(length, best), run_time=0.4)
                best = max(best, length)
                self.play(goto("best"), set_counters(length, best),
                          band.animate.set_stroke(OK if length == best else MUTED), run_time=0.4)
                self.play(cells[num].box.animate.set_stroke(ACCENT, width=3), run_time=0.2)
                bands.add(band)

        verdict = Text(f"return best = {best}", font=MONO, font_size=32, color=OK, weight=BOLD).move_to(status)
        self.play(goto("ret"), FadeOut(frame), Transform(status, verdict), run_time=0.5)
        return VGroup(line, line_title, it_row, it_title, code, cursor, status, counters, bands)


class LessonVideo(Base):
    def construct(self):
        self.intro()
        header = Text("#128 · Longest Consecutive Sequence", font_size=22, color=MUTED).to_corner(UL, buff=0.35)
        self.add(header)
        self.problem_and_sort()

        with self.voice("idea"):
            idea = VGroup(
                Text("Cách 2 — HashSet", font_size=44, color=CURRENT, weight=BOLD),
                Text("\"x có trong set không?\" → O(1)", font_size=34),
            ).arrange(DOWN, buff=0.4).shift(UP * 0.6)
            self.play(FadeIn(idea, shift=UP * 0.3))
        with self.voice("key"):
            key = Text("Chỉ đếm từ ĐẦU dãy:  num − 1 ∉ set", font=MONO, font_size=34, color=OK)
            key.next_to(idea, DOWN, buff=0.6)
            self.play(FadeIn(key, shift=UP * 0.2))
        self.play(FadeOut(VGroup(idea, key)), run_time=0.4)

        run = self.consecutive_walkthrough(NUMS, voiced=True)
        self.wait(0.8)
        self.play(FadeOut(run), run_time=0.5)

        self.why_start_check()
        self.code_python()
        self.code_java()
        self.complexity_and_tradeoff()

    def intro(self):
        with self.voice("intro"):
            tag = Text("Bài 32 / 38 · Nhóm C — HashSet / Dictionary", font_size=26, color=ACCENT)
            title = Text("Longest Consecutive Sequence", font_size=60, weight=BOLD)
            sub = Text("LeetCode #128 · Medium", font_size=32, color=MUTED)
            group = VGroup(tag, title, sub).arrange(DOWN, buff=0.35).shift(UP * 0.4)
            self.play(FadeIn(tag), Write(title), run_time=1.4)
            self.play(FadeIn(sub, shift=UP * 0.2), run_time=0.6)
        self.play(FadeOut(group), run_time=0.5)

    def problem_and_sort(self):
        with self.voice("problem"):
            statement = VGroup(
                Text("Dãy số nguyên liên tiếp dài nhất?", font_size=40),
                Text("liên tiếp về giá trị · không cần kề nhau trong mảng · yêu cầu O(n)", font_size=26, color=MUTED),
            ).arrange(DOWN, buff=0.25).to_edge(UP, buff=0.9)
            self.play(FadeIn(statement, shift=DOWN * 0.2))

        row = make_array(NUMS, side=1.0, buff=0.18, indexed=False).move_to(UP * 0.1)
        for c in row:
            c.label.scale(0.8 if c.value >= 100 else 1)
        with self.voice("example"):
            self.play(FadeIn(row), run_time=0.6)
            seq = [c for c in row if c.value in (1, 2, 3, 4)]
            self.play(*[c.box.animate.set_stroke(OK, width=6) for c in seq], run_time=0.6)
            ans = Text("→ 4   (1, 2, 3, 4)", font=MONO, font_size=32, color=OK).next_to(row, DOWN, buff=0.45)
            self.play(FadeIn(ans))

        with self.voice("sort"):
            self.play(FadeOut(ans), *[c.box.animate.set_stroke(ACCENT, width=3) for c in seq], run_time=0.3)
            slots = [c.get_center() for c in row]
            order = sorted(range(len(row)), key=lambda i: row[i].value)
            self.play(*[row[i].animate.move_to(slots[j]) for j, i in enumerate(order)], run_time=1.4)
            run = SurroundingRectangle(VGroup(*[row[i] for i in order[:4]]), color=OK, buff=0.1, stroke_width=4)
            cost = Text("sort → O(n log n)  ✗ chưa đạt O(n)", font=MONO, font_size=28, color=BAD)
            cost.next_to(row, DOWN, buff=0.5)
            self.play(Create(run), FadeIn(cost))
        self.play(FadeOut(VGroup(statement, row, run, cost)), run_time=0.5)

    def why_start_check(self):
        values = list(range(1, 7))
        line, cells = number_line([values], set(values), side=0.9)
        line.move_to(UP * 1.9)
        left_x = cells[1].get_left()[0]
        step = cells[2].get_x() - cells[1].get_x()
        bars, counts = VGroup(), VGroup()
        with self.voice("trap"):
            title = Text("Bỏ kiểm tra num − 1: số nào cũng đếm tới cuối", font_size=28, color=BAD)
            title.next_to(line, UP, buff=0.25)
            self.play(FadeIn(line), FadeIn(title), run_time=0.6)
            for i, start in enumerate(values):
                y = 0.85 - 0.36 * i
                bar = Rectangle(width=step * (7 - start) - 0.1, height=0.24, stroke_width=0,
                                fill_color=BAD, fill_opacity=0.6)
                bar.move_to([left_x + step * (start - 1) + bar.width / 2 + 0.05, y, 0])
                count = Text(str(7 - start), font=MONO, font_size=22, color=BAD).next_to(bar, RIGHT, buff=0.2)
                count.set_x(cells[6].get_right()[0] + 0.45)
                bars.add(bar)
                counts.add(count)
                self.play(FadeIn(bar, shift=RIGHT * 0.2), FadeIn(count), run_time=0.3)

        with self.voice("trap_count"):
            total = Text("6 + 5 + 4 + 3 + 2 + 1 = 21 = n(n+1)/2  →  O(n²)", font=MONO, font_size=28, color=BAD)
            total.move_to(DOWN * 1.65)
            self.play(FadeIn(total))

        with self.voice("trap_fix"):
            fixed = Text("Có kiểm tra: chỉ 1 là đầu dãy → 6 bước → O(n)", font=MONO, font_size=28, color=OK).move_to(total)
            new_title = Text("Có kiểm tra num − 1: chỉ đầu dãy mới đếm", font_size=28, color=OK).move_to(title)
            self.play(
                bars[0].animate.set_fill(OK, opacity=0.7), counts[0].animate.set_color(OK),
                *[b.animate.set_fill(MUTED, opacity=0.15) for b in bars[1:]],
                *[c.animate.set_opacity(0.25) for c in counts[1:]],
                Transform(total, fixed), Transform(title, new_title), run_time=0.8,
            )
        self.wait(0.3)
        self.play(FadeOut(VGroup(line, title, bars, counts, total)), run_time=0.5)

    def code_python(self):
        src = python_solution(PY_FILE)
        py = make_code(src, "python", font_size=26)
        py.scale_to_fit_width(min(py.width, 10.5))
        if py.height > 5.4:
            py.scale_to_fit_height(5.4)
        py.move_to(UP * 0.45)
        path = Text("leetcode-38-bai/lc128-longest-consecutive-sequence.py", font=MONO, font_size=20, color=MUTED)
        path.next_to(py, DOWN, buff=0.15)
        with self.voice("py_intro"):
            self.play(FadeIn(py), FadeIn(path))
        ln = {k: line_of(src, n) for k, n in {
            "set": "num_set = set(nums)", "if": "if num - 1 not in num_set",
            "while": "while num + length in num_set", "best": "best = max(best, length)",
        }.items()}
        cursor = highlight(py, ln["set"])
        with self.voice("py_set"):
            self.play(FadeIn(cursor))
        with self.voice("py_start"):
            self.play(cursor.animate.become(highlight(py, ln["if"])))
        with self.voice("py_while"):
            self.play(cursor.animate.become(highlight(py, ln["while"], ln["best"])))
        self.wait(0.3)
        self.play(FadeOut(VGroup(py, path, cursor)), run_time=0.5)

    def code_java(self):
        src = java_solution(JAVA_FILE)
        java = make_code(src, "java", font_size=24)
        java.scale_to_fit_height(min(java.height, 5.7))
        if java.width > 6.5:
            java.scale_to_fit_width(6.5)
        java.move_to(RIGHT * 3.55 + UP * 0.4)
        path = Text(".../groupc/LongestConsecutiveSequence.java", font=MONO, font_size=18,
                    color=MUTED).next_to(java, DOWN, buff=0.1)
        ln = {k: line_of(src, n) for k, n in {
            "loop": "for (int value : values)", "start": "boolean isSequenceStart",
            "helper": "private int sequenceLengthFrom",
        }.items()}
        left_x = LEFT * 3.7
        with self.voice("java_intro"):
            cursor = highlight(java, ln["helper"], ln["helper"] + 6)
            self.play(FadeIn(java), FadeIn(path), run_time=0.8)
            self.play(FadeIn(cursor), run_time=0.4)

        with self.voice("java_start"):
            named = VGroup(
                Text("boolean isSequenceStart =", font=MONO, font_size=22, color=CURRENT),
                Text("    !values.contains(value - 1);", font=MONO, font_size=22, color=CURRENT),
                Text("đặt tên cho điều kiện → đọc như câu", font_size=22, color=MUTED),
            ).arrange(DOWN, buff=0.12, aligned_edge=LEFT).move_to(left_x + UP * 1.4)
            self.play(cursor.animate.become(highlight(java, ln["start"])), FadeIn(named))

        with self.voice("java_trap"):
            trap = VGroup(
                Text("for (int value : values)  ✓ set", font=MONO, font_size=22, color=OK),
                Text("for (int num : nums)      ✗ mảng gốc", font=MONO, font_size=22, color=BAD),
                Text("1000 số 1 trùng + dãy 2…1000:", font_size=22, color=FG),
                Text("999 bước  vs  999.000 bước", font=MONO, font_size=24, color=CURRENT),
            ).arrange(DOWN, buff=0.16, aligned_edge=LEFT)
            plate = RoundedRectangle(width=trap.width + 0.5, height=trap.height + 0.4, corner_radius=0.15,
                                     fill_color="#020617", fill_opacity=0.95, stroke_color=CURRENT, stroke_width=2)
            box = VGroup(plate, trap.move_to(plate)).move_to(left_x + DOWN * 1.0)
            if box.width > 6.0:
                box.scale_to_fit_width(6.0)
            self.play(cursor.animate.become(highlight(java, ln["loop"])), FadeIn(box, shift=UP * 0.2))
        self.wait(0.4)
        self.play(FadeOut(VGroup(java, path, cursor, named, box)), run_time=0.5)

    def complexity_and_tradeoff(self):
        with self.voice("complexity"):
            cards = VGroup()
            for label, value, note in (("Time", "O(n)", "mỗi số đi qua ≤ 1 lần"), ("Space", "O(n)", "cho num_set")):
                box = RoundedRectangle(width=4.4, height=2.5, corner_radius=0.25, stroke_color=ACCENT, stroke_width=3)
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
                ("Cách", "Time", "Ghi chú", FG),
                ("Sort rồi đếm", "O(n log n)", "chưa đạt yêu cầu", BAD),
                ("Set, không kiểm tra đầu dãy", "O(n²)", "dãy dài là chậm", BAD),
                ("Set + chỉ đếm từ đầu dãy", "O(n)", "bài này ✓", OK),
                ("Union-Find", "≈ O(n)", "đúng, nhưng dài dòng", CURRENT),
            ]
            table = VGroup()
            col_x = [-3.4, 1.3, 4.4]
            for r, (name, time, note, color) in enumerate(rows):
                row = VGroup(Text(name, font_size=28, color=color), Text(time, font_size=28, color=color),
                             Text(note, font_size=26, color=color))
                for c, item in enumerate(row):
                    item.move_to([col_x[c], 1.9 - r * 0.85, 0])
                table.add(row)
            rule = Rectangle(width=12.8, height=0.02, stroke_width=0, fill_color=MUTED, fill_opacity=0.6)
            rule.move_to([0.3, 1.9 - 0.43, 0])
            self.play(FadeIn(table[0]), FadeIn(rule), run_time=0.4)
            for row in table[1:]:
                self.play(FadeIn(row, shift=RIGHT * 0.2), run_time=0.45)
        self.play(FadeOut(VGroup(table, rule)), run_time=0.4)

        with self.voice("outro", pad=1.2):
            nxt = VGroup(
                Text("Tiếp theo", font_size=28, color=MUTED),
                Text("Bài 33 · Subarray Sum Equals K", font_size=52, weight=BOLD),
                Text("LeetCode #560 · Prefix Sum + HashMap", font_size=28, color=ACCENT),
            ).arrange(DOWN, buff=0.3)
            self.play(FadeIn(nxt, shift=UP * 0.2))


class LessonGif(Base):
    VOICE = False
    HOLD_WITHOUT_VOICE = 0.6

    def construct(self):
        title = Text("Longest Consecutive · chỉ đếm từ đầu dãy", font_size=24, color=MUTED).to_corner(UL, buff=0.35)
        self.add(title)
        run = self.consecutive_walkthrough(NUMS, voiced=False)
        self.wait(1.5)
        self.play(FadeOut(run), run_time=0.4)
