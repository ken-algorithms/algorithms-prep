"""Bài 38 — Design HashMap (#706). Code hiển thị được đọc trực tiếp từ file nguồn Python/Java.

Trọng tâm: 1000 bucket, hash = key % 1000, separate chaining; put cập nhật tại chỗ; collision; trường hợp xấu O(n).

    ./build.sh            # video đầy đủ (LessonVideo) + GIF (LessonGif)
"""

import sys
from contextlib import nullcontext
from pathlib import Path

from manim import (
    BOLD, DOWN, LEFT, RIGHT, UL, UP,
    Arrow, FadeIn, FadeOut, Indicate, Rectangle, RoundedRectangle, SurroundingRectangle, Text,
    Transform, VGroup, Write,
)

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parents[1] / "common"))
from kit import (  # noqa: E402
    ACCENT, BAD, CURRENT, FG, JAVA_DIR, MONO, MUTED, OK, PY_DIR, Lesson,
    highlight, java_solution, line_of, make_code,
)

PY_FILE = PY_DIR / "lc706-design-hashmap.py"
JAVA_FILE = JAVA_DIR / "groupc" / "MyHashMap.java"
SIZE = 1000
SHOWN = [0, 1, 2, 3, 4, 5]                      # các bucket vẽ ra; 999 vẽ riêng sau dấu ⋮
OPS = [                                          # (voice key, thao tác) — chạy đúng thứ tự như ví dụ đã kiểm chứng
    ("w_put", ("put", 1, 1)), ("w_put", ("put", 2, 2)), ("w_coll", ("put", 1001, 7)),
    ("w_get", ("get", 1001)), ("w_miss", ("get", 3)), ("w_update", ("put", 2, 1)),
    ("w_remove", ("remove", 1)), ("w_remove", ("get", 1)), ("w_remove", ("get", 1001)),
]


def class_source(py_file: Path) -> str:
    """Như kit.python_solution nhưng lớp tên MyHashMap thay vì Solution."""
    lines = py_file.read_text(encoding="utf-8").splitlines()
    start = next(i for i, l in enumerate(lines) if l.startswith("class MyHashMap"))
    end = next(i for i, l in enumerate(lines) if l.startswith("if __name__"))
    return "\n".join(lines[start:end]).rstrip()


def split_at(src: str, needle: str) -> tuple[str, str]:
    lines = src.splitlines()
    cut = line_of(src, needle)
    return "\n".join(lines[:cut]).rstrip(), "\n".join(lines[cut:]).rstrip()


def entry_box(key: int, value: int, color: str = ACCENT) -> VGroup:
    box = RoundedRectangle(width=1.75, height=0.5, corner_radius=0.1, stroke_color=color, stroke_width=3)
    box.set_fill("#0b1222", opacity=1)
    label = Text(f"{key} : {value}", font=MONO, font_size=20).move_to(box)
    return VGroup(box, label)


