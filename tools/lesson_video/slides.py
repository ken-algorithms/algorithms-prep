"""Vẽ slide 1280×720 bằng Pillow: thanh đầu, tiêu đề cảnh, nội dung theo kiểu cảnh, phụ đề, thanh tiến độ.

Khung chung (thanh đầu, phụ đề có avatar người đọc, đếm ngược, thanh tiến độ) theo cách của công cụ
`agents/lesson_video` repo superken-ielts/ielts-target-5-5; các kiểu cảnh viết mới cho nội dung system design:
bảng, phép tính, code, cặp code xấu/sửa, sơ đồ hộp–mũi tên, luồng các bước, thẻ chữ viết tắt, bài nói mẫu.
"""
from __future__ import annotations

import math
import re
import textwrap
from functools import lru_cache
from pathlib import Path
from typing import Optional

from PIL import Image, ImageDraw, ImageFont

from .script import Lesson, Scene
from .timeline import Frame

W, H = 1280, 720
CONTENT_TOP_HEADED, CONTENT_TOP_BARE, BOTTOM = 146, 92, 548
C = {
    "bg": "#F4F6F9", "ink": "#16202A", "muted": "#5A6675", "dim": "#A3ACB8", "line": "#D8DFE8",
    "card": "#FFFFFF", "acc": "#1F5FAD", "soft": "#E2EBF7", "ok": "#1E7B3A", "oksoft": "#E1F1E5",
    "bad": "#C62828", "badsoft": "#FBE4E4", "warn": "#A15C00", "warnsoft": "#FFF0D9", "zone": "#7A8797",
    "code": "#101820", "codeink": "#E6EDF3", "codedim": "#6E7B8B", "codehl": "#1F3A5F",
    "kw": "#FF7B72", "str": "#A5D6FF", "com": "#8B949E", "num": "#79C0FF", "ann": "#D2A8FF", "type": "#FFA657",
}
TONES = {"bad": ("bad", "badsoft"), "ok": ("ok", "oksoft"), "warn": ("warn", "warnsoft"), "": ("acc", "soft")}

FONT_FILES = {
    "regular": ["/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"],
    "bold": ["/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"],
    "italic": ["/usr/share/fonts/truetype/liberation/LiberationSans-Italic.ttf",
               "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"],
    "mono": ["/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"],
    "monob": ["/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"],
}


@lru_cache(maxsize=None)
def font(kind: str, size: int) -> ImageFont.FreeTypeFont:
    for p in FONT_FILES[kind]:
        if Path(p).exists():
            return ImageFont.truetype(p, size)
    return ImageFont.load_default(size)


def wrap(d: ImageDraw.ImageDraw, text: str, f, width: float) -> list[str]:
    out: list[str] = []
    for para in str(text).split("\n"):
        cur = ""
        for w in para.split():
            t = (cur + " " + w).strip()
            if not cur or d.textlength(t, font=f) <= width:
                cur = t
            else:
                out.append(cur)
                cur = w
        out.append(cur)
    return out


def fit(d, text: str, kind: str, size: int, width: float, max_lines: int, min_size: int = 13):
    """Cỡ chữ lớn nhất (≤ size) để đoạn văn vừa max_lines dòng."""
    while True:
        f = font(kind, size)
        lines = wrap(d, text, f, width)
        if len(lines) <= max_lines or size <= min_size:
            if len(lines) > max_lines:
                lines = lines[:max_lines]
                lines[-1] = lines[-1].rstrip(".,;:") + "…"
            return f, lines
        size -= 1


def block(d, xy, text: str, kind: str, size: int, width: float, fill: str, max_lines: int = 3,
          lead: float = 1.25, min_size: int = 13) -> int:
    """Vẽ đoạn văn tự xuống dòng; trả về tọa độ y ngay dưới đoạn."""
    if not text:
        return xy[1]
    f, lines = fit(d, text, kind, size, width, max_lines, min_size)
    x, y = xy
    step = int(f.size * lead)
    for ln in lines:
        d.text((x, y), ln, font=f, fill=fill)
        y += step
    return y


def _hex(c: str) -> tuple[int, int, int]:
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5))


def tint(c: str, a: float) -> str:
    r, g, b = _hex(c)
    return "#%02X%02X%02X" % tuple(round(255 - (255 - v) * a) for v in (r, g, b))


def dashed_line(d, a, b, fill, width=2, dash=9, gap=7):
    (x0, y0), (x1, y1) = a, b
    length = math.hypot(x1 - x0, y1 - y0)
    if length == 0:
        return
    ux, uy = (x1 - x0) / length, (y1 - y0) / length
    t = 0.0
    while t < length:
        e = min(t + dash, length)
        d.line((x0 + ux * t, y0 + uy * t, x0 + ux * e, y0 + uy * e), fill=fill, width=width)
        t = e + gap


def dashed_rect(d, box, fill, width=2, r=14):
    x0, y0, x1, y1 = box
    for a, b in (((x0 + r, y0), (x1 - r, y0)), ((x0 + r, y1), (x1 - r, y1)),
                 ((x0, y0 + r), (x0, y1 - r)), ((x1, y0 + r), (x1, y1 - r))):
        dashed_line(d, a, b, fill, width)
    for (cx, cy, s) in ((x0 + r, y0 + r, 180), (x1 - r, y0 + r, 270), (x1 - r, y1 - r, 0), (x0 + r, y1 - r, 90)):
        d.arc((cx - r, cy - r, cx + r, cy + r), s, s + 90, fill=fill, width=width)


