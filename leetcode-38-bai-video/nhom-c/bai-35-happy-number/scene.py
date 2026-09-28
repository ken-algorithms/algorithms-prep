"""Bài 35 — Happy Number (#202). Code hiển thị được đọc trực tiếp từ file nguồn Python/Java.

Trọng tâm: phát hiện chu trình bằng HashSet. Dãy của n = 2 vẽ thành hình ρ (đuôi 2 + vòng 8 số);
follow-up rùa–thỏ (Floyd) chạy trên chính hình đó, vị trí gặp nhau tính bằng code thật.

    ./build.sh            # video đầy đủ (LessonVideo) + GIF (LessonGif)
"""

import math
import sys
from contextlib import nullcontext
from pathlib import Path

from manim import (
    BOLD, DOWN, LEFT, RIGHT, UL, UP,
    Arrow, Circle, Create, FadeIn, FadeOut, Indicate, Rectangle, RoundedRectangle,
    Text, Transform, VGroup, Write,
)

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / "common"))
from kit import (  # noqa: E402
    ACCENT, BAD, BG, CURRENT, FG, JAVA_DIR, MONO, MUTED, OK, PY_DIR, Lesson,
    highlight, java_solution, line_of, make_code, python_body, python_solution,
)

PY_FILE = PY_DIR / "lc202-happy-number.py"
JAVA_FILE = JAVA_DIR / "groupc" / "HappyNumber.java"


def f(n: int) -> int:
    return sum(int(d) ** 2 for d in str(n))


def calc(n: int) -> str:
    return " + ".join(f"{d}²" for d in str(n)) + f" = {f(n)}"


def trail(n: int) -> tuple[list[int], int]:
    """Đúng vòng while của Solution.is_happy: các số được thêm vào seen, và số làm vòng dừng."""
    seen, seq = set(), []
    while n != 1 and n not in seen:
        seen.add(n)
        seq.append(n)
        n = f(n)
    return seq, n


class Node(VGroup):
    def __init__(self, value: int, radius: float = 0.42):
        super().__init__()
        self.value = value
        self.circle = Circle(radius=radius, stroke_color=ACCENT, stroke_width=3).set_fill(BG, opacity=1)
        self.label = Text(str(value), font=MONO, font_size=24 if value < 100 else 20).move_to(self.circle)
        self.add(self.circle, self.label)


def link(a: Node, b: Node, color: str = MUTED) -> Arrow:
    return Arrow(a.get_center(), b.get_center(), buff=0.45, color=color, stroke_width=4,
                 max_tip_length_to_length_ratio=0.18)


def rho(tail: list[int], cycle: list[int], center, radius: float = 1.85) -> dict[int, Node]:
    """Vòng `cycle` trên đường tròn (bắt đầu ở điểm trái nhất, đi theo chiều kim đồng hồ), `tail` nối vào bên trái."""
    nodes = {}
    for k, v in enumerate(cycle):
        angle = math.pi - k * 2 * math.pi / len(cycle)
        nodes[v] = Node(v).move_to(center + radius * (RIGHT * math.cos(angle) + UP * math.sin(angle)))
    for i, v in enumerate(reversed(tail)):
        nodes[v] = Node(v).move_to(center + LEFT * (radius + 1.45 * (i + 1)))
    return nodes


class Base(Lesson):
    LESSON_DIR = HERE