class BucketTable(VGroup):
    """buckets[0..5] + ⋮ + buckets[999]; mỗi bucket là một chuỗi ô (key : value) nối bằng mũi tên."""

    def __init__(self, top: float = 2.15, step: float = 0.6, x: float = -5.2):
        super().__init__()
        self.step, self.x0 = 2.35, x + 2.0
        self.y: dict[int, float] = {}
        self.slots = VGroup()
        for r, b in enumerate(SHOWN + [999]):
            y = top - step * r - (0.45 if b == 999 else 0)
            self.y[b] = y
            slot = Rectangle(width=0.8, height=0.5, stroke_color=MUTED, stroke_width=2).move_to([x, y, 0])
            self.slots.add(VGroup(slot, Text(str(b), font=MONO, font_size=20, color=MUTED).move_to(slot)))
        dots = Text("⋮", font_size=28, color=MUTED).move_to([x, (self.y[5] + self.y[999]) / 2, 0])
        title = Text("buckets", font=MONO, font_size=20, color=MUTED).next_to(self.slots, UP, buff=0.15)
        self.add(self.slots, dots, title)
        self.chains: dict[int, list[VGroup]] = {b: [] for b in self.y}
        self.links: dict[int, list[Arrow]] = {b: [] for b in self.y}

    def slot(self, b: int) -> VGroup:
        return self.slots[(SHOWN + [999]).index(b)]

    def pos(self, b: int, i: int):
        return [self.x0 + self.step * i, self.y[b], 0]

    def link(self, b: int, i: int) -> Arrow:
        start = self.slot(b).get_right() if i == 0 else self.chains[b][i - 1].get_right()
        end = [self.pos(b, i)[0] - 0.88, self.y[b], 0]
        return Arrow(start, end, buff=0.05, stroke_width=3, max_tip_length_to_length_ratio=0.35, color=MUTED)

    def append(self, b: int, key: int, value: int, color: str = ACCENT):
        box = entry_box(key, value, color).move_to(self.pos(b, len(self.chains[b])))
        self.chains[b].append(box)
        arrow = self.link(b, len(self.chains[b]) - 1)
        self.links[b].append(arrow)
        self.add(box, arrow)
        return [FadeIn(box, shift=LEFT * 0.3), FadeIn(arrow)]

    def replace(self, b: int, i: int, key: int, value: int):
        new = entry_box(key, value, OK).move_to(self.chains[b][i])
        return Transform(self.chains[b][i], new)

    def pop(self, b: int, i: int):
        gone, arrow = self.chains[b].pop(i), self.links[b].pop(i)
        anims = [FadeOut(gone, shift=UP * 0.3), FadeOut(arrow)]
        for j in range(i, len(self.chains[b])):
            anims.append(self.chains[b][j].animate.move_to(self.pos(b, j)))
            anims.append(Transform(self.links[b][j], self.link_after_move(b, j)))
        return anims

    def link_after_move(self, b: int, j: int) -> Arrow:
        start = self.slot(b).get_right() if j == 0 else [self.pos(b, j - 1)[0] + 0.88, self.y[b], 0]
        end = [self.pos(b, j)[0] - 0.88, self.y[b], 0]
        return Arrow(start, end, buff=0.05, stroke_width=3, max_tip_length_to_length_ratio=0.35, color=MUTED)


