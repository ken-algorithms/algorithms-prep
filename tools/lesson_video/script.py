"""Kịch bản video: đọc file YAML, kiểm tra, chuẩn hóa câu thoại.

Mỗi cảnh (scene) là một kiểu slide; mỗi dòng trong `lines` là một câu của người đọc (khóa là mã người nói,
ví dụ `tom: "Hello"`) hoặc một khoảng lặng có đếm ngược (`wait: 5`) để người xem tự làm bài. `focus` chọn mục
đang nói tới (giữ tới dòng sau), `reveal` mở dần mục (cộng dồn trong cảnh). `say` là chữ đưa cho bộ đọc khi
cách đọc khác chữ trên màn hình (số, ký hiệu, chữ viết tắt).
"""
from __future__ import annotations

import re
from pathlib import Path
from typing import Literal, Optional, Union

import yaml
from pydantic import BaseModel, ConfigDict, Field, ValidationError, field_validator, model_validator

Kind = Literal["title", "acronyms", "bullets", "table", "code", "pattern", "compare", "flow", "diagram",
               "speech", "stats", "exercise"]

# Khóa bắt buộc của từng mục theo kiểu cảnh
ITEM_KEYS: dict[str, tuple[str, ...]] = {
    "title": (),
    "acronyms": ("abbr", "full"),      # + `vi`: nghĩa tiếng Việt một dòng
    "bullets": ("t",),                 # + `s`: dòng phụ
    "table": (),                       # dùng `columns` + `rows`
    "code": ("lines", "t"),            # chú thích: `lines` = [từ, tới] (đánh số từ 1), `t` = nội dung
    "pattern": (),                     # dùng `bad`, `good`, `stats`
    "compare": ("title", "lines"),
    "flow": ("t",),                    # + `s`: dòng phụ, `f`: công thức dưới hộp
    "diagram": (),                     # dùng `nodes`, `edges`
    "speech": ("t",),                  # bài nói mẫu: mỗi mục một câu
    "stats": ("v", "l"),               # + `s`: dòng phụ
    "exercise": ("t",),                # dữ kiện của đề, mỗi mục một dòng
}
LINE_FIELDS = {"say", "focus", "reveal", "wait", "pause", "note", "read"}

DEFAULT_SPEAKERS = {
    "tom": {"name": "Tom", "role": "narrator", "color": "#1F5FAD", "voice": {"kokoro": "am_michael"}},
}