# ---------- tô màu code đơn giản ----------
KEYWORDS = {
    "java": set("""abstract boolean break byte case catch char class const continue default do double else enum
        extends final finally float for if implements import instanceof int interface long new null package private
        protected public return short static super switch synchronized this throw throws try var void volatile while
        true false record""".split()),
    "lua": set("local function end if then else elseif return nil and or not for in do while true false".split()),
    "sql": set("""select from where and or order by desc asc limit insert into values update set delete join on
        for skip locked index include partition create table group having as in not null""".split()),
    "bash": set("cd mvn java".split()),
    "text": set(),
}
TOKEN = re.compile(r'(//.*$|--.*$|#.*$|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|@\w+|\b\d[\d_.,]*[a-zA-Z]*\b|\b\w+\b|\s+|.)')


def code_tokens(line: str, lang: str) -> list[tuple[str, str]]:
    kws = KEYWORDS.get(lang, set())
    out = []
    for m in TOKEN.finditer(line):
        t = m.group(0)
        if (t.startswith("//") and lang in ("java", "text")) or (t.startswith("--") and lang in ("lua", "sql")) \
                or (t.startswith("#") and lang == "bash"):
            col = "com"
        elif t[:1] in "\"'" and len(t) > 1:
            col = "str"
        elif t.startswith("@") and len(t) > 1:
            col = "ann"
        elif t[:1].isdigit():
            col = "num"
        elif (t.lower() if lang == "sql" else t) in kws:
            col = "kw"
        elif lang == "java" and t[:1].isupper() and t.isidentifier():
            col = "type"
        else:
            col = "codeink"
        out.append((t, col))
    return out