class Base(Lesson):
    LESSON_DIR = HERE

    def hashmap_walkthrough(self, voiced: bool):
        """Chạy đúng MyHashMap.put/get/remove trên mô hình list-of-lists, vẽ từng bước."""
        table = BucketTable()
        panel_x = RIGHT * 3.9
        op_t = Text("·", font=MONO, font_size=32).set_opacity(0).move_to(panel_x + UP * 1.75)
        hash_t = Text("·", font=MONO, font_size=26).set_opacity(0).move_to(panel_x + UP * 1.05)
        code_t = Text("·", font=MONO, font_size=22).set_opacity(0).move_to(panel_x + UP * 0.35)
        status = Text("·", font_size=26).set_opacity(0).move_to(panel_x + DOWN * 0.4)
        self.play(FadeIn(table), run_time=0.7)
        self.add(op_t, hash_t, code_t, status)
        model = [[] for _ in range(SIZE)]

        def show(mob, text, color=FG, font=MONO, size=26):
            new = Text(text, font=font, font_size=size, color=color).move_to(mob)
            if new.width > 5.6:
                new.scale_to_fit_width(5.6)
            return Transform(mob, new)

        groups: list[tuple[str, list]] = []
        for key, op in OPS:
            if groups and groups[-1][0] == key:
                groups[-1][1].append(op)
            else:
                groups.append((key, [op]))

        frame = None
        for key, ops in groups:
            with (self.voice(key) if voiced else nullcontext()):
                for name, k, *v in ops:
                    b = k % SIZE
                    bucket = model[b]
                    call = f"{name}({k}{', ' + str(v[0]) if v else ''})"
                    box = SurroundingRectangle(table.slot(b), color=CURRENT, buff=0.06, stroke_width=4)
                    self.play(show(op_t, call, CURRENT, size=32), show(hash_t, f"{k} % {SIZE} = {b}"),
                              show(code_t, "bucket = self.buckets[self._hash(key)]", MUTED, size=22),
                              show(status, "…", MUTED, font="Arial"),
                              FadeIn(box) if frame is None else Transform(frame, box), run_time=0.5)
                    frame = frame or box
                    found = None
                    for i, (kk, _) in enumerate(bucket):          # duyệt tuyến tính trong bucket
                        hit = kk == k
                        self.play(Indicate(table.chains[b][i], color=OK if hit else BAD, scale_factor=1.15),
                                  show(code_t, f"{kk} == {k}  → {'khớp' if hit else 'khác'}", OK if hit else BAD, size=22),
                                  run_time=0.45)
                        if hit:
                            found = i
                            break
                    if name == "put":
                        if found is not None:
                            bucket[found] = (k, v[0])
                            self.play(table.replace(b, found, k, v[0]), show(code_t, "bucket[i] = (key, value)", OK, size=22),
                                      show(status, f"key {k} đã có → cập nhật tại chỗ", OK, font="Arial"), run_time=0.55)
                        else:
                            bucket.append((k, v[0]))
                            note = "va chạm → nối vào cuối list" if len(bucket) > 1 else "chưa có → append"
                            self.play(*table.append(b, k, v[0]), show(code_t, "bucket.append((key, value))", OK, size=22),
                                      show(status, note, CURRENT if len(bucket) > 1 else FG, font="Arial"), run_time=0.55)
                    elif name == "get":
                        result = bucket[found][1] if found is not None else -1
                        self.play(show(code_t, f"return {'v' if found is not None else '-1'}", OK if found is not None else BAD, size=22),
                                  show(status, f"→ {result}", OK if found is not None else BAD, size=34), run_time=0.5)
                    else:
                        bucket.pop(found)
                        self.play(*table.pop(b, found), show(code_t, "bucket.pop(i)", OK, size=22),
                                  show(status, f"xoá ({k}, …) khỏi bucket {b}", FG, font="Arial"), run_time=0.6)
        parts = VGroup(table, op_t, hash_t, code_t, status, frame)
        return parts, table