class _M(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Speaker(_M):
    name: str
    role: str = ""
    voice: Union[str, dict[str, str]]
    color: str = "#1F5FAD"

    def voice_for(self, engine: str) -> str:
        if isinstance(self.voice, str):
            return self.voice
        if engine in self.voice:
            return self.voice[engine]
        if engine == "silent":
            return next(iter(self.voice.values()))
        raise ValueError(f"{self.name}: chưa khai báo giọng cho bộ đọc {engine} (có: {', '.join(self.voice)})")

    @field_validator("color")
    @classmethod
    def _hex(cls, v: str) -> str:
        if not re.fullmatch(r"#[0-9A-Fa-f]{6}", v):
            raise ValueError(f"màu phải dạng #RRGGBB: {v}")
        return v


class Line(_M):
    speaker: Optional[str] = None
    text: str = ""
    say: Optional[str] = None
    focus: Optional[int] = None
    reveal: list[str] = []
    wait: float = Field(0, ge=0, le=60)
    pause: float = Field(0, ge=0, le=10)
    note: str = ""

    @field_validator("reveal", mode="before")
    @classmethod
    def _list(cls, v):
        if v is None:
            return []
        return [str(x) for x in (v if isinstance(v, list) else [v])]

    @model_validator(mode="after")
    def _kind(self):
        if bool(self.speaker) == bool(self.wait):
            raise ValueError("mỗi dòng là một câu (khóa = mã người nói) hoặc một khoảng lặng `wait`")
        if self.speaker and not self.text.strip():
            raise ValueError(f"câu của {self.speaker} trống")
        return self

    @property
    def spoken(self) -> str:
        from .speak import speakable
        return speakable(self.say or self.text)


class Node(_M):
    """Hộp trong sơ đồ. Tọa độ (x, y) là tâm hộp, tính trong vùng nội dung 1160 × 390 (gốc ở góc trên trái)."""
    id: str
    label: str
    sub: str = ""
    x: float
    y: float
    w: float = 190
    h: float = 64
    shape: Literal["box", "db", "queue", "user", "zone"] = "box"
    at: int = 0          # hiện ra từ bước `focus` này
    tone: Literal["", "bad", "ok", "warn"] = ""


class Edge(_M):
    a: str
    b: str
    label: str = ""
    dashed: bool = False
    at: int = 0


class Scene(_M):
    kind: Kind
    chapter: str = ""
    heading: str = ""
    sub: str = ""
    items: list[dict] = []
    columns: list[str] = []
    rows: list[list[str]] = []
    widths: list[float] = []
    mono: list[int] = []       # bảng: cột chữ đơn cách (phép tính)
    accent: list[int] = []     # bảng: cột tô màu nhấn
    build: bool = False        # hiện dần hàng / mục tới mục đang `focus`
    style: Literal["num", "dot", "check"] = "num"
    code: str = ""
    bad: str = ""
    good: str = ""
    bad_label: str = "Bad"
    good_label: str = "Fix"
    stats: list[list[str]] = []
    stats_columns: list[str] = ["", "Bad", "Fix", "Gain", "Garbage per call"]
    nodes: list[Node] = []
    edges: list[Edge] = []
    q: str = ""                # đề bài (cảnh exercise)
    n: str = ""                # số hiệu bài tập
    label: str = ""            # nhãn của cảnh exercise thay cho "Exercise" (Question, Drill…)
    note: str = ""             # chú thích nhỏ cuối vùng nội dung
    covers: list[str] = []     # anchor nguồn mà cảnh này dạy
    points: list[str] = []     # mã ý chính (points.yaml) mà cảnh này dạy
    lines: list[Line]

    @model_validator(mode="after")
    def _items(self):
        need = ITEM_KEYS[self.kind]
        for i, it in enumerate(self.items):
            miss = [k for k in need if k not in it]
            if miss:
                raise ValueError(f"cảnh {self.kind}: mục {i} thiếu {', '.join(miss)}")
        if need and not self.items:
            raise ValueError(f"cảnh {self.kind} cần `items`")
        if self.kind == "table":
            if not self.rows:
                raise ValueError("cảnh table cần `rows`")
            width = len(self.columns) or len(self.rows[0])
            for r in self.rows:
                if len(r) != width:
                    raise ValueError(f"cảnh table: hàng {r!r} có {len(r)} ô, cần {width}")
            if self.widths and len(self.widths) != width:
                raise ValueError("cảnh table: `widths` phải có đúng số cột")
        if self.kind == "pattern" and not (self.bad and self.good):
            raise ValueError("cảnh pattern cần `bad` và `good`")
        if self.kind == "code" and not self.code.strip():
            raise ValueError("cảnh code cần `code`")
        if self.kind == "diagram":
            ids = {n.id for n in self.nodes}
            if not self.nodes:
                raise ValueError("cảnh diagram cần `nodes`")
            for e in self.edges:
                if e.a not in ids or e.b not in ids:
                    raise ValueError(f"cảnh diagram: cạnh {e.a} → {e.b} trỏ tới hộp không có")
        if not self.lines:
            raise ValueError("cảnh không có câu nào")
        n = self.focus_count()
        for ln in self.lines:
            if ln.focus is not None and not 0 <= ln.focus < max(n, 1):
                raise ValueError(f"cảnh {self.kind}: focus {ln.focus} ngoài khoảng 0…{n - 1}")
            bad = [r for r in ln.reveal if not r.isdigit() or int(r) >= max(n, 1)]
            if bad:
                raise ValueError(f"cảnh {self.kind}: reveal không khớp mục nào: {bad}")
        return self

    def focus_count(self) -> int:
        if self.kind == "table":
            return len(self.rows)
        if self.kind == "pattern":
            return 3  # 0 = bản xấu, 1 = bản sửa, 2 = số đo
        if self.kind == "diagram":
            return 1 + max([n.at for n in self.nodes] + [e.at for e in self.edges] + [0])
        return len(self.items)

    def text_blob(self) -> str:
        """Toàn bộ chữ của cảnh (màn hình + lời đọc), dùng để kiểm ý chính và chữ viết tắt."""
        parts = [self.heading, self.sub, self.q, self.n, self.note, self.code, self.bad, self.good]
        parts += [str(v) for it in self.items for v in it.values()]
        parts += [c for r in self.rows for c in r] + self.columns
        parts += [c for r in self.stats for c in r]
        parts += [n.label + " " + n.sub for n in self.nodes] + [e.label for e in self.edges]
        parts += [ln.text + " " + (ln.say or "") + " " + ln.note for ln in self.lines]
        return "\n".join(p for p in parts if p)


class Lesson(_M):
    id: str = Field(pattern=r"^[A-Za-z0-9_.-]+$")
    ep: int = Field(ge=0)
    week: int = Field(0, ge=0)
    title: str
    subtitle: str = ""
    tag: str = ""
    prefix: str = Field("Ep", pattern=r"^[A-Za-z]{1,3}$")  # mã video: Ep03, K03…
    series: str = "Java System Design · Phase 1"
    sources: str = "../.."          # thư mục chứa file nguồn, tính từ thư mục của file YAML
    covers: list[str] = []
    speakers: dict[str, Speaker] = Field(default_factory=lambda: {k: Speaker(**v) for k, v in DEFAULT_SPEAKERS.items()})
    gap: float = Field(0.45, ge=0, le=3)
    scene_gap: float = Field(0.8, ge=0, le=5)
    scenes: list[Scene]

    @model_validator(mode="before")
    @classmethod
    def _lines(cls, data):
        """`- tom: "Hello"` → {"speaker": "tom", "text": "Hello"}; `- read: tom` + `focus: i` → đọc mục i của cảnh."""
        if not isinstance(data, dict):
            return data
        data = dict(data)
        known = set((data.get("speakers") or DEFAULT_SPEAKERS).keys())
        scenes = []
        for sc in data.get("scenes") or []:
            if not isinstance(sc, dict):
                scenes.append(sc)
                continue
            sc, out = dict(sc), []
            for ln in sc.get("lines") or []:
                if not isinstance(ln, dict):
                    raise ValueError(f"dòng phải là bảng khóa–giá trị: {ln!r}")
                if "speaker" in ln or "text" in ln:
                    out.append(dict(ln))
                    continue
                if "read" in ln:
                    items = sc.get("items") or []
                    i = ln.get("focus")
                    if not isinstance(i, int) or not 0 <= i < len(items):
                        raise ValueError(f"`read` cần `focus` là số thứ tự mục: {ln!r}")
                    if ln["read"] not in known:
                        raise ValueError(f"người nói chưa khai báo: {ln['read']}")
                    row = {k: v for k, v in ln.items() if k in LINE_FIELDS and k != "read"}
                    row["speaker"], row["text"] = ln["read"], str(items[i]["t"])
                    if items[i].get("say") and "say" not in row:
                        row["say"] = str(items[i]["say"])
                    out.append(row)
                    continue
                who = [k for k in ln if k not in LINE_FIELDS]
                if len(who) > 1:
                    raise ValueError(f"một dòng chỉ có một người nói: {who}")
                if who and who[0] not in known:
                    raise ValueError(f"người nói chưa khai báo: {who[0]}")
                row = {k: v for k, v in ln.items() if k in LINE_FIELDS}
                if who:
                    row["speaker"], row["text"] = who[0], str(ln[who[0]])
                out.append(row)
            sc["lines"] = out
            scenes.append(sc)
        if "scenes" in data:
            data["scenes"] = scenes
        return data

    @property
    def code(self) -> str:
        """Mã hiện trên màn hình, trong bảng độ phủ và trên web: Ep03, K03…"""
        return f"{self.prefix}{self.ep:02d}"

    def chapters(self) -> list[tuple[int, str]]:
        return [(i, sc.chapter) for i, sc in enumerate(self.scenes) if sc.chapter]

    def all_covers(self) -> list[str]:
        seen: list[str] = []
        for c in self.covers + [c for sc in self.scenes for c in sc.covers]:
            if c not in seen:
                seen.append(c)
        return seen


SERIES_KEYS = {"series", "speakers", "sources", "gap", "scene_gap", "prefix"}


def lesson_paths(lessons_dir: Path) -> list[Path]:
    """Mọi kịch bản của một bộ: các file .yaml trừ points.yaml và series.yaml (ep03-….yaml, k03-….yaml…)."""
    return sorted(p for p in Path(lessons_dir).glob("*.yaml") if p.name not in ("points.yaml", "series.yaml"))


def series_defaults(path: Path) -> dict:
    """`series.yaml` cạnh kịch bản: giá trị chung của cả bộ video (tên bộ, người đọc…); kịch bản ghi đè được."""
    p = Path(path).resolve().parent / "series.yaml"
    if not p.exists():
        return {}
    raw = yaml.safe_load(p.read_text(encoding="utf-8")) or {}
    bad = set(raw) - SERIES_KEYS
    if bad:
        raise SystemExit(f"{p}: khoá không dùng được ở series.yaml: {', '.join(sorted(bad))} (chỉ {', '.join(sorted(SERIES_KEYS))})")
    return raw


def load(path: Path) -> Lesson:
    raw = yaml.safe_load(Path(path).read_text(encoding="utf-8"))
    if isinstance(raw, dict):
        raw = {**series_defaults(path), **raw}
    try:
        return Lesson.model_validate(raw)
    except ValidationError as e:
        raise SystemExit(f"{path}: kịch bản không hợp lệ\n{e}") from None


# ---------- anchor của file nguồn ----------
def gh_slug(text: str) -> str:
    """Thuật toán anchor của GitHub (giống build_site.gh_slug)."""
    text = re.sub(r"`", "", text.strip())
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)
    text = re.sub(r"<[^>]+>", "", text)
    text = text.lower()
    text = re.sub(r"[^\w\s-]", "", text, flags=re.UNICODE)
    return re.sub(r"\s", "-", text)


