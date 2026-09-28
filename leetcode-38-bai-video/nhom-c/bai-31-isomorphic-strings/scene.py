"""Bài 31 — Isomorphic Strings (#205). Code hiển thị được đọc trực tiếp từ file nguồn Python/Java.

Trọng tâm: vì sao cần 2 map — "badc"/"baba" qua được map s→t nhưng bị map t→s chặn.

    ./build.sh            # video đầy đủ (LessonVideo) + GIF (LessonGif)
"""

import sys
from contextlib import nullcontext
from pathlib import Path

from manim import (
    BOLD, DOWN, LEFT, RIGHT, UL, UP,
    Arrow, Create, FadeIn, FadeOut, Indicate, Rectangle, RoundedRectangle,
    SurroundingRectangle, Text, Transform, VGroup, Write,
)

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / "common"))
from kit import (  # noqa: E402
    ACCENT, BAD, CURRENT, FG, JAVA_DIR, MONO, MUTED, OK, PY_DIR, Lesson,
    highlight, java_solution, line_of, make_array, make_code, python_body, python_solution,
)

PY_FILE = PY_DIR / "lc205-isomorphic-strings.py"
JAVA_FILE = JAVA_DIR / "groupc" / "IsomorphicStrings.java"


def pair_rows(s: str, t: str, side: float, gap: float):
    s_row = make_array(list(s), side=side, buff=0.14, indexed=False)
    t_row = make_array(list(t), side=side, buff=0.14, indexed=False).next_to(s_row, DOWN, buff=gap)
    s_lab = Text("s", font=MONO, font_size=26, color=MUTED).next_to(s_row, LEFT, buff=0.3)
    t_lab = Text("t", font=MONO, font_size=26, color=MUTED).next_to(t_row, LEFT, buff=0.3)
    return s_row, t_row, VGroup(s_row, t_row, s_lab, t_lab)


def link(s_row, t_row, i: int, color: str) -> Arrow:
    return Arrow(s_row[i].box.get_bottom(), t_row[i].box.get_top(), buff=0.06,
                 color=color, stroke_width=5, max_tip_length_to_length_ratio=0.35)


class MapPanel(VGroup):
    """Một dict vẽ thành bảng: mỗi dòng 'khoá → giá trị'."""

    def __init__(self, title: str, width: float = 2.8, height: float = 2.3):
        super().__init__()
        self.box = RoundedRectangle(width=width, height=height, corner_radius=0.18, stroke_color=CURRENT, stroke_width=3)
        self.title = Text(title, font=MONO, font_size=22, color=CURRENT).next_to(self.box, UP, buff=0.12)
        self.entries: dict[str, Text] = {}
        self.add(self.box, self.title)

    def new_entry(self, key: str, value: str) -> Text:
        row = len(self.entries)
        entry = Text(f"{key} → {value}", font=MONO, font_size=28)
        entry.move_to(self.box.get_top() + DOWN * (0.38 + 0.46 * row))
        self.entries[key] = entry
        self.add(entry)
        return entry


