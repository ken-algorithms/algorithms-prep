"""Bài 33 — Subarray Sum Equals K (#560). Code hiển thị được đọc trực tiếp từ file nguồn Python/Java.

Trọng tâm: tổng đoạn = k ⇔ P[i] = P − k → đếm P − k đã gặp bằng HashMap; {0: 1}; tra trước ghi sau.
Bảng prefix_count vẽ ĐÚNG như defaultdict chạy thật — kể cả key rác (−2 → 0, 2 → 0) do đọc bằng [x].

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
    highlight, java_solution, line_of, make_array, make_code, python_body, python_solution,
)

PY_FILE = PY_DIR / "lc560-subarray-sum-equals-k.py"
JAVA_FILE = JAVA_DIR / "groupc" / "SubarraySumEqualsK.java"
NUMS, K = [1, 2, 1, -1, 2, 1], 3


def span(row: VGroup, first: int, last: int, level: int, color: str = OK) -> Line:
    """Vạch trên đầu các ô first…last — mỗi mảng con tìm được một vạch, xếp tầng cho khỏi đè nhau."""
    y = row.get_top()[1] + 0.2 + 0.2 * level
    return Line([row[first].get_left()[0] + 0.06, y, 0], [row[last].get_right()[0] - 0.06, y, 0],
                color=color, stroke_width=7)


def prefix_row(row: VGroup, values: list[int], side: float = 0.62) -> VGroup:
    """P[0] nằm trước ô đầu tiên; P[j] (j ≥ 1) nằm ngay dưới phần tử j − 1 = tổng tính tới hết phần tử đó."""
    step = row[1].get_x() - row[0].get_x()
    cells = VGroup()
    for j, v in enumerate(values):
        cell = Cell(v, side=side)
        cell.box.set_stroke(CURRENT, width=2)
        x = row[0].get_x() - step if j == 0 else row[j - 1].get_x()
        cells.add(cell.move_to([x, row.get_bottom()[1] - 0.25 - side / 2, 0]))
    return cells


class CountMap(VGroup):
    """prefix_count vẽ thành bảng 'tổng → số lần', thứ tự dòng = thứ tự key được tạo (như dict)."""

    def __init__(self, title: str, width: float = 2.3, height: float = 3.7):
        super().__init__()
        self.box = RoundedRectangle(width=width, height=height, corner_radius=0.18, stroke_color=CURRENT, stroke_width=3)
        self.title = Text(title, font=MONO, font_size=22, color=CURRENT).next_to(self.box, UP, buff=0.12)
        self.rows: dict[int, Text] = {}
        self.add(self.box, self.title)

    def _text(self, key: int, count: int, color: str) -> Text:
        return Text(f"{key:>2} → {count}", font=MONO, font_size=26, color=color)

    def put(self, key: int, count: int, color: str = FG):
        """Tạo dòng mới hoặc đổi số đếm của dòng cũ; trả về animation."""
        if key in self.rows:
            new = self._text(key, count, color).move_to(self.rows[key])
            return Transform(self.rows[key], new)
        row = self._text(key, count, color)
        row.move_to(self.box.get_top() + DOWN * (0.34 + 0.42 * len(self.rows)))
        self.rows[key] = row
        self.add(row)
        return FadeIn(row, shift=DOWN * 0.15)


class Base(Lesson):
    LESSON_DIR = HERE

    def prefix_walkthrough(self, nums: list[int], k: int, voiced: bool):
        """Chạy đúng Solution.subarray_sum (defaultdict) trên nums, vẽ từng bước."""
        say = (lambda j: self.voice(f"s{j}")) if voiced else (lambda j: nullcontext())
        row = make_array(nums, side=0.72, buff=0.1, indexed=False).move_to(LEFT * 2.95 + UP * 1.75)
        row_lab = Text("nums", font=MONO, font_size=20, color=MUTED).next_to(row, LEFT, buff=0.2)
        prefixes = [0]
        for x in nums:
            prefixes.append(prefixes[-1] + x)
        p_row = prefix_row(row, prefixes)
        p_lab = Text("P", font=MONO, font_size=24, color=CURRENT).next_to(p_row[0], LEFT, buff=0.2)
        for cell in p_row[1:]:
            cell.set_opacity(0)

        pmap = CountMap("prefix_count").move_to(RIGHT * 1.0 + DOWN * 0.1)
        body = python_body(PY_FILE, "subarray_sum")
        code = make_code(body, "python", font_size=24)
        code.scale_to_fit_width(min(code.width, 4.75)).move_to(RIGHT * 4.6 + UP * 0.35)
        ln = {key: line_of(body, n) for key, n in {
            "init": "prefix_count[0] = 1", "for": "for num in nums", "psum": "prefix_sum += num",
            "res": "result += prefix_count[prefix_sum - k]", "put": "prefix_count[prefix_sum] += 1",
            "ret": "return result",
        }.items()}
        cursor = highlight(code, ln["init"])

        eq1 = Text("·", font=MONO, font_size=26).set_opacity(0).move_to(LEFT * 3.25 + DOWN * 0.35)
        eq2 = Text("·", font=MONO, font_size=26).set_opacity(0).move_to(LEFT * 3.25 + DOWN * 0.95)
        result_t = Text("result = 0", font=MONO, font_size=30, color=FG).move_to(LEFT * 3.25 + DOWN * 1.8)

        self.play(FadeIn(row), FadeIn(row_lab), FadeIn(p_row[0]), FadeIn(p_lab), FadeIn(pmap), FadeIn(code),
                  FadeIn(cursor), FadeIn(result_t), run_time=0.9)
        counts = {0: 1}
        self.play(pmap.put(0, 1), run_time=0.4)
        self.add(eq1, eq2)

        def goto(key):
            return cursor.animate.become(highlight(code, ln[key]))

        def text_at(target, s, color):
            new = Text(s if s.strip() else "·", font=MONO, font_size=26, color=color).move_to(target)
            return Transform(target, new if s.strip() else new.set_opacity(0))

        p, result, frame, lines = 0, 0, None, VGroup()
        for j, num in enumerate(nums):
            with say(j):
                box = SurroundingRectangle(row[j], color=CURRENT, buff=0.06, stroke_width=4)
                self.play(Create(box) if frame is None else Transform(frame, box), goto("for"), run_time=0.35)
                frame = frame or box
                p += num
                p_cell = p_row[j + 1]
                self.play(goto("psum"), p_cell.animate.set_opacity(1), text_at(eq1, f"P = {p}   cần P − k = {p} − {k} = {p - k}", FG),
                          text_at(eq2, " ", MUTED), run_time=0.45)

                target = p - k
                found = counts.get(target, 0)
                if found:
                    starts = [i for i in range(j + 1) if prefixes[i] == target]
                    new_lines = [span(row, i, j, len(lines) + n) for n, i in enumerate(starts)]
                    self.play(goto("res"), Indicate(pmap.rows[target], color=OK, scale_factor=1.3),
                              *[Indicate(p_row[i], color=OK, scale_factor=1.25) for i in starts], run_time=0.55)
                    result += found
                    self.play(*[Create(ln_) for ln_ in new_lines],
                              text_at(eq2, f"map[{target}] = {found} → result += {found}", OK),
                              text_at(result_t, f"result = {result}", OK), run_time=0.55)
                    lines.add(*new_lines)
                else:
                    anims = [goto("res"), text_at(eq2, f"{target} chưa gặp → +0", MUTED)]
                    if target not in counts:
                        counts[target] = 0          # defaultdict: đọc [x] là tạo key x = 0
                        anims.append(pmap.put(target, 0, MUTED))
                    self.play(*anims, run_time=0.5)

                counts[p] = counts.get(p, 0) + 1
                self.play(goto("put"), pmap.put(p, counts[p], FG), run_time=0.45)

        verdict = Text(f"return result = {result}", font=MONO, font_size=30, color=OK, weight=BOLD).move_to(result_t)
        self.play(goto("ret"), FadeOut(frame), Transform(result_t, verdict), run_time=0.5)
        return VGroup(row, row_lab, p_row, p_lab, pmap, code, cursor, eq1, eq2, result_t, lines)


class LessonVideo(Base):
    def construct(self):
        self.intro()
        header = Text("#560 · Subarray Sum Equals K", font_size=22, color=MUTED).to_corner(UL, buff=0.35)
        self.add(header)
        self.problem_brute_window()
        self.prefix_idea()

        run = self.prefix_walkthrough(NUMS, K, voiced=True)
        self.wait(0.8)
        self.play(FadeOut(run), run_time=0.5)

        self.code_python()
        self.code_java()
        self.complexity_and_tradeoff()

    def intro(self):
        with self.voice("intro"):
            tag = Text("Bài 33 / 38 · Nhóm C — HashSet / Dictionary", font_size=26, color=ACCENT)
            title = Text("Subarray Sum Equals K", font_size=68, weight=BOLD)
            sub = Text("LeetCode #560 · Medium", font_size=32, color=MUTED)
            group = VGroup(tag, title, sub).arrange(DOWN, buff=0.35).shift(UP * 0.4)
            self.play(FadeIn(tag), Write(title), run_time=1.4)
            self.play(FadeIn(sub, shift=UP * 0.2), run_time=0.6)
        self.play(FadeOut(group), run_time=0.5)

    def problem_brute_window(self):
        with self.voice("problem"):
            statement = VGroup(
                Text("Có bao nhiêu mảng con LIÊN TIẾP có tổng = k?", font_size=38),
                Text("mảng có thể có số âm", font_size=26, color=MUTED),
            ).arrange(DOWN, buff=0.25).to_edge(UP, buff=0.9)
            self.play(FadeIn(statement, shift=DOWN * 0.2))

        row = make_array([1, 1, 1], side=1.0, buff=0.2).move_to(DOWN * 0.1)
        k_lab = Text("k = 2", font=MONO, font_size=30, color=CURRENT).next_to(row, RIGHT, buff=0.6)
        with self.voice("example"):
            self.play(FadeIn(row), FadeIn(k_lab), run_time=0.6)
            a, b = span(row, 0, 1, 0), span(row, 1, 2, 1)
            self.play(Create(a), run_time=0.5)
            self.play(Create(b), run_time=0.5)
            ans = Text("→ 2", font=MONO, font_size=34, color=OK).next_to(row, DOWN, buff=0.5)
            self.play(FadeIn(ans))

        with self.voice("brute"):
            self.play(FadeOut(VGroup(a, b, ans)), run_time=0.3)
            window = None
            for i in range(3):
                for j in range(i, 3):
                    total = j - i + 1
                    box = SurroundingRectangle(VGroup(*row[i:j + 1]), buff=0.08, stroke_width=4,
                                               color=OK if total == 2 else MUTED)
                    tag = Text(f"sum = {total}", font=MONO, font_size=26, color=OK if total == 2 else MUTED)
                    tag.next_to(row, DOWN, buff=0.45)
                    if window is None:
                        window = VGroup(box, tag)
                        self.play(Create(box), FadeIn(tag), run_time=0.35)
                    else:
                        self.play(Transform(window, VGroup(box, tag)), run_time=0.35)
            cost = Text("n(n+1)/2 mảng con → O(n²)", font=MONO, font_size=28, color=BAD).next_to(window, DOWN, buff=0.3)
            self.play(FadeIn(cost))
        self.play(FadeOut(VGroup(row, k_lab, window, cost)), run_time=0.4)

        neg = make_array([1, -1, 1], side=1.0, buff=0.2).move_to(DOWN * 0.1)
        k1 = Text("k = 1", font=MONO, font_size=30, color=CURRENT).next_to(neg, RIGHT, buff=0.6)
        with self.voice("window"):
            self.play(FadeIn(neg), FadeIn(k1), run_time=0.5)
            hits = [span(neg, 0, 0, 0), span(neg, 0, 2, 1), span(neg, 2, 2, 0)]
            self.play(*[Create(h) for h in hits], run_time=0.7)
            note = VGroup(
                Text("3 đáp án — có cả [1, −1, 1]", font=MONO, font_size=26, color=OK),
                Text("số âm → tổng lúc tăng lúc giảm → sliding window ✗", font_size=28, color=BAD),
            ).arrange(DOWN, buff=0.2).next_to(neg, DOWN, buff=0.5)
            self.play(FadeIn(note))
        self.play(FadeOut(VGroup(statement, neg, k1, *hits, note)), run_time=0.5)

    def prefix_idea(self):
        row = make_array(NUMS, side=0.95, buff=0.14, indexed=False).move_to(UP * 1.6)
        prefixes = [0]
        for x in NUMS:
            prefixes.append(prefixes[-1] + x)
        p_row = prefix_row(row, prefixes, side=0.75)
        p_lab = Text("P", font=MONO, font_size=28, color=CURRENT).next_to(p_row[0], LEFT, buff=0.25)
        with self.voice("prefix_idea"):
            self.play(FadeIn(row), run_time=0.5)
            self.play(FadeIn(p_lab), *[FadeIn(c, shift=DOWN * 0.1) for c in p_row], run_time=1.0)
            seg = SurroundingRectangle(VGroup(row[1], row[2]), color=OK, buff=0.07, stroke_width=4)
            self.play(Create(seg), p_row[3].box.animate.set_stroke(OK, width=5),
                      p_row[1].box.animate.set_stroke(OK, width=5), run_time=0.6)
            eq = Text("2 + 1  =  P[3] − P[1]  =  4 − 1  =  3", font=MONO, font_size=28, color=OK)
            eq.next_to(p_row, DOWN, buff=0.45)
            self.play(FadeIn(eq))
        with self.voice("prefix_eq"):
            key = VGroup(
                Text("tổng đoạn = k   ⇔   P[i] = P − k", font=MONO, font_size=30, color=CURRENT),
                Text("→ đếm P − k đã gặp bao nhiêu lần: HashMap {tổng: số lần}", font_size=28, color=FG),
            ).arrange(DOWN, buff=0.22).next_to(eq, DOWN, buff=0.45)
            self.play(FadeIn(key, shift=UP * 0.2))
        with self.voice("init"):
            self.play(FadeOut(VGroup(seg, eq, key)),
                      p_row[3].box.animate.set_stroke(CURRENT, width=2), p_row[1].box.animate.set_stroke(CURRENT, width=2),
                      run_time=0.4)
            init = SurroundingRectangle(p_row[0], color=OK, buff=0.08, stroke_width=5)
            note = VGroup(
                Text("{0: 1}", font=MONO, font_size=34, color=OK),
                Text("tổng rỗng, trước phần tử đầu tiên", font_size=26, color=FG),
                Text("→ đếm được mảng con bắt đầu từ vị trí 0", font_size=26, color=MUTED),
            ).arrange(DOWN, buff=0.15).next_to(p_row, DOWN, buff=0.5)
            self.play(Create(init), FadeIn(note))
        self.play(FadeOut(VGroup(row, p_row, p_lab, init, note)), run_time=0.5)

    def code_python(self):
        src = python_solution(PY_FILE)
        py = make_code(src, "python", font_size=26)
        py.scale_to_fit_width(min(py.width, 10.5))
        if py.height > 5.3:
            py.scale_to_fit_height(5.3)
        py.move_to(UP * 0.5)
        path = Text("leetcode-38-bai/lc560-subarray-sum-equals-k.py", font=MONO, font_size=20, color=MUTED)
        path.next_to(py, DOWN, buff=0.15)
        with self.voice("py_intro"):
            self.play(FadeIn(py), FadeIn(path))
        ln = {k: line_of(src, n) for k, n in {
            "init": "prefix_count[0] = 1", "res": "result += prefix_count[prefix_sum - k]",
            "put": "prefix_count[prefix_sum] += 1",
        }.items()}
        cursor = highlight(py, ln["init"])
        with self.voice("py_init"):
            self.play(FadeIn(cursor))
        with self.voice("py_order"):
            self.play(cursor.animate.become(highlight(py, ln["res"], ln["put"])))
        with self.voice("py_default"):
            self.play(cursor.animate.become(highlight(py, ln["res"], color=BAD)))
        self.wait(0.3)
        self.play(FadeOut(VGroup(py, path, cursor)), run_time=0.5)

    def code_java(self):
        src = java_solution(JAVA_FILE)
        java = make_code(src, "java", font_size=24)
        java.scale_to_fit_width(8.7)
        if java.height > 5.5:
            java.scale_to_fit_height(5.5)
        java.move_to(LEFT * 2.25 + UP * 0.45)
        path = Text(".../groupc/SubarraySumEqualsK.java", font=MONO, font_size=18, color=MUTED)
        path.next_to(java, DOWN, buff=0.1)
        ln = {k: line_of(src, n) for k, n in {
            "map": "Map<Integer, Integer> countByPrefixSum", "get": "getOrDefault(prefixSum - k, 0)",
            "merge": "merge(prefixSum, 1, Integer::sum)",
        }.items()}
        left_x = RIGHT * 4.85
        with self.voice("java_intro"):
            cursor = highlight(java, ln["map"], ln["map"] + 1)
            self.play(FadeIn(java), FadeIn(path), run_time=0.8)
            self.play(FadeIn(cursor), run_time=0.4)

        with self.voice("java_get"):
            cmp = VGroup(
                Text("Python · defaultdict[x]", font_size=22, color=MUTED),
                Text("8 key, có rác −2:0, 2:0", font=MONO, font_size=20, color=BAD),
                Text("Java · getOrDefault", font_size=22, color=MUTED),
                Text("6 key, không key rác", font=MONO, font_size=20, color=OK),
            ).arrange(DOWN, buff=0.14, aligned_edge=LEFT).move_to(left_x + UP * 1.3)
            if cmp.width > 3.9:
                cmp.scale_to_fit_width(3.9)
            self.play(cursor.animate.become(highlight(java, ln["get"])), FadeIn(cmp))

        with self.voice("java_merge"):
            same = VGroup(
                Text("merge(P, 1, Integer::sum)", font=MONO, font_size=20, color=CURRENT),
                Text("chưa có P → put(P, 1)", font=MONO, font_size=19, color=FG),
                Text("có rồi    → cộng thêm 1", font=MONO, font_size=19, color=FG),
            ).arrange(DOWN, buff=0.12, aligned_edge=LEFT)
            plate = RoundedRectangle(width=same.width + 0.5, height=same.height + 0.4, corner_radius=0.15,
                                     fill_color="#020617", fill_opacity=0.95, stroke_color=CURRENT, stroke_width=2)
            box = VGroup(plate, same.move_to(plate)).move_to(left_x + DOWN * 0.9)
            if box.width > 4.1:
                box.scale_to_fit_width(4.1)
            self.play(cursor.animate.become(highlight(java, ln["merge"])), FadeIn(box, shift=UP * 0.2))
        self.wait(0.4)
        self.play(FadeOut(VGroup(java, path, cursor, cmp, box)), run_time=0.5)

    def complexity_and_tradeoff(self):
        with self.voice("complexity"):
            cards = VGroup()
            for label, value, note in (("Time", "O(n)", "duyệt 1 lần, mỗi bước O(1)"), ("Space", "O(n)", "≤ n + 1 tổng tiền tố")):
                box = RoundedRectangle(width=4.6, height=2.5, corner_radius=0.25, stroke_color=ACCENT, stroke_width=3)
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
                ("Cách", "Time", "Đúng khi", FG),
                ("Thử mọi mảng con", "O(n²)", "mọi trường hợp", BAD),
                ("Sliding window", "O(n)", "chỉ khi mọi số > 0", CURRENT),
                ("Prefix sum + HashMap", "O(n)", "mọi trường hợp ✓", OK),
            ]
            table = VGroup()
            col_x = [-3.5, 1.0, 4.2]
            for r, (name, time, note, color) in enumerate(rows):
                row = VGroup(Text(name, font_size=30, color=color), Text(time, font_size=30, color=color),
                             Text(note, font_size=28, color=color))
                for c, item in enumerate(row):
                    item.move_to([col_x[c], 1.6 - r * 0.95, 0])
                table.add(row)
            rule = Rectangle(width=12.6, height=0.02, stroke_width=0, fill_color=MUTED, fill_opacity=0.6)
            rule.move_to([0.3, 1.6 - 0.48, 0])
            self.play(FadeIn(table[0]), FadeIn(rule), run_time=0.4)
            for row in table[1:]:
                self.play(FadeIn(row, shift=RIGHT * 0.2), run_time=0.5)
        self.play(FadeOut(VGroup(table, rule)), run_time=0.4)

        with self.voice("outro", pad=1.2):
            nxt = VGroup(
                Text("Tiếp theo", font_size=28, color=MUTED),
                Text("Bài 34 · Intersection of Two Arrays II", font_size=48, weight=BOLD),
                Text("LeetCode #350 · HashMap Counting", font_size=28, color=ACCENT),
            ).arrange(DOWN, buff=0.3)
            self.play(FadeIn(nxt, shift=UP * 0.2))


class LessonGif(Base):
    VOICE = False

    def construct(self):
        title = Text("Subarray Sum = K · prefix sum + HashMap", font_size=24, color=MUTED).to_corner(UL, buff=0.35)
        self.add(title)
        run = self.prefix_walkthrough(NUMS, K, voiced=False)
        self.wait(1.5)
        self.play(FadeOut(run), run_time=0.4)