class Slides:
    def __init__(self, lesson: Lesson, voice_label: Optional[dict[str, str]] = None):
        self.L = lesson
        self.voice_label = voice_label or {}
        self.order = list(lesson.speakers)

    # ---------- khung chung ----------
    def render(self, fr: Frame, progress: float) -> Image.Image:
        img = Image.new("RGB", (W, H), C["bg"])
        d = ImageDraw.Draw(img)
        sc = self.L.scenes[fr.scene]
        self._header(d)
        top = CONTENT_TOP_BARE
        if sc.kind != "title" and (sc.heading or sc.sub):
            self._heading(d, sc)
            top = CONTENT_TOP_HEADED
        bottom = BOTTOM - (30 if sc.note else 0)
        getattr(self, "_k_" + sc.kind)(img, d, sc, fr, top, bottom)
        if sc.note:
            f, lines = fit(d, sc.note, "italic", 17, W - 120, 1)
            d.text((60, BOTTOM - 22), lines[0], font=f, fill=C["muted"])
        self._caption(d, fr)
        d.rounded_rectangle((40, 711, W - 40, 715), 2, fill=C["line"])
        if progress > 0:
            d.rounded_rectangle((40, 711, 40 + max(4, (W - 80) * min(progress, 1)), 715), 2, fill=C["acc"])
        return img

    def _header(self, d):
        d.rectangle((0, 0, W, 64), fill=C["card"])
        d.line((0, 64, W, 64), fill=C["line"], width=1)
        tag = self.L.tag or (f"EP{self.L.ep:02d}" + (f" · WEEK {self.L.week}" if self.L.week else ""))
        ft = font("bold", 20)
        tw = d.textlength(tag, font=ft)
        d.polygon([(0, 0), (tw + 56, 0), (tw + 36, 64), (0, 64)], fill=C["acc"])
        d.text((22, 32), tag, font=ft, fill="#FFFFFF", anchor="lm")
        fr = font("regular", 16)
        rw = d.textlength(self.L.series, font=fr)
        d.text((W - 26, 32), self.L.series, font=fr, fill=C["muted"], anchor="rm")
        fs, lines = fit(d, self.L.title, "bold", 21, W - 26 - rw - 30 - (tw + 66), 1)
        d.text((tw + 66, 32), lines[0], font=fs, fill=C["ink"], anchor="lm")

    def _heading(self, d, sc: Scene):
        if sc.heading:
            f, lines = fit(d, sc.heading, "bold", 30, W - 120, 1, min_size=20)
            d.text((60, 80), lines[0], font=f, fill=C["acc"])
        if sc.sub:
            f, lines = fit(d, sc.sub, "regular", 18, W - 120, 1)
            d.text((60, 118), lines[0], font=f, fill=C["muted"])

    def _avatar(self, d, cx, cy, r, key):
        sp = self.L.speakers[key]
        d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=sp.color)
        d.text((cx, cy + 1), sp.name[:1].upper(), font=font("bold", int(r * 0.95)), fill="#FFFFFF", anchor="mm")

    def _caption(self, d, fr: Frame):
        box = (40, 560, W - 40, 704)
        d.rounded_rectangle(box, 16, fill=C["card"], outline=C["line"], width=2)
        x0, width = 146, W - 40 - 146 - 26
        if fr.speaker:
            sp = self.L.speakers[fr.speaker]
            d.rounded_rectangle((box[0], box[1], box[0] + 8, box[3]), 4, fill=sp.color)
            self._avatar(d, 94, 632, 34, fr.speaker)
            d.text((x0, 572), sp.name + (" · " + sp.role if sp.role else ""), font=font("bold", 16), fill=sp.color)
            block(d, (x0, 596), fr.text, "regular", 25, width, C["ink"], max_lines=3, lead=1.22, min_size=17)
        elif fr.countdown is not None:
            d.ellipse((60, 598, 128, 666), outline=C["acc"], width=5)
            d.text((94, 633), str(fr.countdown), font=font("bold", 32), fill=C["acc"], anchor="mm")
            d.text((x0, 584), "Pause the video and try it yourself", font=font("bold", 25), fill=C["ink"])
            block(d, (x0, 626), fr.note or "Then press play to compare with the answer.", "italic", 21, width,
                  C["muted"], max_lines=2)

    # ---------- tiện ích ----------
    @staticmethod
    def _visible(sc: Scene, fr: Frame, n: int) -> int:
        """Số mục đang hiện: tất cả, hoặc (khi `build`) tới mục đang focus / đã reveal."""
        if not sc.build:
            return n
        upto = -1 if fr.focus is None else fr.focus
        if fr.revealed:
            upto = max(upto, max(int(r) for r in fr.revealed))
        return max(0, min(n, upto + 1))

    def _card(self, d, box, on=False, tone="", fill=None, width=2, r=14):
        col, soft = TONES.get(tone, TONES[""])
        d.rounded_rectangle(box, r, fill=fill or (C[soft] if on else C["card"]),
                            outline=C[col] if on else C["line"], width=3 if on else width)

    # ---------- các kiểu cảnh ----------
    def _k_title(self, img, d, sc, fr, top, bottom):
        y = block(d, (60, 96), self.L.title, "bold", 50, W - 120, C["ink"], max_lines=2, lead=1.12)
        if self.L.subtitle:
            y = block(d, (60, y + 6), self.L.subtitle, "bold", 26, W - 120, C["acc"], max_lines=2)
        chapters = [name for i, name in self.L.chapters() if i != fr.scene]
        x0, y0 = 60, max(y + 26, 236)
        d.text((x0, y0), "In this video", font=font("bold", 19), fill=C["muted"])
        f = font("regular", 20 if len(chapters) <= 7 else 18)
        step = min(34, (bottom - y0 - 34) / max(len(chapters), 1))
        for i, name in enumerate(chapters):
            yy = y0 + 34 + i * step
            d.ellipse((x0, yy + 3, x0 + 22, yy + 25), fill=C["acc"])
            d.text((x0 + 11, yy + 14), str(i + 1), font=font("bold", 12), fill="#FFFFFF", anchor="mm")
            fl, lines = fit(d, name, "regular", f.size, 640, 1)
            d.text((x0 + 34, yy + 2), lines[0], font=fl, fill=C["ink"])
        cx0, cx1 = 780, W - 60
        cy0 = y0
        d.rounded_rectangle((cx0, cy0, cx1, bottom), 18, fill=C["card"], outline=C["line"], width=2)
        key = self.order[0]
        sp = self.L.speakers[key]
        self._avatar(d, cx0 + 64, cy0 + 64, 38, key)
        d.text((cx0 + 120, cy0 + 36), sp.name, font=font("bold", 28), fill=C["ink"])
        d.text((cx0 + 120, cy0 + 72), self.voice_label.get(key, sp.role), font=font("regular", 16), fill=C["muted"])
        yy = cy0 + 124
        for it in sc.items:
            yy = block(d, (cx0 + 28, yy), str(it.get("t", "")), "regular", 17, cx1 - cx0 - 56, C["ink"],
                       max_lines=3) + 8

    def _k_acronyms(self, img, d, sc, fr, top, bottom):
        n = len(sc.items)
        cols = 1 if n <= 5 else 2
        rows = math.ceil(n / cols)
        gap = 22
        cw = (W - 120 - gap * (cols - 1)) / cols
        rh = min(84, (bottom - top) / rows)
        for i, it in enumerate(sc.items):
            c, r = divmod(i, rows)
            x, y = 60 + c * (cw + gap), top + r * rh
            on = fr.focus == i
            self._card(d, (x, y + 3, x + cw, y + rh - 5), on=on, r=12)
            abbr = str(it["abbr"])
            fa, la = fit(d, abbr, "bold", 24, 150, 1, min_size=14)
            d.rounded_rectangle((x + 12, y + 12, x + 176, y + rh - 14), 10, fill=C["soft"])
            d.text((x + 94, y + rh / 2 - 1), la[0], font=fa, fill=C["acc"], anchor="mm")
            tx = x + 192
            tw = cw - 206
            if it.get("vi") and rh >= 60:
                ff, lf = fit(d, str(it["full"]), "bold", 20, tw, 1, min_size=14)
                d.text((tx, y + rh / 2 - 13), lf[0], font=ff, fill=C["ink"], anchor="lm")
                fv, lv = fit(d, str(it["vi"]), "italic", 18, tw, 1, min_size=13)
                d.text((tx, y + rh / 2 + 14), lv[0], font=fv, fill=C["muted"], anchor="lm")
            else:
                ff, lf = fit(d, str(it["full"]), "bold", 20, tw, 2, min_size=14)
                yy = y + rh / 2 - len(lf) * ff.size * 0.62
                for ln in lf:
                    d.text((tx, yy), ln, font=ff, fill=C["ink"])
                    yy += ff.size * 1.22

    def _k_bullets(self, img, d, sc, fr, top, bottom):
        n = len(sc.items)
        shown = self._visible(sc, fr, n)
        has_sub = any(it.get("s") for it in sc.items)
        long_item = any(len(str(it["t"])) > 70 for it in sc.items)
        rh = min(92 if has_sub else (78 if long_item else 64), (bottom - top) / max(n, 1))
        for i, it in enumerate(sc.items[:shown]):
            y = top + i * rh
            on = fr.focus == i
            if on:
                d.rounded_rectangle((52, y + 2, W - 52, y + rh - 4), 12, fill=C["soft"])
                d.rounded_rectangle((52, y + 2, 58, y + rh - 4), 3, fill=C["acc"])
            cy = y + (min(rh, 64) - 6) / 2 + (2 if has_sub else 0)
            if sc.style == "check":
                done = str(i) in fr.revealed
                d.rounded_rectangle((74, cy - 15, 104, cy + 15), 6, fill=C["ok"] if done else C["card"],
                                    outline=C["ok"] if done else C["dim"], width=2)
                if done:
                    d.line((81, cy, 87, cy + 7, 98, cy - 8), fill="#FFFFFF", width=4)
            elif sc.style == "dot":
                d.ellipse((82, cy - 7, 96, cy + 7), fill=C["acc"])
            else:
                d.ellipse((74, cy - 16, 106, cy + 16), fill=C["acc"])
                d.text((90, cy + 1), str(i + 1), font=font("bold", 17), fill="#FFFFFF", anchor="mm")
            tx, tw = 126, W - 190
            if it.get("s") and rh >= 52:
                f, lines = fit(d, str(it["t"]), "bold", 23, tw, 1, min_size=15)
                d.text((tx, cy - 3), lines[0], font=f, fill=C["ink"], anchor="lb")
                fs, ls = fit(d, str(it["s"]), "regular", 18, tw, 2 if rh >= 80 else 1, min_size=13)
                yy = cy + 6
                for ln in ls:
                    d.text((tx, yy), ln, font=fs, fill=C["muted"])
                    yy += fs.size * 1.2
            else:
                f, lines = fit(d, str(it["t"]), "bold", 23, tw, 2 if rh >= 60 else 1, min_size=15)
                yy = cy - len(lines) * f.size * 0.62
                for ln in lines:
                    d.text((tx, yy), ln, font=f, fill=C["ink"])
                    yy += f.size * 1.24

    def _table_layout(self, d, sc, widths, avail_h, has_header):
        for size in range(26 if len(sc.rows) <= 4 else 23, 12, -1):
            fonts = {k: font(k, size) for k in ("regular", "bold")}
            mono = font("mono", size - 1)
            lines = []
            for r in sc.rows:
                n = 1
                for ci, cell in enumerate(r):
                    f = mono if ci in sc.mono else (fonts["bold"] if ci in sc.accent or ci == 0 else fonts["regular"])
                    n = max(n, len(wrap(d, str(cell), f, widths[ci] - 22)))
                lines.append(n)
            hb = font("bold", size - 1)
            hl = max([len(wrap(d, str(c), hb, widths[ci] - 22)) for ci, c in enumerate(sc.columns)] or [1])
            head = (hl * (size - 1) * 1.18 + 16) if has_header else 0
            for pad in ((20, 12, 8) if len(sc.rows) <= 5 else (14, 8)):
                heights = [n * size * 1.24 + pad for n in lines]
                if sum(heights) + head <= avail_h and hl <= 2:
                    return size, heights, head
        return 13, [(avail_h - 44) / len(sc.rows)] * len(sc.rows), 44 if has_header else 0

    def _k_table(self, img, d, sc, fr, top, bottom):
        ncol = len(sc.columns) or len(sc.rows[0])
        weights = sc.widths or [1] * ncol
        total_w = W - 120
        widths = [total_w * w / sum(weights) for w in weights]
        has_header = bool(sc.columns)
        size, heights, head = self._table_layout(d, sc, widths, bottom - top, has_header)
        shown = self._visible(sc, fr, len(sc.rows))
        x0 = 60
        y = top
        if has_header:
            d.rounded_rectangle((x0, y, x0 + total_w, y + head), 10, fill=C["soft"])
            x = x0
            for ci, col in enumerate(sc.columns):
                f, lines = fit(d, str(col), "bold", size - 1, widths[ci] - 22, 2, min_size=11)
                yy = y + head / 2 - len(lines) * f.size * 0.6
                for ln in lines:
                    d.text((x + 11, yy), ln, font=f, fill=C["acc"])
                    yy += f.size * 1.18
                x += widths[ci]
            y += head + 4
        for ri, r in enumerate(sc.rows):
            h = heights[ri]
            if ri >= shown:
                break
            on = fr.focus == ri
            d.rounded_rectangle((x0, y, x0 + total_w, y + h - 4), 8, fill=C["soft"] if on else C["card"],
                                outline=C["acc"] if on else C["line"], width=2 if on else 1)
            if on:
                d.rounded_rectangle((x0, y, x0 + 6, y + h - 4), 3, fill=C["acc"])
            x = x0
            for ci, cell in enumerate(r):
                if ci in sc.mono:
                    f, col = font("mono", size - 1), C["ink"]
                elif ci in sc.accent:
                    f, col = font("bold", size), C["acc"]
                elif ci == 0:
                    f, col = font("bold", size), C["ink"]
                else:
                    f, col = font("regular", size), C["ink"]
                lines = wrap(d, str(cell), f, widths[ci] - 22)
                yy = y + (h - 4) / 2 - len(lines) * size * 1.24 / 2 + 1
                for ln in lines:
                    d.text((x + 11, yy), ln, font=f, fill=col)
                    yy += size * 1.24
                x += widths[ci]
            y += h

    def _code_panel(self, d, box, code: str, lang: str, hl: Optional[tuple[int, int]] = None,
                    max_size: int = 20, number: bool = True):
        x0, y0, x1, y1 = box
        d.rounded_rectangle(box, 12, fill=C["code"])
        lines = textwrap.dedent(code).strip("\n").split("\n")
        longest = max(len(ln) for ln in lines) + (4 if number else 0)
        lead = 1.32 if len(lines) <= 14 else 1.17
        size = int(min(max_size, (y1 - y0 - 24) / (len(lines) * lead), (x1 - x0 - 32) / (max(longest, 1) * 0.602)))
        size = max(size, 10)
        f = font("mono", size)
        step = size * lead
        y = y0 + 12 + max(0, ((y1 - y0 - 24) - len(lines) * step) / 2) if len(lines) <= 6 else y0 + 12
        gutter = (f.getlength("00 ") if number else 0)
        for i, ln in enumerate(lines, start=1):
            if hl and hl[0] <= i <= hl[1]:
                d.rectangle((x0 + 4, y - 2, x1 - 4, y + step - 2), fill=C["codehl"])
                d.rectangle((x0 + 4, y - 2, x0 + 9, y + step - 2), fill="#58A6FF")
            x = x0 + 16
            if number:
                d.text((x, y), f"{i:>2}", font=f, fill=C["codedim"])
                x += gutter
            for tok, col in code_tokens(ln, lang):
                d.text((x, y), tok, font=f, fill=C[col])
                x += f.getlength(tok)
            y += step
        return size

    def _k_code(self, img, d, sc, fr, top, bottom):
        hl = None
        it = sc.items[fr.focus] if fr.focus is not None else None
        if it:
            a, b = (it["lines"] + [it["lines"][0]])[:2] if isinstance(it["lines"], list) else (it["lines"], it["lines"])
            hl = (int(a), int(b))
        if sc.heading and not sc.sub:
            top = 124
        self._code_panel(d, (52, top, W - 52, bottom), sc.code, self._lang(sc), hl)
        if it:
            label = str(it["t"])
            f, lines = fit(d, label, "bold", 20, 520, 2, min_size=14)
            bw = max(f.getlength(ln) for ln in lines) + 36
            bh = len(lines) * f.size * 1.25 + 22
            bx1, by1 = W - 66, bottom - 14
            d.rounded_rectangle((bx1 - bw, by1 - bh, bx1, by1), 12, fill=C["acc"])
            yy = by1 - bh + 11
            for ln in lines:
                d.text((bx1 - bw + 18, yy), ln, font=f, fill="#FFFFFF")
                yy += f.size * 1.25

    def _lang(self, sc) -> str:
        for it in sc.items:
            if it.get("lang"):
                return str(it["lang"])
        return "lua" if "redis.call" in sc.code else ("sql" if re.match(r"\s*(SELECT|UPDATE|INSERT)", sc.code)
                                                       else ("bash" if sc.code.lstrip().startswith(("cd ", "mvn", "java "))
                                                             else "java"))

    def _k_pattern(self, img, d, sc, fr, top, bottom):
        has_stats = bool(sc.stats)
        stats_h = (36 + 34 * len(sc.stats)) if has_stats else 0
        panel_bottom = bottom - (stats_h + 12 if has_stats else 0)
        gap = 20
        pw = (W - 104 - gap) / 2
        for k, (label, code, tone) in enumerate(((sc.bad_label, sc.bad, "bad"), (sc.good_label, sc.good, "ok"))):
            x0 = 52 + k * (pw + gap)
            on = fr.focus == k
            col = C["bad"] if tone == "bad" else C["ok"]
            d.rounded_rectangle((x0, top, x0 + pw, top + 36), 10, fill=col if on else tint(col, 0.16))
            d.text((x0 + 16, top + 18), ("✗  " if tone == "bad" else "✓  ") + label, font=font("bold", 18),
                   fill="#FFFFFF" if on else col, anchor="lm")
            self._code_panel(d, (x0, top + 42, x0 + pw, panel_bottom), code, "java", None, max_size=21, number=False)
            if on:
                d.rounded_rectangle((x0 - 3, top - 3, x0 + pw + 3, panel_bottom + 3), 14, outline=col, width=3)
        if has_stats:
            y0 = bottom - stats_h
            on = fr.focus == 2
            d.rounded_rectangle((52, y0, W - 52, bottom), 12, fill=C["soft"] if on else C["card"],
                                outline=C["acc"] if on else C["line"], width=3 if on else 1)
            cols = sc.stats_columns
            weights = [3.2, 1.4, 1.4, 1.1, 2.4][:len(cols)]
            widths = [(W - 124) * w / sum(weights) for w in weights]
            x = 62
            for ci, c in enumerate(cols):
                d.text((x + 6, y0 + 10), c, font=font("bold", 15), fill=C["acc"])
                x += widths[ci]
            for ri, row in enumerate(sc.stats):
                x, yy = 62, y0 + 36 + ri * 34
                for ci, cell in enumerate(row):
                    colr = C["bad"] if ci == 1 else (C["ok"] if ci == 2 else (C["acc"] if ci == 3 else C["ink"]))
                    f, lines = fit(d, str(cell), "bold" if ci in (0, 3) else "regular", 18, widths[ci] - 12, 1, min_size=12)
                    d.text((x + 6, yy + 4), lines[0], font=f, fill=colr)
                    x += widths[ci]

    def _k_compare(self, img, d, sc, fr, top, bottom):
        n = len(sc.items)
        gap = 24
        bw = (W - 120 - gap * (n - 1)) / n
        for i, it in enumerate(sc.items):
            x0, x1 = 60 + i * (bw + gap), 60 + i * (bw + gap) + bw
            on = fr.focus == i
            tone = it.get("tone", "")
            col, soft = TONES.get(tone, TONES[""])
            d.rounded_rectangle((x0, top, x1, bottom), 16, fill=C["card"], outline=C[col] if on else C["line"],
                                width=4 if on else 2)
            d.rounded_rectangle((x0 + 2, top + 2, x1 - 2, top + 64), 14, fill=C[soft])
            d.rectangle((x0 + 2, top + 44, x1 - 2, top + 64), fill=C[soft])
            block(d, (x0 + 20, top + 16), str(it["title"]), "bold", 23, bw - 40, C[col], max_lines=1)
            y = top + 80
            lines = it["lines"]
            most = max(len(x["lines"]) for x in sc.items)
            size = 21 if most <= 5 else (19 if most <= 7 else 17)
            for ln in lines:
                d.ellipse((x0 + 22, y + 9, x0 + 30, y + 17), fill=C[col])
                y = block(d, (x0 + 42, y), str(ln), "regular", size, bw - 64, C["ink"], max_lines=3, lead=1.2,
                          min_size=13) + 8
                if y > bottom - 24:
                    break

    def _k_flow(self, img, d, sc, fr, top, bottom):
        n = len(sc.items)
        shown = self._visible(sc, fr, n)
        gap = 46
        bw = (W - 120 - gap * (n - 1)) / n
        has_f = any(it.get("f") for it in sc.items)
        bh = min(190, (bottom - top) - (96 if has_f else 20))
        y0 = top + 6
        for i, it in enumerate(sc.items[:shown]):
            x0 = 60 + i * (bw + gap)
            on = fr.focus == i
            tone = it.get("tone", "")
            col, soft = TONES.get(tone, TONES[""])
            d.rounded_rectangle((x0, y0, x0 + bw, y0 + bh), 16, fill=C[soft] if on else C["card"],
                                outline=C[col] if on else C["line"], width=3 if on else 2)
            d.ellipse((x0 + 14, y0 + 14, x0 + 44, y0 + 44), fill=C[col])
            d.text((x0 + 29, y0 + 30), str(it.get("k", i + 1)), font=font("bold", 15), fill="#FFFFFF", anchor="mm")
            y = block(d, (x0 + 54, y0 + 16), str(it["t"]), "bold", 21, bw - 66, C["ink"], max_lines=2, lead=1.15,
                      min_size=13)
            if it.get("s"):
                block(d, (x0 + 16, max(y + 8, y0 + 64)), str(it["s"]), "regular", 17, bw - 32, C["muted"],
                      max_lines=5, lead=1.22, min_size=12)
            if it.get("f"):
                f, lines = fit(d, str(it["f"]), "mono", 16, bw, 4, min_size=11)
                yy = y0 + bh + 14
                for ln in lines:
                    d.text((x0 + bw / 2, yy), ln, font=f, fill=C["acc"], anchor="ma")
                    yy += f.size * 1.25
            if i < n - 1 and i + 1 < shown:
                ax = x0 + bw + 8
                ay = y0 + bh / 2
                d.line((ax, ay, ax + gap - 18, ay), fill=C["dim"], width=4)
                d.polygon([(ax + gap - 16, ay - 9), (ax + gap - 4, ay), (ax + gap - 16, ay + 9)], fill=C["dim"])

    def _k_stats(self, img, d, sc, fr, top, bottom):
        n = len(sc.items)
        shown = self._visible(sc, fr, n)
        cols = n if n <= 4 else math.ceil(n / 2)
        rows = math.ceil(n / cols)
        gap = 22
        cw = (W - 120 - gap * (cols - 1)) / cols
        ch = min(230, (bottom - top - gap * (rows - 1)) / rows)
        for i, it in enumerate(sc.items[:shown]):
            r, c = divmod(i, cols)
            x0, y0 = 60 + c * (cw + gap), top + r * (ch + gap)
            on = fr.focus == i
            tone = it.get("tone", "")
            col, soft = TONES.get(tone, TONES[""])
            d.rounded_rectangle((x0, y0, x0 + cw, y0 + ch), 16, fill=C[soft] if on else C["card"],
                                outline=C[col] if on else C["line"], width=3 if on else 2)
            f, lines = fit(d, str(it["v"]), "bold", 44, cw - 36, 1, min_size=18)
            d.text((x0 + 18, y0 + 16), lines[0], font=f, fill=C[col])
            y = block(d, (x0 + 18, y0 + 26 + f.size * 1.15), str(it["l"]), "bold", 21, cw - 36, C["ink"], max_lines=2,
                      lead=1.18, min_size=14)
            if it.get("s"):
                block(d, (x0 + 18, y + 6), str(it["s"]), "regular", 18, cw - 36, C["muted"],
                      max_lines=max(1, int((y0 + ch - y - 12) // 23)), lead=1.22, min_size=13)

    def _k_exercise(self, img, d, sc, fr, top, bottom):
        label = "Exercise " + sc.n if sc.n else "Exercise"
        f = font("bold", 18)
        lw = d.textlength(label, font=f) + 30
        d.rounded_rectangle((60, top, 60 + lw, top + 36), 18, fill=C["acc"])
        d.text((60 + 15, top + 18), label, font=f, fill="#FFFFFF", anchor="lm")
        y = block(d, (60, top + 50), sc.q, "bold", 25, W - 120, C["ink"], max_lines=4, lead=1.22, min_size=16)
        if sc.items:
            y0 = y + 14
            n = len(sc.items)
            avail = bottom - y0 - 48
            size = 21 if avail / max(n, 1) >= 40 else (19 if avail / max(n, 1) >= 32 else 16)
            rows = [fit(d, str(it["t"]), "regular", size, W - 190, 2, 13) for it in sc.items]
            need = sum(len(ls) * f.size * 1.18 + 8 for f, ls in rows)
            y1 = min(bottom, y0 + 50 + need)
            d.rounded_rectangle((60, y0, W - 60, y1), 14, fill=C["card"], outline=C["line"], width=2)
            d.text((82, y0 + 14), "Given", font=font("bold", 17), fill=C["muted"])
            yy = y0 + 44
            for f, ls in rows:
                d.ellipse((84, yy + f.size * 0.42, 92, yy + f.size * 0.42 + 8), fill=C["acc"])
                for ln in ls:
                    d.text((104, yy), ln, font=f, fill=C["ink"])
                    yy += f.size * 1.18
                yy += 8

    def _k_speech(self, img, d, sc, fr, top, bottom):
        d.rounded_rectangle((52, top, W - 52, bottom), 16, fill=C["card"], outline=C["line"], width=2)
        words: list[tuple[str, int]] = [(w, i) for i, it in enumerate(sc.items) for w in str(it["t"]).split()]
        width = W - 52 * 2 - 60
        for size in range(26, 14, -1):
            f = font("regular", size)
            space = f.getlength(" ")
            lines, cur, cw = [], [], 0.0
            for w, i in words:
                wl = f.getlength(w)
                if cur and cw + space + wl > width:
                    lines.append(cur)
                    cur, cw = [], 0.0
                cur.append((w, i, wl))
                cw += (space if len(cur) > 1 else 0) + wl
            if cur:
                lines.append(cur)
            step = size * 1.42
            if len(lines) * step <= bottom - top - 40:
                break
        y = top + 20
        for line in lines:
            x = 52 + 30
            for k, (w, i, wl) in enumerate(line):
                if i == fr.focus:
                    pad_l = 0 if k == 0 or line[k - 1][1] != i else space
                    d.rectangle((x - pad_l, y - 3, x + wl + 2, y + size * 1.22), fill=C["soft"])
                x += wl + space
            x = 52 + 30
            for w, i, wl in line:
                d.text((x, y), w, font=f, fill=C["ink"] if fr.focus is None or i == fr.focus else C["muted"])
                x += wl + space
            y += step

    # ---------- sơ đồ ----------
    def _k_diagram(self, img, d, sc, fr, top, bottom):
        step = fr.focus or 0
        sx, sy = 60, top
        scale_y = (bottom - top) / 390
        boxes = {}
        for nd in sc.nodes:
            cx, cy = sx + nd.x, sy + nd.y * scale_y
            boxes[nd.id] = (cx - nd.w / 2, cy - nd.h * scale_y / 2, cx + nd.w / 2, cy + nd.h * scale_y / 2)
        for nd in sc.nodes:  # vùng (AZ, region) vẽ trước, nằm dưới
            if nd.shape == "zone" and nd.at <= step:
                x0, y0, x1, y1 = boxes[nd.id]
                dashed_rect(d, (x0, y0, x1, y1), C["zone"], width=2)
                d.text((x0 + 14, y0 + 8), nd.label, font=font("bold", 15), fill=C["zone"])
        for e in sc.edges:
            a = next(n for n in sc.nodes if n.id == e.a)
            b = next(n for n in sc.nodes if n.id == e.b)
            if e.at > step or a.at > step or b.at > step:
                continue
            self._edge(d, boxes[e.a], boxes[e.b], e.label, e.dashed, hot=(step > 0 and e.at == step))
        for nd in sc.nodes:
            if nd.shape == "zone" or nd.at > step:
                continue
            self._node(d, nd, boxes[nd.id], hot=(step > 0 and nd.at == step))

    def _node(self, d, nd, box, hot: bool):
        x0, y0, x1, y1 = box
        col, soft = TONES.get(nd.tone, TONES[""])
        outline = C[col] if (hot or nd.tone) else C["line"]
        fill = C[soft] if (hot or nd.tone) else C["card"]
        wdt = 3 if hot else 2
        if nd.shape == "db":
            e = min(16, (y1 - y0) * 0.22)
            d.rectangle((x0, y0 + e / 2, x1, y1 - e / 2), fill=fill)
            d.ellipse((x0, y1 - e, x1, y1), fill=fill, outline=outline, width=wdt)
            d.rectangle((x0 + wdt, y0 + e / 2, x1 - wdt, y1 - e / 2), fill=fill)
            d.line((x0, y0 + e / 2, x0, y1 - e / 2), fill=outline, width=wdt)
            d.line((x1, y0 + e / 2, x1, y1 - e / 2), fill=outline, width=wdt)
            d.ellipse((x0, y0, x1, y0 + e), fill=tint(C[col], 0.18) if (hot or nd.tone) else C["soft"],
                      outline=outline, width=wdt)
            ty0 = y0 + e
        elif nd.shape == "queue":
            d.rounded_rectangle(box, 10, fill=fill, outline=outline, width=wdt)
            for k in range(3):
                xx = x1 - 14 - k * 9
                d.line((xx, y0 + 8, xx, y1 - 8), fill=outline, width=2)
            ty0 = y0
            x1 = x1 - 34
        elif nd.shape == "user":
            d.rounded_rectangle(box, 22, fill=C["soft"], outline=C["acc"], width=wdt)
            ty0 = y0
        else:
            d.rounded_rectangle(box, 10, fill=fill, outline=outline, width=wdt)
            ty0 = y0
        tw = x1 - x0 - 16
        f, lines = fit(d, nd.label, "bold", 16, tw, 2, min_size=11)
        sub_lines: list[str] = []
        fs = font("regular", 13)
        if nd.sub:
            fs, sub_lines = fit(d, nd.sub, "regular", 13, tw, 3, min_size=10)
        total = len(lines) * f.size * 1.18 + len(sub_lines) * fs.size * 1.2 + (4 if sub_lines else 0)
        y = ty0 + ((y1 - ty0) - total) / 2
        cx = (x0 + x1) / 2
        for ln in lines:
            d.text((cx, y), ln, font=f, fill=C["ink"], anchor="ma")
            y += f.size * 1.18
        y += 4 if sub_lines else 0
        for ln in sub_lines:
            d.text((cx, y), ln, font=fs, fill=C["muted"], anchor="ma")
            y += fs.size * 1.2

    @staticmethod
    def _border_point(box, tx, ty):
        """Điểm trên viền hộp theo hướng từ tâm hộp tới (tx, ty)."""
        x0, y0, x1, y1 = box
        cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
        dx, dy = tx - cx, ty - cy
        if dx == 0 and dy == 0:
            return cx, cy
        hw, hh = (x1 - x0) / 2, (y1 - y0) / 2
        t = min(hw / abs(dx) if dx else math.inf, hh / abs(dy) if dy else math.inf)
        return cx + dx * t, cy + dy * t

    def _edge(self, d, a, b, label, dashed, hot):
        ca = ((a[0] + a[2]) / 2, (a[1] + a[3]) / 2)
        cb = ((b[0] + b[2]) / 2, (b[1] + b[3]) / 2)
        p0 = self._border_point(a, *cb)
        p1 = self._border_point(b, *ca)
        col = C["acc"] if hot else "#7D8896"
        ang = math.atan2(p1[1] - p0[1], p1[0] - p0[0])
        end = (p1[0] - 10 * math.cos(ang), p1[1] - 10 * math.sin(ang))
        if dashed:
            dashed_line(d, p0, end, col, width=3 if hot else 2)
        else:
            d.line((*p0, *end), fill=col, width=3 if hot else 2)
        tip = (p1[0] - 2 * math.cos(ang), p1[1] - 2 * math.sin(ang))
        left = (tip[0] - 13 * math.cos(ang - 0.42), tip[1] - 13 * math.sin(ang - 0.42))
        right = (tip[0] - 13 * math.cos(ang + 0.42), tip[1] - 13 * math.sin(ang + 0.42))
        d.polygon([tip, left, right], fill=col)
        if label:
            f = font("regular", 13)
            mx, my = (p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2
            tw = d.textlength(label, font=f)
            d.rounded_rectangle((mx - tw / 2 - 6, my - 10, mx + tw / 2 + 6, my + 10), 6, fill=C["bg"])
            d.text((mx, my), label, font=f, fill=C["acc"] if hot else C["muted"], anchor="mm")