class Base(Lesson):
    LESSON_DIR = HERE

    def iso_walkthrough(self, s: str, t: str, step_keys: list[str] | None, fast: bool = False):
        """Chạy đúng vòng lặp của Solution.is_isomorphic, vẽ từng bước."""
        speed = 0.55 if fast else 1.0
        say = (lambda i: self.voice(step_keys[i])) if step_keys else (lambda i: nullcontext())

        s_row, t_row, rows = pair_rows(s, t, side=0.75, gap=0.5)
        rows.move_to(LEFT * 3.4 + UP * 1.75)
        st = MapPanel("map_st (s → t)").move_to(LEFT * 5.0 + DOWN * 1.15)
        ts = MapPanel("map_ts (t → s)").move_to(LEFT * 1.85 + DOWN * 1.15)

        body = python_body(PY_FILE, "is_isomorphic")
        code = make_code(body, "python", font_size=24)
        code.scale_to_fit_width(min(code.width, 6.3)).move_to(RIGHT * 3.6 + UP * 0.55)
        code_title = Text("Python", font_size=20, color=MUTED).next_to(code, UP, buff=0.12).align_to(code, LEFT)
        ln = {k: line_of(body, n) for k, n in {
            "for": "for cs, ct in zip", "if1": "if cs in map_st", "if2": "if ct in map_ts",
            "put1": "map_st[cs] = ct", "put2": "map_ts[ct] = cs", "true": "return True",
        }.items()}
        cursor = highlight(code, ln["for"])
        self.play(FadeIn(rows), FadeIn(st), FadeIn(ts), FadeIn(code), FadeIn(code_title), FadeIn(cursor),
                  run_time=0.8 * speed)

        def goto(first, last=None):
            return cursor.animate.become(highlight(code, ln[first] if isinstance(first, str) else first,
                                                   None if last is None else ln[last]))

        map_st, map_ts = {}, {}
        frame, arrows, verdict = None, VGroup(), None
        for i, (cs, ct) in enumerate(zip(s, t)):
            with say(i):
                col = SurroundingRectangle(VGroup(s_row[i], t_row[i]), color=CURRENT, buff=0.08, stroke_width=3)
                arrow = link(s_row, t_row, i, CURRENT)
                arrows.add(arrow)
                self.play(Create(col) if frame is None else Transform(frame, col), goto("for"),
                          FadeIn(arrow), run_time=0.4 * speed)
                frame = frame or col

                failed_at = None
                for check, (m, k, v, panel) in (("if1", (map_st, cs, ct, st)), ("if2", (map_ts, ct, cs, ts))):
                    self.play(goto(check), run_time=0.3 * speed)
                    if k in m:
                        good = m[k] == v
                        self.play(Indicate(panel.entries[k], color=OK if good else BAD, scale_factor=1.35),
                                  run_time=0.5 * speed)
                        if not good:
                            failed_at = check
                            break
                if failed_at:
                    panel = st if failed_at == "if1" else ts
                    verdict = Text("return False", font=MONO, font_size=32, color=BAD, weight=BOLD)
                    verdict.next_to(code, DOWN, buff=0.35)
                    self.play(arrow.animate.set_color(BAD), panel.entries[cs if failed_at == "if1" else ct]
                              .animate.set_color(BAD), goto(ln[failed_at] + 1), FadeIn(verdict), run_time=0.5 * speed)
                    break

                news = []
                if cs not in map_st:
                    news.append(st.new_entry(cs, ct))
                if ct not in map_ts:
                    news.append(ts.new_entry(ct, cs))
                map_st[cs], map_ts[ct] = ct, cs
                self.play(goto("put1", "put2"), arrow.animate.set_color(OK),
                          *[FadeIn(e, shift=DOWN * 0.15) for e in news], run_time=0.45 * speed)
        else:
            verdict = Text("return True", font=MONO, font_size=32, color=OK, weight=BOLD)
            verdict.next_to(code, DOWN, buff=0.35)
            self.play(goto("true"), FadeOut(frame), FadeIn(verdict), run_time=0.5 * speed)
            frame = None
        parts = [rows, st, ts, code, code_title, cursor, arrows, verdict]
        return VGroup(*parts, *([frame] if frame else []))