class LessonVideo(Base):
    def construct(self):
        self.intro()
        header = Text("#706 · Design HashMap", font_size=22, color=MUTED).to_corner(UL, buff=0.35)
        self.add(header)
        self.problem_and_idea()

        run, _ = self.hashmap_walkthrough(voiced=True)
        self.wait(0.6)
        self.play(FadeOut(run), run_time=0.5)

        self.worst_case()
        self.code_python()
        self.code_java()
        self.real_hashmap_and_complexity()

    def intro(self):
        with self.voice("intro"):
            tag = Text("Bài 38 / 38 · Nhóm C — HashSet / Dictionary", font_size=26, color=ACCENT)
            title = Text("Design HashMap", font_size=72, weight=BOLD)
            sub = Text("LeetCode #706 · tự cài bảng băm", font_size=30, color=MUTED)
            group = VGroup(tag, title, sub).arrange(DOWN, buff=0.35).shift(UP * 0.4)
            self.play(FadeIn(tag), Write(title), run_time=1.4)
            self.play(FadeIn(sub, shift=UP * 0.2), run_time=0.6)
        self.play(FadeOut(group), run_time=0.5)

    def problem_and_idea(self):
        with self.voice("problem"):
            title = Text("Tự cài HashMap — không dùng dict / HashMap có sẵn", font_size=36).to_edge(UP, buff=0.9)
            api = VGroup(
                Text("put(key, value)", font=MONO, font_size=30, color=ACCENT),
                Text("get(key)  → value, không có → -1", font=MONO, font_size=30, color=ACCENT),
                Text("remove(key)", font=MONO, font_size=30, color=ACCENT),
            ).arrange(DOWN, buff=0.3, aligned_edge=LEFT).move_to(UP * 0.4)
            self.play(FadeIn(title, shift=DOWN * 0.2), FadeIn(api, lag_ratio=0.3), run_time=1.2)

        with self.voice("naive"):
            self.play(FadeOut(api), run_time=0.3)
            cells = VGroup(*[Rectangle(width=0.55, height=0.55, stroke_color=MUTED, stroke_width=2) for _ in range(9)])
            cells.arrange(RIGHT, buff=0).move_to(UP * 0.9)
            labels = VGroup(*[Text(t, font=MONO, font_size=16, color=MUTED).next_to(cells[i], DOWN, buff=0.1)
                              for i, t in [(0, "0"), (1, "1"), (2, "2"), (7, "…"), (8, "10⁶")]])
            dots = Text("…", font_size=30, color=MUTED).move_to(VGroup(cells[3], cells[6]))
            for c in cells[3:7]:
                c.set_stroke(opacity=0)
            cost = VGroup(
                Text("vals = [-1] * (10**6 + 1)   → O(1) mọi thao tác", font=MONO, font_size=24, color=OK),
                Text("nhưng 1 triệu ô cho ≤ 10⁴ lời gọi", font_size=26, color=BAD),
            ).arrange(DOWN, buff=0.2).next_to(cells, DOWN, buff=0.75)
            self.play(FadeIn(cells), FadeIn(labels), FadeIn(dots), run_time=0.7)
            self.play(FadeIn(cost, lag_ratio=0.4), run_time=1.0)
        self.play(FadeOut(VGroup(cells, labels, dots, cost)), run_time=0.4)

        with self.voice("idea"):
            idea = VGroup(
                Text("1000 bucket", font_size=38, color=ACCENT, weight=BOLD),
                Text("hash(key) = key % 1000", font=MONO, font_size=34, color=CURRENT),
                Text("7 → 7     1001 → 1     2025 → 25", font=MONO, font_size=28, color=FG),
            ).arrange(DOWN, buff=0.35).move_to(UP * 0.5)
            self.play(FadeIn(idea, lag_ratio=0.3), run_time=1.2)
        with self.voice("chain"):
            chain = VGroup(
                Text("bucket[1]", font=MONO, font_size=24, color=MUTED),
                entry_box(1, 1), entry_box(1001, 7, CURRENT),
            ).arrange(RIGHT, buff=0.6).next_to(idea, DOWN, buff=0.55)
            arrows = VGroup(*[Arrow(chain[i].get_right(), chain[i + 1].get_left(), buff=0.08, stroke_width=3,
                                    max_tip_length_to_length_ratio=0.3, color=MUTED) for i in range(2)])
            tag = Text("separate chaining", font=MONO, font_size=24, color=CURRENT).next_to(chain, RIGHT, buff=0.4)
            if VGroup(chain, tag).width > 12.5:
                VGroup(chain, arrows, tag).scale_to_fit_width(12.5)
            self.play(FadeIn(chain, lag_ratio=0.3), FadeIn(arrows), FadeIn(tag), run_time=1.2)
        self.play(FadeOut(VGroup(title, idea, chain, arrows, tag)), run_time=0.5)

    def worst_case(self):
        with self.voice("worst"):
            table = BucketTable()
            self.play(FadeIn(table), run_time=0.4)
            for n, k in enumerate([5, 1005, 2005, 3005]):
                self.play(*table.append(5, k, n, BAD), run_time=0.35)
            note = VGroup(
                Text("mọi key ≡ 5 (mod 1000)", font=MONO, font_size=26, color=BAD),
                Text("→ 1 list dài n → O(n) mỗi thao tác", font_size=28, color=BAD),
                Text("hash tốt + đủ bucket → list ngắn → O(1) trung bình", font_size=24, color=OK),
            ).arrange(DOWN, buff=0.2).move_to(RIGHT * 2.1 + UP * 1.25)
            self.play(FadeIn(note, lag_ratio=0.3), run_time=1.0)
        self.play(FadeOut(VGroup(table, note)), run_time=0.5)

    def panel(self, src: str, lang: str, font_size: int, max_width: float, center, height: float = 5.3):
        code = make_code(src, lang, font_size)
        code.scale_to_fit_height(height)
        if code.width > max_width:
            code.scale_to_fit_width(max_width)
        return code.move_to(center)

    def code_python(self):
        src = class_source(PY_FILE)
        left_src, right_src = split_at(src, "def get")
        left = self.panel(left_src, "python", 24, 12.0, UP * 0.5)
        right = self.panel(right_src, "python", 24, 12.0, UP * 0.5)
        path = Text("leetcode-38-bai/lc706-design-hashmap.py", font=MONO, font_size=20, color=MUTED)
        path.next_to(left, DOWN, buff=0.12)
        with self.voice("py_intro"):
            self.play(FadeIn(left), FadeIn(path))
        ln = {k: line_of(left_src, s) for k, s in {
            "init": "self.size = 1000", "hash": "return key % self.size",
            "loop": "for i, (k, v) in enumerate(bucket)", "append": "bucket.append((key, value))",
        }.items()}
        rm = line_of(right_src, "bucket.pop(i)")
        with self.voice("py_hash"):
            cursor = highlight(left, ln["init"], ln["init"] + 1)
            self.play(FadeIn(cursor))
            self.play(cursor.animate.become(highlight(left, ln["hash"])), run_time=0.6)
        with self.voice("py_put"):
            self.play(cursor.animate.become(highlight(left, ln["loop"], ln["loop"] + 3)))
            self.play(cursor.animate.become(highlight(left, ln["append"])), run_time=0.6)
        with self.voice("py_remove"):
            self.play(FadeOut(left), FadeOut(cursor), FadeIn(right), run_time=0.5)
            cursor = highlight(right, rm, rm + 1)
            self.play(FadeIn(cursor))
        self.wait(0.3)
        self.play(FadeOut(VGroup(right, path, cursor)), run_time=0.5)

    def code_java(self):
        src = java_solution(JAVA_FILE)
        left_src, right_src = split_at(src, "public int get")
        left = self.panel(left_src, "java", 22, 12.4, UP * 0.7, height=5.0)
        right = self.panel(right_src, "java", 22, 9.6, UP * 0.7, height=5.0)
        right.move_to(LEFT * (6.9 - right.width / 2 - 0.1) + UP * 0.7)
        path = Text(".../groupc/MyHashMap.java", font=MONO, font_size=18, color=MUTED).next_to(left, DOWN, buff=0.1)
        ln_set = line_of(left_src, "bucket.set(i, new Entry(key, value))")
        ln = {k: line_of(right_src, s) for k, s in {
            "record": "private record Entry", "floor": "Math.floorMod(key, BUCKET_COUNT)",
            "remove": "bucket.removeIf",
        }.items()}
        with self.voice("java_intro"):
            self.play(FadeIn(left), FadeIn(path), run_time=0.8)
            cursor = highlight(left, ln_set)
            self.play(FadeIn(cursor), run_time=0.4)
            self.wait(1.2)
            self.play(FadeOut(left), FadeOut(cursor), FadeIn(right), run_time=0.5)
            cursor = highlight(right, ln["record"], ln["record"] + 1)
            self.play(FadeIn(cursor), run_time=0.4)
        right_x = (right.get_right()[0] + 7.1) / 2
        with self.voice("java_floor"):
            self.play(cursor.animate.become(highlight(right, ln["floor"])), run_time=0.5)
            card = VGroup(
                Text("-1 % 1000 = -1", font=MONO, font_size=22, color=BAD),
                Text("✗ index âm", font_size=22, color=BAD),
                Text("floorMod(-1, 1000) = 999", font=MONO, font_size=22, color=OK),
                Text("đề cho key ≥ 0", font_size=22, color=FG),
                Text("→ ở đây là phòng thủ", font_size=22, color=FG),
            ).arrange(DOWN, buff=0.15, aligned_edge=LEFT)
            plate = RoundedRectangle(width=card.width + 0.5, height=card.height + 0.4, corner_radius=0.15,
                                     fill_color="#020617", fill_opacity=0.96, stroke_color=CURRENT, stroke_width=2)
            box = VGroup(plate, card.move_to(plate))
            box.scale_to_fit_width(min(box.width, 7.0 - right.get_right()[0] - 0.2))
            box.move_to([right_x, 0.6, 0])
            self.play(FadeIn(box, shift=UP * 0.2))
        with self.voice("java_remove"):
            self.play(cursor.animate.become(highlight(right, ln["remove"])))
        self.wait(0.4)
        self.play(FadeOut(VGroup(right, path, cursor, box)), run_time=0.5)

    def real_hashmap_and_complexity(self):
        with self.voice("real"):
            title = Text("java.util.HashMap làm thêm gì?", font_size=36, color=CURRENT).to_edge(UP, buff=0.9)
            cards = VGroup()
            for head, lines in (
                ("Resize", ["capacity 16, load factor 0.75", "size > 12 → 32 bucket, rehash", "list luôn ngắn"]),
                ("Treeify", ["1 bucket dài quá 8 phần tử", "→ cây đỏ-đen, O(log n)", "(bảng < 64 bucket: resize trước)"]),
            ):
                body = VGroup(Text(head, font_size=32, color=ACCENT, weight=BOLD),
                              *[Text(t, font_size=24, color=FG) for t in lines]).arrange(DOWN, buff=0.2)
                box = RoundedRectangle(width=5.6, height=2.8, corner_radius=0.2, stroke_color=ACCENT, stroke_width=3)
                cards.add(VGroup(box, body.move_to(box)))
            cards.arrange(RIGHT, buff=0.6).move_to(UP * 0.2)
            self.play(FadeIn(title), FadeIn(cards[0], shift=UP * 0.2), run_time=0.8)
            self.play(FadeIn(cards[1], shift=UP * 0.2), run_time=0.8)
        self.play(FadeOut(VGroup(title, cards)), run_time=0.4)

        with self.voice("complexity"):
            cards = VGroup()
            for label, value, note in (("Time", "O(1) tb", "xấu nhất O(n): cùng 1 bucket"),
                                       ("Space", "O(1000 + n)", "1000 bucket + n cặp")):
                box = RoundedRectangle(width=5.0, height=2.5, corner_radius=0.25, stroke_color=ACCENT, stroke_width=3)
                content = VGroup(Text(label, font_size=30, color=MUTED), Text(value, font_size=50, color=FG, weight=BOLD),
                                 Text(note, font_size=22, color=MUTED)).arrange(DOWN, buff=0.2).move_to(box)
                cards.add(VGroup(box, content))
            cards.arrange(RIGHT, buff=0.7).move_to(UP * 0.3)
            self.play(FadeIn(cards, shift=UP * 0.3))
        self.play(FadeOut(cards), run_time=0.4)

        with self.voice("outro", pad=1.5):
            done = VGroup(
                Text("Hoàn thành nhóm C", font_size=54, weight=BOLD, color=OK),
                Text("10 bài HashSet / Dictionary · bài 29 → 38", font_size=30, color=ACCENT),
            ).arrange(DOWN, buff=0.3)
            self.play(FadeIn(done, shift=UP * 0.2))


class LessonGif(Base):
    VOICE = False

    def construct(self):
        title = Text("Design HashMap · separate chaining", font_size=24, color=MUTED).to_corner(UL, buff=0.35)
        self.add(title)
        run, _ = self.hashmap_walkthrough(voiced=False)
        self.wait(1.5)
        self.play(FadeOut(run), run_time=0.4)
