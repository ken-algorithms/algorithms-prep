"""Bài 34 — Intersection of Two Arrays II (#350). Code hiển thị được đọc trực tiếp từ file nguồn Python/Java.

Trọng tâm: Counter(nums1) = số "suất" mỗi giá trị; duyệt nums2, còn suất thì lấy và trừ 1.
Mỗi suất vẽ thành 1 khối — lấy được thì khối bay xuống hàng result.

    ./build.sh            # video đầy đủ (LessonVideo) + GIF (LessonGif)
"""

import sys
from collections import Counter
from contextlib import nullcontext
from pathlib import Path

from manim import (
    BOLD, DOWN, LEFT, RIGHT, UL, UP,
    Create, FadeIn, FadeOut, Indicate, Line, ReplacementTransform, RoundedRectangle, Square,
    SurroundingRectangle, Text, Transform, TransformFromCopy, VGroup, Write,
)

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / "common"))
from kit import (  # noqa: E402
    ACCENT, BAD, CURRENT, FG, JAVA_DIR, MONO, MUTED, OK, PY_DIR, Cell, Lesson,
    highlight, java_solution, line_of, make_array, make_code, python_body, python_solution,
)

PY_FILE = PY_DIR / "lc350-intersection-of-two-arrays-ii.py"
JAVA_FILE = JAVA_DIR / "groupc" / "IntersectionOfTwoArrays.java"
NUMS1, NUMS2 = [4, 9, 5, 4], [9, 4, 9, 8, 4]


class TokenBank(VGroup):
    """Counter vẽ bằng khối: mỗi khối là một 'suất' còn lại của giá trị đó."""

    def __init__(self, keys: list[int], max_count: int, block: float = 0.42, gap: float = 1.05):
        super().__init__()
        self.keys, self.block = keys, block
        self.stacks: dict[int, list] = {k: [] for k in keys}
        width = gap * len(keys)
        self.base = Line(LEFT * width / 2, RIGHT * width / 2, color=MUTED, stroke_width=3)
        self.labels = VGroup(*[
            Text(str(k), font=MONO, font_size=26).move_to(self.base.get_left() + RIGHT * gap * (i + 0.5) + DOWN * 0.3)
            for i, k in enumerate(keys)
        ])
        self.title = Text("counts = Counter(nums1)", font=MONO, font_size=22, color=CURRENT)
        self.title.next_to(self.base, UP, buff=block * max_count + 0.5)
        self.add(self.base, self.labels, self.title)

    def slot(self, key: int):
        n = len(self.stacks[key])
        label = self.labels[self.keys.index(key)]
        return [label.get_x(), self.base.get_y() + self.block * (n + 0.5) + 0.03 * n, 0]

    def new_block(self, key: int) -> Square:
        block = Square(side_length=self.block * 0.9, stroke_color=ACCENT, stroke_width=2)
        block.set_fill(ACCENT, opacity=0.45).move_to(self.slot(key))
        self.stacks[key].append(block)
        return block