class LessonVideo(Base):
    def construct(self):
        self.intro()
        header = Text("#205 · Isomorphic Strings", font_size=22, color=MUTED).to_corner(UL, buff=0.35)
        self.add(header)
        self.problem_and_examples()

        with self.voice("idea"):
            idea = VGroup(
                Text("Ý tưởng", font_size=44, color=CURRENT, weight=BOLD),
                Text("Ghi lại cặp đã ghép — gặp lại ký tự thì kiểm tra có khớp không", font_size=32),
            ).arrange(DOWN, buff=0.4)
            self.play(FadeIn(idea, shift=UP * 0.3))
        self.play(FadeOut(idea), run_time=0.4)

        run = self.iso_walkthrough("paper", "title", ["p0", "p1", "p2", "p3", "p4"])
        self.wait(0.6)
        self.play(FadeOut(run), run_time=0.5)

        self.one_map_trap()
        run = self.iso_walkthrough("badc", "baba", ["b0", "b1", "b2"])
        self.wait(0.8)
        self.play(FadeOut(run), run_time=0.5)

        self.code_python()
        self.code_java()
        self.complexity_and_tradeoff()

    def intro(self):
        with self.voice("intro"):
            tag = Text("Bài 31 / 38 · Nhóm C — HashSet / Dictionary", font_size=26, color=ACCENT)
            title = Text("Isomorphic Strings", font_size=72, weight=BOLD)
            sub = Text("LeetCode #205 · Easy", font_size=32, color=MUTED)
            group = VGroup(tag, title, sub).arrange(DOWN, buff=0.35).shift(UP * 0.4)
            self.play(FadeIn(tag), Write(title), run_time=1.4)
            self.play(FadeIn(sub, shift=UP * 0.2), run_time=0.6)
        self.play(FadeOut(group), run_time=0.5)

    def show_pairs(self, s: str, t: str, center, conflict: int | None):
        """Mũi tên ghép từng cặp; `conflict` = vị trí ký tự s bị ghép sang chỗ khác."""
        s_row, t_row, rows = pair_rows(s, t, side=0.95, gap=0.75)
        rows.move_to(center)
        self.play(FadeIn(rows), run_time=0.6)
        arrows = VGroup()
        for i in range(len(s)):
            arrow = link(s_row, t_row, i, BAD if i == conflict else OK)
            arrows.add(arrow)
            self.play(Create(arrow), run_time=0.35)
        return VGroup(rows, arrows)

    def problem_and_examples(self):
        with self.voice("problem"):
            statement = VGroup(
                Text("Thay từng ký tự của s để được t?", font_size=40),
                Text("mỗi ký tự s ↔ đúng 1 ký tự t · giữ nguyên thứ tự", font_size=28, color=MUTED),
            ).arrange(DOWN, buff=0.25).to_edge(UP, buff=0.9)
            self.play(FadeIn(statement, shift=DOWN * 0.2))

        with self.voice("example"):
            good = self.show_pairs("egg", "add", LEFT * 3.3 + UP * 0.05, None)
            ok = Text("true", font=MONO, font_size=34, color=OK).next_to(good, RIGHT, buff=0.5)
            self.play(FadeIn(ok))
        with self.voice("ex_false"):
            bad = self.show_pairs("foo", "bar", RIGHT * 3.3 + UP * 0.05, conflict=2)
            why = Text("o → a  rồi  o → r", font=MONO, font_size=26, color=BAD).next_to(bad, DOWN, buff=0.25)
            no = Text("false", font=MONO, font_size=34, color=BAD).next_to(bad, RIGHT, buff=0.5)
            self.play(FadeIn(why), FadeIn(no))
        self.play(FadeOut(VGroup(statement, good, ok, bad, why, no)), run_time=0.5)

    def one_map_trap(self):
        s, t = "badc", "baba"
        s_row, t_row, rows = pair_rows(s, t, side=0.95, gap=0.75)
        rows.move_to(LEFT * 2.8 + UP * 1.3)
        st = MapPanel("chỉ có map_st", width=3.0, height=2.3).move_to(RIGHT * 3.4 + UP * 1.0)
        with self.voice("trap"):
            self.play(FadeIn(rows), FadeIn(st), run_time=0.6)
            arrows = VGroup()
            for i, (cs, ct) in enumerate(zip(s, t)):
                arrow = link(s_row, t_row, i, OK)
                arrows.add(arrow)
                self.play(Create(arrow), FadeIn(st.new_entry(cs, ct), shift=DOWN * 0.15), run_time=0.45)
            fooled = Text("1 map → True ?", font=MONO, font_size=30, color=CURRENT).next_to(st, DOWN, buff=0.3)
            self.play(FadeIn(fooled))

        with self.voice("trap_why"):
            clash = [i for i, ct in enumerate(t) if ct == "b"]
            self.play(*[arrows[i].animate.set_color(BAD) for i in clash],
                      *[t_row[i].box.animate.set_stroke(BAD, width=6) for i in clash],
                      st.entries["b"].animate.set_color(BAD), st.entries["d"].animate.set_color(BAD),
                      run_time=0.7)
            note = Text("b → b, d → b : 2 ký tự s chung chữ b", font=MONO, font_size=24, color=BAD)
            note.next_to(rows, DOWN, buff=0.4)
            wrong = Text("✗ SAI — không phải 1–1", font_size=30, color=BAD, weight=BOLD).move_to(fooled)
            self.play(FadeIn(note), Transform(fooled, wrong))

        with self.voice("two_maps"):
            fix = VGroup(
                Text("map_st : s → t", font=MONO, font_size=28, color=FG),
                Text("map_ts : t → s   ← bắt được ca này", font=MONO, font_size=28, color=OK),
            ).arrange(DOWN, buff=0.18, aligned_edge=LEFT).move_to(DOWN * 1.75)
            self.play(FadeIn(fix, shift=UP * 0.2))
        self.play(FadeOut(VGroup(rows, arrows, st, fooled, note, fix)), run_time=0.5)

    def code_python(self):
        src = python_solution(PY_FILE)
        py = make_code(src, "python", font_size=26)
        py.scale_to_fit_width(min(py.width, 10.5))
        if py.height > 5.4:
            py.scale_to_fit_height(5.4)
        py.move_to(UP * 0.45)
        path = Text("leetcode-38-bai/lc205-isomorphic-strings.py", font=MONO, font_size=20, color=MUTED)
        path.next_to(py, DOWN, buff=0.15)
        with self.voice("py_intro"):
            self.play(FadeIn(py), FadeIn(path))
        ln = {k: line_of(src, n) for k, n in {
            "st": "map_st = {}", "ts": "map_ts = {}", "if1": "if cs in map_st", "if2": "if ct in map_ts",
            "put1": "map_st[cs] = ct", "put2": "map_ts[ct] = cs", "true": "return True",
        }.items()}
        cursor = highlight(py, ln["st"], ln["ts"])
        with self.voice("py_maps"):
            self.play(FadeIn(cursor))
        with self.voice("py_checks"):
            self.play(cursor.animate.become(highlight(py, ln["if1"], ln["if2"] + 1)))
        with self.voice("py_put"):
            self.play(cursor.animate.become(highlight(py, ln["put1"], ln["true"])))
        self.wait(0.3)
        self.play(FadeOut(VGroup(py, path, cursor)), run_time=0.5)

    def code_java(self):
        src = java_solution(JAVA_FILE)
        java = make_code(src, "java", font_size=24)
        java.scale_to_fit_height(min(java.height, 5.6))
        if java.width > 7.2:
            java.scale_to_fit_width(7.2)
        java.move_to(RIGHT * 3.2 + UP * 0.45)
        path = Text("leetcode-38-bai-java/.../groupc/IsomorphicStrings.java", font=MONO, font_size=18, color=MUTED)
        path.next_to(java, DOWN, buff=0.12)
        ln = {k: line_of(src, n) for k, n in {
            "maps": "Map<Character, Character> sourceToTarget", "get1": "sourceToTarget.get(sourceChar)",
            "get2": "targetToSource.get(targetChar)", "cmp1": "mappedTarget != null && mappedTarget != targetChar",
            "cmp2": "mappedSource != null && mappedSource != sourceChar",
        }.items()}
        left_x = LEFT * 3.9
        with self.voice("java_intro"):
            cursor = highlight(java, ln["maps"], ln["maps"] + 1)
            self.play(FadeIn(java), FadeIn(path), run_time=0.8)
            self.play(FadeIn(cursor), run_time=0.4)

        with self.voice("java_get"):
            pair = VGroup(
                Text("Python", font_size=22, color=MUTED),
                Text("if cs in map_st", font=MONO, font_size=24, color=FG),
                Text("Java", font_size=22, color=MUTED),
                Text("map.get(c) == null", font=MONO, font_size=24, color=FG),
                Text("→ chưa có khoá", font_size=22, color=MUTED),
            ).arrange(DOWN, buff=0.14, aligned_edge=LEFT).move_to(left_x + UP * 1.2)
            self.play(cursor.animate.become(highlight(java, ln["get1"], ln["get2"])), FadeIn(pair))

        with self.voice("java_box"):
            trap = VGroup(
                Text("mappedTarget != targetChar", font=MONO, font_size=22, color=CURRENT),
                Text("Character  vs  char → unbox, so giá trị ✓", font_size=22, color=OK),
                Text("Character x = 'é', y = 'é';", font=MONO, font_size=20, color=FG),
                Text("x != y  // có thể true: so địa chỉ", font=MONO, font_size=20, color=BAD),
                Text("→ 2 Character: dùng x.equals(y)", font=MONO, font_size=20, color=OK),
            ).arrange(DOWN, buff=0.16, aligned_edge=LEFT)
            plate = RoundedRectangle(width=trap.width + 0.5, height=trap.height + 0.4, corner_radius=0.15,
                                     fill_color="#020617", fill_opacity=0.95, stroke_color=CURRENT, stroke_width=2)
            box = VGroup(plate, trap.move_to(plate)).move_to(left_x + DOWN * 1.15)
            if box.width > 6.0:
                box.scale_to_fit_width(6.0)
            self.play(cursor.animate.become(highlight(java, ln["cmp1"], ln["cmp2"] + 2)), FadeIn(box, shift=UP * 0.2))
        self.wait(0.4)
        self.play(FadeOut(VGroup(java, path, cursor, pair, box)), run_time=0.5)

    def complexity_and_tradeoff(self):
        with self.voice("complexity"):
            cards = VGroup()
            for label, value, note in (("Time", "O(n)", "duyệt 1 lần"), ("Space", "O(1)", "≤ số ký tự bảng mã")):
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
                ("Cách", "Time", "Ghi chú", FG),
                ("2 HashMap (bài này)", "O(n)", "mọi ký tự, dễ đọc", OK),
                ("1 HashMap + HashSet đã dùng", "O(n)", "tương đương", ACCENT),
                ("2 mảng int[256]", "O(n)", "nhanh nhất, chỉ ASCII", CURRENT),
            ]
            table = VGroup()
            col_x = [-3.4, 1.1, 4.3]
            for r, (name, time, note, color) in enumerate(rows):
                row = VGroup(Text(name, font_size=28, color=color), Text(time, font_size=28, color=color),
                             Text(note, font_size=26, color=color))
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
                Text("Bài 32 · Longest Consecutive Sequence", font_size=48, weight=BOLD),
                Text("LeetCode #128 · HashSet", font_size=28, color=ACCENT),
            ).arrange(DOWN, buff=0.3)
            self.play(FadeIn(nxt, shift=UP * 0.2))


class LessonGif(Base):
    VOICE = False
    HOLD_WITHOUT_VOICE = 1.0

    def construct(self):
        title = Text("Isomorphic Strings · 2 map", font_size=26, color=MUTED).to_corner(UL, buff=0.35)
        self.add(title)
        run = self.iso_walkthrough("badc", "baba", ["b0", "b1", "b2"])
        self.wait(1.5)
        self.play(FadeOut(run), run_time=0.4)