def headings(md_path: Path) -> list[tuple[int, str, str]]:
    """[(cấp, tiêu đề, anchor)] của một file markdown, bỏ qua khối code."""
    out, seen, fence = [], {}, False
    for line in Path(md_path).read_text(encoding="utf-8").splitlines():
        if line.startswith("```"):
            fence = not fence
            continue
        m = None if fence else re.match(r"^(#{1,6}) (.*)", line)
        if not m:
            continue
        slug = gh_slug(m.group(2))
        k = seen.get(slug, 0)
        seen[slug] = k + 1
        out.append((len(m.group(1)), m.group(2).strip(), f"{slug}-{k}" if k else slug))
    return out


def source_dir(path: Path, lesson: Lesson) -> Path:
    return (Path(path).resolve().parent / lesson.sources).resolve()


def check(lesson: Lesson, path: Path, points: Optional[dict] = None) -> list[str]:
    """Anchor trong `covers` có thật; mã ý chính có trong points.yaml và chữ `expect` có mặt trong cảnh nhận ý đó."""
    problems: list[str] = []
    src = source_dir(path, lesson)
    cache: dict[str, set[str]] = {}
    for c in lesson.all_covers():
        f, _, anchor = c.partition("#")
        p = src / f
        if not p.exists():
            problems.append(f"covers: không thấy file {f}")
            continue
        if anchor:
            if f not in cache:
                cache[f] = {a for _, _, a in headings(p)}
            if anchor not in cache[f]:
                problems.append(f"covers: {f} không có anchor #{anchor}")
    if points is not None:
        for i, sc in enumerate(lesson.scenes):
            blob = sc.text_blob()
            for pid in sc.points:
                pt = points.get(pid)
                if pt is None:
                    problems.append(f"cảnh {i}: ý chính '{pid}' không có trong points.yaml")
                    continue
                for pat in pt.get("expect", []):
                    if not re.search(pat, blob, flags=re.I):
                        problems.append(f"cảnh {i}: ý '{pid}' cần chữ /{pat}/ nhưng cảnh không có")
    return problems


def load_points(path: Path) -> dict:
    """points.yaml: danh sách {id, src, text, expect: [regex…]} → {id: mục}."""
    raw = yaml.safe_load(Path(path).read_text(encoding="utf-8")) or {}
    out = {}
    for sec in raw.get("sections", []):
        for pt in sec.get("points", []):
            if pt["id"] in out:
                raise SystemExit(f"{path}: trùng mã ý chính {pt['id']}")
            out[pt["id"]] = dict(pt, src=sec["src"], section=sec.get("title", ""))
    return out