class Base(Lesson):
    LESSON_DIR = HERE

    def intersect_walkthrough(self, nums1: list[int], nums2: list[int], voiced: bool):
        """Chạy đúng Solution.intersect: Counter(nums1), rồi duyệt nums2 lấy suất."""
        say = (lambda key: self.voice(key)) if voiced else (lambda key: nullcontext())
        r1 = make_array(nums1, side=0.7, buff=0.1, indexed=False).move_to(LEFT * 3.4 + UP * 2.35)
        r2 = make_array(nums2, side=0.7, buff=0.1, indexed=False).move_to(LEFT * 3.4 + UP * 1.25)
        l1 = Text("nums1", font=MONO, font_size=20, color=MUTED).next_to(r1, LEFT, buff=0.2)
        l2 = Text("nums2", font=MONO, font_size=20, color=MUTED).next_to(r2, LEFT, buff=0.2)
        counts = Counter(nums1)
        bank = TokenBank(sorted(counts), max(counts.values()))
        bank.shift(LEFT * 3.4 + UP * (-0.95 - bank.base.get_y()))

        res_lab = Text("result", font=MONO, font_size=20, color=MUTED).move_to(LEFT * 6.2 + DOWN * 1.95)
        res_slots = [res_lab.get_right() + RIGHT * (0.5 + 0.66 * i) for i in range(len(nums2))]

        body = python_body(PY_FILE, "intersect")
        code = make_code(body, "python", font_size=24)
        code.scale_to_fit_width(min(code.width, 5.6)).move_to(RIGHT * 3.75 + UP * 0.55)
        ln = {k: line_of(body, n) for k, n in {
            "counter": "counts = Counter(nums1)", "for": "for num in nums2", "if": "if counts[num] > 0",
            "append": "result.append(num)", "dec": "counts[num] -= 1", "ret": "return result",
        }.items()}
        cursor = highlight(code, ln["counter"])
        status = Text("·", font_size=26).set_opacity(0).next_to(code, DOWN, buff=0.4)

        self.play(FadeIn(r1), FadeIn(r2), FadeIn(l1), FadeIn(l2), FadeIn(bank), FadeIn(res_lab),
                  FadeIn(code), FadeIn(cursor), run_time=0.9)
        self.add(status)

        def goto(key):
            return cursor.animate.become(highlight(code, ln[key]))

        def say_status(text, color):
            return Transform(status, Text(text, font_size=26, color=color).move_to(status))

        with say("c0"):
            for cell in r1:
                block = bank.new_block(cell.value)
                self.play(TransformFromCopy(cell.box, block), cell.box.animate.set_stroke(ACCENT), run_time=0.38)
                bank.add(block)

        result, frame, taken = [], None, VGroup()
        for j, num in enumerate(nums2):
            with say(f"w{j}"):
                box = SurroundingRectangle(r2[j], color=CURRENT, buff=0.06, stroke_width=4)
                self.play(Create(box) if frame is None else Transform(frame, box), goto("for"), run_time=0.35)
                frame = frame or box
                left = counts[num]
                self.play(goto("if"), say_status(f"counts[{num}] = {left}", FG), run_time=0.4)
                if left > 0:
                    token = bank.stacks[num].pop()
                    bank.remove(token)
                    out = Cell(num, side=0.6).move_to(res_slots[len(result)])
                    out.box.set_stroke(OK, width=4)
                    self.play(goto("append"), ReplacementTransform(token, out), r2[j].box.animate.set_stroke(OK),
                              say_status(f"counts[{num}] = {left} > 0 → lấy", OK), run_time=0.55)
                    counts[num] -= 1
                    result.append(num)
                    taken.add(out)
                    self.play(goto("dec"), say_status(f"counts[{num}] → {counts[num]}", OK), run_time=0.35)
                else:
                    why = "hết suất" if num in counts else "không có trong nums1"
                    anims = [say_status(f"counts[{num}] = 0 → {why}", BAD), r2[j].box.animate.set_stroke(BAD)]
                    if num in counts:
                        anims.append(Indicate(bank.labels[bank.keys.index(num)], color=BAD, scale_factor=1.4))
                    self.play(*anims, run_time=0.5)
                    self.play(r2[j].box.animate.set_stroke(MUTED), run_time=0.2)

        verdict = Text(f"return {result}", font=MONO, font_size=30, color=OK, weight=BOLD).move_to(status)
        self.play(goto("ret"), FadeOut(frame), Transform(status, verdict), run_time=0.5)
        return VGroup(r1, r2, l1, l2, bank, res_lab, taken, code, cursor, status)