class LessonVideo(Base):
    def construct(self):
        self.intro()
        header = Text("#202 · Happy Number", font_size=22, color=MUTED).to_corner(UL, buff=0.35)
        self.add(header)
        self.problem_and_examples()
        self.why_bounded()

        with self.voice("idea"):
            idea = VGroup(
                Text("Phát hiện chu trình", font_size=44, color=CURRENT, weight=BOLD),
                Text("lưu mọi số đã gặp vào HashSet seen", font_size=32),
                Text("gặp lại số cũ → đang lặp → false", font_size=32, color=BAD),
            ).arrange(DOWN, buff=0.35)
            self.play(FadeIn(idea, shift=UP * 0.3))
        self.play(FadeOut(idea), run_time=0.4)

        parts = self.cycle_walkthrough(voiced=True)
        self.floyd_and_trick(parts)
        self.code_python()
        self.code_java()
        self.complexity_and_outro()

    def cycle_walkthrough(self, voiced: bool):
        """n = 2: chạy đúng vòng while — thêm vào seen, tính f, cho tới khi gặp lại 4."""
        seq, stop = trail(2)
        cut = seq.index(stop)
        center = LEFT * 2.6 + UP * 0.25
        nodes = rho(seq[:cut], seq[cut:], center)
        say = (lambda k: self.voice(k)) if voiced else (lambda k: nullcontext())

        body = python_body(PY_FILE, "is_happy")
        code = make_code(body, "python", font_size=24)
        code.scale_to_fit_width(min(code.width, 5.9)).move_to(RIGHT * 3.75 + UP * 1.85)
        ln = {k: line_of(body, s) for k, s in {
            "while": "while n != 1 and n not in seen", "add": "seen.add(n)",
            "f": "n = sum(int(d) ** 2 for d in str(n))", "ret": "return n == 1",
        }.items()}
        cursor = highlight(code, ln["while"])
        seen_box = RoundedRectangle(width=5.6, height=1.55, corner_radius=0.18, stroke_color=CURRENT, stroke_width=3)
        seen_box.move_to(RIGHT * 3.75 + DOWN * 1.2)
        seen_title = Text("seen", font=MONO, font_size=22, color=CURRENT).next_to(seen_box, UP, buff=0.1)
        seen_title.align_to(seen_box, LEFT)
        calc_t = Text("·", font=MONO, font_size=26).set_opacity(0).move_to(RIGHT * 3.75 + UP * 0.2)
        self.play(*[FadeIn(nd) for nd in nodes.values()], FadeIn(code), FadeIn(cursor),
                  FadeIn(seen_box), FadeIn(seen_title), run_time=0.9)
        self.add(calc_t)

        def goto(key):
            return cursor.animate.become(highlight(code, ln[key]))

        def chip_pos(i: int):
            return seen_box.get_corner(UL) + RIGHT * (0.55 + 0.98 * (i % 5)) + DOWN * (0.42 + 0.66 * (i // 5))

        chips, arrows = {}, VGroup()

        def step(i: int, x: int, speed: float):
            node = nodes[x]
            self.play(goto("while"), node.circle.animate.set_stroke(CURRENT, width=6), run_time=0.35 * speed)
            chip = Text(str(x), font=MONO, font_size=24, color=FG).move_to(chip_pos(i))
            chips[x] = chip
            self.play(goto("add"), node.circle.animate.set_fill(OK, opacity=0.22).set_stroke(OK, width=3),
                      FadeIn(chip, shift=DOWN * 0.15), run_time=0.35 * speed)
            arrow = link(node, nodes[f(x)])
            arrows.add(arrow)
            new_calc = Text(f"f({x}) = {calc(x)}", font=MONO, font_size=26, color=FG).move_to(calc_t)
            if new_calc.width > 5.8:
                new_calc.scale_to_fit_width(5.8)
            self.play(goto("f"), Transform(calc_t, new_calc), Create(arrow), run_time=0.45 * speed)

        with say("h0"):
            step(0, seq[0], 1.0)
        with say("h_mid"):
            for i in range(1, len(seq)):
                step(i, seq[i], 0.62)
        with say("h_end"):
            self.play(goto("while"), Indicate(nodes[stop], color=BAD, scale_factor=1.35),
                      Indicate(chips[stop], color=BAD, scale_factor=1.6), run_time=0.7)
            self.play(nodes[stop].circle.animate.set_stroke(BAD, width=6), chips[stop].animate.set_color(BAD),
                      run_time=0.3)
            verdict = Text(f"{stop} ∈ seen → return False", font=MONO, font_size=28, color=BAD, weight=BOLD)
            verdict.move_to(calc_t)
            self.play(goto("ret"), Transform(calc_t, verdict), run_time=0.5)
        self.wait(0.6)
        side = VGroup(code, cursor, seen_box, seen_title, calc_t, *chips.values())
        self.play(FadeOut(side), run_time=0.5)
        for nd in nodes.values():
            nd.circle.set_stroke(ACCENT, width=3).set_fill(BG, opacity=1)
        return seq, stop, nodes, arrows

    def floyd_and_trick(self, parts):
        seq, stop, nodes, arrows = parts
        diagram = VGroup(*nodes.values(), arrows)
        self.play(diagram.animate.shift(RIGHT * 2.3), run_time=0.6)
        with self.voice("floyd"):
            tag_r = Text("R", font=MONO, font_size=24, color=OK, weight=BOLD)
            tag_t = Text("T", font=MONO, font_size=24, color=CURRENT, weight=BOLD)
            legend = VGroup(
                Text("R = rùa: 1 bước", font_size=24, color=OK),
                Text("T = thỏ: 2 bước", font_size=24, color=CURRENT),
            ).arrange(DOWN, buff=0.15, aligned_edge=LEFT).to_corner(UL, buff=0.35).shift(DOWN * 0.6)
            slow, fast = 2, f(2)
            tag_r.next_to(nodes[slow], UP, buff=0.05)
            tag_t.next_to(nodes[fast], DOWN, buff=0.05)
            self.play(FadeIn(legend), FadeIn(tag_r), FadeIn(tag_t), run_time=0.5)
            while fast != 1 and slow != fast:
                slow, fast = f(slow), f(f(fast))
                self.play(tag_r.animate.next_to(nodes[slow], UP, buff=0.05),
                          tag_t.animate.next_to(nodes[fast], DOWN, buff=0.05), run_time=0.45)
            met = VGroup(
                Text(f"gặp nhau ở {fast}", font=MONO, font_size=28, color=BAD),
                Text(f"{fast} ≠ 1 → false", font=MONO, font_size=28, color=BAD),
                Text("bộ nhớ O(1)", font=MONO, font_size=26, color=OK),
            ).arrange(DOWN, buff=0.18, aligned_edge=LEFT).move_to(RIGHT * 4.9 + UP * 0.3)
            self.play(Indicate(nodes[fast], color=BAD, scale_factor=1.35), FadeIn(met), run_time=0.7)
        with self.voice("trick4"):
            self.play(FadeOut(met), FadeOut(tag_r), FadeOut(tag_t), FadeOut(legend), run_time=0.3)
            four = nodes[4]
            self.play(four.circle.animate.set_stroke(CURRENT, width=7), Indicate(four, color=CURRENT, scale_factor=1.3))
            note = VGroup(
                Text("mọi số không vui", font_size=28, color=CURRENT),
                Text("→ rơi vào vòng chứa 4", font_size=28, color=CURRENT),
                Text("(đã kiểm tra n ≤ 100.000)", font_size=22, color=MUTED),
            ).arrange(DOWN, buff=0.15, aligned_edge=LEFT).move_to(RIGHT * 4.9 + UP * 0.3)
            self.play(FadeIn(note))
        self.wait(0.3)
        self.play(FadeOut(VGroup(diagram, note)), run_time=0.5)

    def intro(self):
        with self.voice("intro"):
            tag = Text("Bài 35 / 38 · Nhóm C — HashSet / Dictionary", font_size=26, color=ACCENT)
            title = Text("Happy Number", font_size=72, weight=BOLD)
            sub = Text("LeetCode #202 · Easy", font_size=32, color=MUTED)
            group = VGroup(tag, title, sub).arrange(DOWN, buff=0.35).shift(UP * 0.4)
            self.play(FadeIn(tag), Write(title), run_time=1.4)
            self.play(FadeIn(sub, shift=UP * 0.2), run_time=0.6)
        self.play(FadeOut(group), run_time=0.5)

    def problem_and_examples(self):
        with self.voice("problem"):
            statement = VGroup(
                Text("n → tổng bình phương các chữ số → lặp lại", font_size=38),
                Text("về được 1 → happy number", font_size=30, color=OK),
            ).arrange(DOWN, buff=0.25).to_edge(UP, buff=0.9)
            self.play(FadeIn(statement, shift=DOWN * 0.2))

        chain_vals, _ = trail(19)
        chain_vals = chain_vals + [1]
        chain = VGroup(*[Node(v) for v in chain_vals]).arrange(RIGHT, buff=1.75).move_to(UP * 0.1)
        chain[-1].circle.set_stroke(OK, width=5)
        with self.voice("example"):
            self.play(FadeIn(chain[0]), run_time=0.4)
            labels = VGroup()
            for a, b in zip(chain[:-1], chain[1:]):
                arrow = link(a, b, ACCENT)
                lab = Text("+".join(f"{d}²" for d in str(a.value)), font=MONO, font_size=18, color=MUTED)
                lab.next_to(arrow, DOWN, buff=0.12)
                labels.add(arrow, lab)
                self.play(Create(arrow), FadeIn(lab), FadeIn(b), run_time=0.55)
            ok = Text("→ true", font=MONO, font_size=32, color=OK).next_to(chain, DOWN, buff=0.8)
            self.play(FadeIn(ok))

        with self.voice("unhappy"):
            self.play(FadeOut(VGroup(chain, labels, ok)), run_time=0.4)
            seq, stop = trail(2)
            parts = [Text(str(v), font=MONO, font_size=30, color=CURRENT if v == stop else FG) for v in seq]
            parts.append(Text(f"{stop} …", font=MONO, font_size=30, color=BAD))
            line = VGroup()
            for i, p in enumerate(parts):
                if i:
                    line.add(Text("→", font=MONO, font_size=26, color=MUTED))
                line.add(p)
            line.arrange(RIGHT, buff=0.16).move_to(UP * 0.1)
            if line.width > 13:
                line.scale_to_fit_width(13)
            self.play(FadeIn(line, lag_ratio=0.1), run_time=2.2)
            no = Text("quay lại 4 → lặp mãi → false", font=MONO, font_size=30, color=BAD).next_to(line, DOWN, buff=0.6)
            self.play(FadeIn(no))
        self.play(FadeOut(VGroup(statement, line, no)), run_time=0.5)

    def why_bounded(self):
        with self.voice("bound"):
            title = Text("Vì sao dãy không tăng mãi?", font_size=38, color=CURRENT).to_edge(UP, buff=0.9)
            rows = [
                ("số chữ số", "số nhỏ nhất", "tổng lớn nhất", FG),
                ("3", "100", "3 · 81 = 243", ACCENT),
                ("4", "1 000", "4 · 81 = 324  < 1 000 → giảm", OK),
                ("13", "10¹²", "13 · 81 = 1 053 → giảm", OK),
            ]
            table = VGroup()
            for r, (a, b, c, color) in enumerate(rows):
                row = VGroup(Text(a, font_size=28, color=color), Text(b, font_size=28, color=color),
                             Text(c, font_size=28, color=color))
                for col, (item, x) in enumerate(zip(row, (-4.6, -1.9, 2.4))):
                    item.move_to([x, 1.5 - r * 0.8, 0])
                table.add(row)
            rule = Rectangle(width=12, height=0.02, stroke_width=0, fill_color=MUTED, fill_opacity=0.6).move_to([0, 1.1, 0])
            self.play(FadeIn(title), FadeIn(table[0]), FadeIn(rule), run_time=0.5)
            for row in table[1:]:
                self.play(FadeIn(row, shift=RIGHT * 0.2), run_time=0.6)
            end = Text("→ sau vài bước dãy luôn ≤ 243: hữu hạn giá trị → về 1 hoặc lặp", font_size=28, color=CURRENT)
            end.move_to(DOWN * 1.9)
            self.play(FadeIn(end))
        self.play(FadeOut(VGroup(title, table, rule, end)), run_time=0.5)

    def code_python(self):
        src = python_solution(PY_FILE)
        py = make_code(src, "python", font_size=28)
        py.scale_to_fit_width(min(py.width, 10.5)).move_to(UP * 0.5)
        path = Text("leetcode-38-bai/lc202-happy-number.py", font=MONO, font_size=20, color=MUTED)
        path.next_to(py, DOWN, buff=0.15)
        with self.voice("py_intro"):
            self.play(FadeIn(py), FadeIn(path))
        ln_while = line_of(src, "while n != 1 and n not in seen")
        ln_f = line_of(src, "n = sum(int(d) ** 2 for d in str(n))")
        cursor = highlight(py, ln_while)
        with self.voice("py_while"):
            self.play(FadeIn(cursor))
        with self.voice("py_digits"):
            self.play(cursor.animate.become(highlight(py, ln_f)))
        self.wait(0.3)
        self.play(FadeOut(VGroup(py, path, cursor)), run_time=0.5)

    def code_java(self):
        src = java_solution(JAVA_FILE)
        java = make_code(src, "java", font_size=24)
        java.scale_to_fit_height(min(java.height, 5.6))
        if java.width > 6.8:
            java.scale_to_fit_width(6.8)
        java.move_to(RIGHT * 3.4 + UP * 0.4)
        path = Text(".../groupc/HappyNumber.java", font=MONO, font_size=18, color=MUTED).next_to(java, DOWN, buff=0.1)
        ln = {k: line_of(src, s) for k, s in {
            "while": "while (current != 1 && seen.add(current))", "helper": "private int sumOfSquaredDigits",
            "digit": "int digit = number % 10", "div": "number /= 10",
        }.items()}
        left_x = LEFT * 3.7
        with self.voice("java_intro"):
            cursor = highlight(java, ln["helper"], ln["helper"] + 8)
            self.play(FadeIn(java), FadeIn(path), run_time=0.8)
            self.play(FadeIn(cursor), run_time=0.4)
        with self.voice("java_add"):
            note = VGroup(
                Text("while (current != 1", font=MONO, font_size=22, color=CURRENT),
                Text("       && seen.add(current))", font=MONO, font_size=22, color=CURRENT),
                Text("add() = false khi đã có → dừng", font_size=22, color=FG),
            ).arrange(DOWN, buff=0.12, aligned_edge=LEFT).move_to(left_x + UP * 1.3)
            self.play(cursor.animate.become(highlight(java, ln["while"])), FadeIn(note))
        with self.voice("java_digits"):
            digits = VGroup(
                Text("145 % 10 = 5   145 / 10 = 14", font=MONO, font_size=22, color=FG),
                Text(" 14 % 10 = 4    14 / 10 = 1", font=MONO, font_size=22, color=FG),
                Text("  1 % 10 = 1     1 / 10 = 0", font=MONO, font_size=22, color=FG),
                Text("5² + 4² + 1² = 42", font=MONO, font_size=24, color=OK),
            ).arrange(DOWN, buff=0.12, aligned_edge=LEFT)
            plate = RoundedRectangle(width=digits.width + 0.5, height=digits.height + 0.4, corner_radius=0.15,
                                     fill_color="#020617", fill_opacity=0.95, stroke_color=CURRENT, stroke_width=2)
            box = VGroup(plate, digits.move_to(plate)).move_to(left_x + DOWN * 0.95)
            if box.width > 6.0:
                box.scale_to_fit_width(6.0)
            self.play(cursor.animate.become(highlight(java, ln["digit"], ln["div"])), FadeIn(box, shift=UP * 0.2))
        self.wait(0.4)
        self.play(FadeOut(VGroup(java, path, cursor, note, box)), run_time=0.5)

    def complexity_and_outro(self):
        with self.voice("complexity"):
            cards = VGroup()
            for label, value, note in (("Time", "O(log n)", "mỗi bước ~ số chữ số\nsố bước bị chặn"),
                                       ("Space", "O(log n)", "cho seen\nFloyd: O(1)")):
                box = RoundedRectangle(width=4.6, height=2.7, corner_radius=0.25, stroke_color=ACCENT, stroke_width=3)
                content = VGroup(
                    Text(label, font_size=30, color=MUTED),
                    Text(value, font_size=56, color=FG, weight=BOLD),
                    Text(note, font_size=22, color=MUTED, line_spacing=0.8),
                ).arrange(DOWN, buff=0.18).move_to(box)
                cards.add(VGroup(box, content))
            cards.arrange(RIGHT, buff=0.8).move_to(UP * 0.3)
            self.play(FadeIn(cards, shift=UP * 0.3))
        self.play(FadeOut(cards), run_time=0.4)
        with self.voice("outro", pad=1.2):
            nxt = VGroup(
                Text("Tiếp theo", font_size=28, color=MUTED),
                Text("Bài 36 · 4Sum II", font_size=56, weight=BOLD),
                Text("LeetCode #454 · HashMap", font_size=28, color=ACCENT),
            ).arrange(DOWN, buff=0.3)
            self.play(FadeIn(nxt, shift=UP * 0.2))


class LessonGif(LessonVideo):
    VOICE = False
    HOLD_WITHOUT_VOICE = 0.5

    def construct(self):
        title = Text("Happy Number · HashSet phát hiện chu trình", font_size=24, color=MUTED).to_corner(UL, buff=0.35)
        self.add(title)
        seq, stop, nodes, arrows = self.cycle_walkthrough(voiced=False)
        self.wait(1.2)
        self.play(FadeOut(VGroup(*nodes.values(), arrows)), run_time=0.4)