class LessonVideo(Base):
    def construct(self):
        self.intro()
        header = Text("#350 · Intersection of Two Arrays II", font_size=22, color=MUTED).to_corner(UL, buff=0.35)
        self.add(header)
        self.problem_and_naive()

        with self.voice("idea"):
            idea = VGroup(
                Text("Cách 2 — đếm \"suất\"", font_size=44, color=CURRENT, weight=BOLD),
                Text("Counter(nums1): mỗi giá trị có bấy nhiêu suất", font_size=32),
                Text("gặp trong nums2 → còn suất thì lấy, trừ 1", font_size=32),
            ).arrange(DOWN, buff=0.35)
            self.play(FadeIn(idea, shift=UP * 0.3))
        self.play(FadeOut(idea), run_time=0.4)

        run = self.intersect_walkthrough(NUMS1, NUMS2, voiced=True)
        self.wait(0.8)
        self.play(FadeOut(run), run_time=0.5)

        self.code_python()
        self.code_java()
        self.follow_ups()
        self.complexity_and_outro()

    def intro(self):
        with self.voice("intro"):
            tag = Text("Bài 34 / 38 · Nhóm C — HashSet / Dictionary", font_size=26, color=ACCENT)
            title = Text("Intersection of Two Arrays II", font_size=62, weight=BOLD)
            sub = Text("LeetCode #350 · Easy", font_size=32, color=MUTED)
            group = VGroup(tag, title, sub).arrange(DOWN, buff=0.35).shift(UP * 0.4)
            self.play(FadeIn(tag), Write(title), run_time=1.4)
            self.play(FadeIn(sub, shift=UP * 0.2), run_time=0.6)
        self.play(FadeOut(group), run_time=0.5)

    def problem_and_naive(self):
        with self.voice("problem"):
            statement = VGroup(
                Text("Giao của 2 mảng — GIỮ số lần lặp", font_size=40),
                Text("xuất hiện ở cả hai bao nhiêu lần thì lấy bấy nhiêu", font_size=28, color=MUTED),
            ).arrange(DOWN, buff=0.25).to_edge(UP, buff=0.9)
            self.play(FadeIn(statement, shift=DOWN * 0.2))

        a = make_array([1, 2, 2, 1], side=0.95, buff=0.15, indexed=False).move_to(LEFT * 1.2 + UP * 0.5)
        b = make_array([2, 2], side=0.95, buff=0.15, indexed=False).next_to(a, DOWN, buff=0.5).align_to(a, LEFT)
        la = Text("nums1", font=MONO, font_size=24, color=MUTED).next_to(a, LEFT, buff=0.3)
        lb = Text("nums2", font=MONO, font_size=24, color=MUTED).next_to(b, LEFT, buff=0.3)
        with self.voice("example"):
            self.play(FadeIn(VGroup(a, b, la, lb)), run_time=0.6)
            ans = VGroup(
                Text("→ [2, 2]", font=MONO, font_size=34, color=OK),
                Text("#349 → [2]", font=MONO, font_size=26, color=MUTED),
            ).arrange(DOWN, buff=0.2, aligned_edge=LEFT).next_to(a, RIGHT, buff=0.8)
            self.play(*[c.box.animate.set_stroke(OK, width=5) for c in (a[1], a[2], b[0], b[1])], FadeIn(ans))

        with self.voice("naive"):
            self.play(FadeOut(ans), *[c.box.animate.set_stroke(ACCENT, width=3) for c in (a[1], a[2], b[0], b[1])],
                      run_time=0.3)
            used = set()
            for j, cell in enumerate(b):
                for i, target in enumerate(a):
                    if i in used:
                        continue
                    self.play(target.box.animate.set_stroke(CURRENT, width=5), run_time=0.22)
                    if target.value == cell.value:
                        used.add(i)
                        self.play(target.animate.set_opacity(0.25), cell.box.animate.set_stroke(OK, width=5), run_time=0.3)
                        break
                    self.play(target.box.animate.set_stroke(ACCENT, width=3), run_time=0.12)
            cost = Text("mỗi số của nums2 quét nums1 → O(n · m)", font=MONO, font_size=28, color=BAD)
            cost.next_to(b, DOWN, buff=0.6).set_x(0)
            self.play(FadeIn(cost))
        self.play(FadeOut(VGroup(statement, a, b, la, lb, cost)), run_time=0.5)

    def code_python(self):
        src = python_solution(PY_FILE)
        py = make_code(src, "python", font_size=28)
        py.scale_to_fit_width(min(py.width, 10.5)).move_to(UP * 0.5)
        path = Text("leetcode-38-bai/lc350-intersection-of-two-arrays-ii.py", font=MONO, font_size=20, color=MUTED)
        path.next_to(py, DOWN, buff=0.15)
        with self.voice("py_intro"):
            self.play(FadeIn(py), FadeIn(path))
        ln = {k: line_of(src, n) for k, n in {
            "counter": "counts = Counter(nums1)", "if": "if counts[num] > 0",
            "for": "for num in nums2", "ret": "return result",
        }.items()}
        cursor = highlight(py, ln["counter"])
        with self.voice("py_counter"):
            self.play(FadeIn(cursor))
        with self.voice("py_check"):
            self.play(cursor.animate.become(highlight(py, ln["if"], ln["if"] + 2)))
        with self.voice("py_order"):
            self.play(cursor.animate.become(highlight(py, ln["for"])))
        self.wait(0.3)
        self.play(FadeOut(VGroup(py, path, cursor)), run_time=0.5)

    def code_java(self):
        src = java_solution(JAVA_FILE)
        java = make_code(src, "java", font_size=24)
        java.scale_to_fit_width(8.7)
        if java.height > 5.5:
            java.scale_to_fit_height(5.5)
        java.move_to(LEFT * 2.25 + UP * 0.45)
        path = Text(".../groupc/IntersectionOfTwoArrays.java", font=MONO, font_size=18, color=MUTED)
        path.next_to(java, DOWN, buff=0.1)
        ln = {k: line_of(src, n) for k, n in {
            "merge": "remainingCount.merge(num, 1, Integer::sum)", "get": "getOrDefault(num, 0)",
            "put": "remainingCount.put(num, available - 1)", "stream": "stream().mapToInt",
        }.items()}
        right_x = RIGHT * 4.85
        with self.voice("java_intro"):
            cursor = highlight(java, ln["merge"])
            self.play(FadeIn(java), FadeIn(path), run_time=0.8)
            notes = VGroup(
                Text("merge(num, 1, Integer::sum)", font=MONO, font_size=20, color=CURRENT),
                Text("≈ Counter(nums1)", font=MONO, font_size=20, color=FG),
                Text("getOrDefault(num, 0)", font=MONO, font_size=20, color=CURRENT),
                Text("≈ counts[num]", font=MONO, font_size=20, color=FG),
            ).arrange(DOWN, buff=0.14, aligned_edge=LEFT).move_to(right_x + UP * 1.3)
            if notes.width > 3.9:
                notes.scale_to_fit_width(3.9)
            self.play(FadeIn(cursor), FadeIn(notes), run_time=0.5)
            self.play(cursor.animate.become(highlight(java, ln["get"])), run_time=0.5)

        with self.voice("java_put"):
            tail = VGroup(
                Text("put(num, available − 1)", font=MONO, font_size=20, color=CURRENT),
                Text("→ trừ 1 suất", font_size=20, color=FG),
                Text("List<Integer> → int[]", font=MONO, font_size=20, color=CURRENT),
                Text("bằng stream().mapToInt(...)", font=MONO, font_size=18, color=FG),
            ).arrange(DOWN, buff=0.14, aligned_edge=LEFT)
            plate = RoundedRectangle(width=tail.width + 0.5, height=tail.height + 0.4, corner_radius=0.15,
                                     fill_color="#020617", fill_opacity=0.95, stroke_color=CURRENT, stroke_width=2)
            box = VGroup(plate, tail.move_to(plate)).move_to(right_x + DOWN * 0.9)
            if box.width > 4.1:
                box.scale_to_fit_width(4.1)
            self.play(cursor.animate.become(highlight(java, ln["put"])), FadeIn(box, shift=UP * 0.2))
            self.play(cursor.animate.become(highlight(java, ln["stream"])), run_time=0.5)
        self.wait(0.4)
        self.play(FadeOut(VGroup(java, path, cursor, notes, box)), run_time=0.5)

    def follow_ups(self):
        title = Text("Follow-up hay gặp", font_size=34, color=CURRENT, weight=BOLD).to_edge(UP, buff=0.8)
        a_vals, b_vals = sorted(NUMS1), sorted(NUMS2)
        a = make_array(a_vals, side=0.75, buff=0.12, indexed=False).move_to(LEFT * 2.4 + UP * 1.35)
        b = make_array(b_vals, side=0.75, buff=0.12, indexed=False).move_to(LEFT * 2.0 + UP * 0.2)
        b.align_to(a, LEFT)
        la = Text("sorted nums1", font=MONO, font_size=18, color=MUTED).next_to(a, LEFT, buff=0.25)
        lb = Text("sorted nums2", font=MONO, font_size=18, color=MUTED).next_to(b, LEFT, buff=0.25)
        with self.voice("fu_sorted"):
            self.play(FadeIn(title), FadeIn(VGroup(a, b, la, lb)), run_time=0.6)
            pi = SurroundingRectangle(a[0], color=CURRENT, buff=0.05, stroke_width=4)
            pj = SurroundingRectangle(b[0], color=CURRENT, buff=0.05, stroke_width=4)
            self.play(Create(pi), Create(pj), run_time=0.3)
            i = j = 0
            got = []
            while i < len(a_vals) and j < len(b_vals):
                if a_vals[i] == b_vals[j]:
                    got.append(a_vals[i])
                    self.play(a[i].box.animate.set_stroke(OK, width=5), b[j].box.animate.set_stroke(OK, width=5),
                              run_time=0.3)
                    i, j = i + 1, j + 1
                elif a_vals[i] < b_vals[j]:
                    i += 1
                else:
                    j += 1
                moves = []
                if i < len(a_vals):
                    moves.append(pi.animate.move_to(a[i]))
                if j < len(b_vals):
                    moves.append(pj.animate.move_to(b[j]))
                if moves:
                    self.play(*moves, run_time=0.3)
            out = Text(f"→ {got}   bộ nhớ thêm O(1)", font=MONO, font_size=26, color=OK).next_to(a, RIGHT, buff=0.6)
            self.play(FadeOut(pi), FadeOut(pj), FadeIn(out), run_time=0.4)

        rows = [
            ("Tình huống", "Cách", FG),
            ("1 mảng nhỏ hơn nhiều", "đếm mảng NHỎ → O(min(n, m))", CURRENT),
            ("nums2 quá lớn, trên đĩa", "Counter mảng nhỏ trong RAM, đọc nums2 từng khúc", ACCENT),
        ]
        table = VGroup()
        for r, (situ, how, color) in enumerate(rows):
            row = VGroup(Text(situ, font_size=26, color=color), Text(how, font_size=24, color=color))
            y = -0.95 - r * 0.62
            row[0].move_to([0, y, 0]).align_to([-6.4, 0, 0], LEFT)
            row[1].move_to([0, y, 0]).align_to([-2.2, 0, 0], LEFT)
            table.add(row)
        with self.voice("fu_small"):
            self.play(FadeIn(table[0]), FadeIn(table[1], shift=RIGHT * 0.2), run_time=0.6)
        with self.voice("fu_disk"):
            self.play(FadeIn(table[2], shift=RIGHT * 0.2), run_time=0.6)
        self.wait(0.3)
        self.play(FadeOut(VGroup(title, a, b, la, lb, out, table)), run_time=0.5)

    def complexity_and_outro(self):
        with self.voice("complexity"):
            cards = VGroup()
            for label, value, note in (("Time", "O(n + m)", "đếm nums1 + duyệt nums2"),
                                       ("Space", "O(n)", "theo nums1\nđếm mảng nhỏ → O(min(n, m))")):
                box = RoundedRectangle(width=5.0, height=2.5, corner_radius=0.25, stroke_color=ACCENT, stroke_width=3)
                content = VGroup(
                    Text(label, font_size=30, color=MUTED),
                    Text(value, font_size=56, color=FG, weight=BOLD),
                    Text(note, font_size=22, color=MUTED, line_spacing=0.8),
                ).arrange(DOWN, buff=0.18).move_to(box)
                cards.add(VGroup(box, content))
            cards.arrange(RIGHT, buff=0.7).move_to(UP * 0.3)
            self.play(FadeIn(cards, shift=UP * 0.3))
        self.play(FadeOut(cards), run_time=0.4)

        with self.voice("outro", pad=1.2):
            nxt = VGroup(
                Text("Tiếp theo", font_size=28, color=MUTED),
                Text("Bài 35 · Happy Number", font_size=52, weight=BOLD),
                Text("LeetCode #202 · HashSet Cycle Detection", font_size=28, color=ACCENT),
            ).arrange(DOWN, buff=0.3)
            self.play(FadeIn(nxt, shift=UP * 0.2))


class LessonGif(Base):
    VOICE = False

    def construct(self):
        title = Text("Intersection II · Counter = số suất", font_size=24, color=MUTED).to_corner(UL, buff=0.35)
        self.add(title)
        run = self.intersect_walkthrough(NUMS1, NUMS2, voiced=False)
        self.wait(1.5)
        self.play(FadeOut(run), run_time=0.4)
