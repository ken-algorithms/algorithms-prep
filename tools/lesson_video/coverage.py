"""Bảng độ phủ: mỗi ý chính trong points.yaml đã được video nào dạy, ở giây thứ mấy."""
from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Optional

from . import script as sc


def mmss(t: float) -> str:
    t = int(round(t))
    return f"{t // 60}:{t % 60:02d}"


def collect(lessons_dir: Path, points_path: Path, week: Optional[int] = None) -> dict:
    points = sc.load_points(points_path)
    manifest = {}
    mpath = lessons_dir / "lessons.json"
    if mpath.exists():
        manifest = {x["id"]: x for x in json.loads(mpath.read_text(encoding="utf-8")).get("lessons", [])}
    lessons = []
    for p in sorted(lessons_dir.glob("ep*.yaml")):
        L = sc.load(p)
        if week is not None and L.week not in (week, 0):
            continue
        lessons.append((p, L))
    claims: dict[str, list[dict]] = {pid: [] for pid in points}
    for p, L in lessons:
        times = {x["id"]: x["t"] for x in manifest.get(L.id, {}).get("points", [])}
        for i, scene in enumerate(L.scenes):
            chapter = next((s.chapter for s in reversed(L.scenes[:i + 1]) if s.chapter), "")
            for pid in scene.points:
                if pid in claims and all(c["ep"] != L.ep for c in claims[pid]):
                    claims[pid].append({"ep": L.ep, "id": L.id, "chapter": chapter, "t": times.get(pid)})
    import yaml
    raw = yaml.safe_load(Path(points_path).read_text(encoding="utf-8")) or {}
    scope = [a for w, anchors in (raw.get("scope") or {}).items() if week is None or int(w) == week for a in anchors]
    covered: dict[str, list[int]] = {a: [] for a in scope}
    for p, L in lessons:
        refs = set(L.all_covers()) | {points[pid]["src"] for s in L.scenes for pid in s.points if pid in points}
        for a in scope:
            if a in refs and L.ep not in covered[a]:
                covered[a].append(L.ep)
    src_dir = sc.source_dir(lessons[0][0], lessons[0][1]) if lessons else None
    titles = {}
    if src_dir:
        for a in scope:
            f, _, anchor = a.partition("#")
            for _, title, slug in sc.headings(src_dir / f):
                if slug == anchor:
                    titles[a] = title
    return {"points": points, "claims": claims, "lessons": lessons, "manifest": manifest,
            "scope": covered, "titles": titles}


def to_markdown(data: dict, title: str, link_prefix: str = "") -> str:
    points, claims = data["points"], data["claims"]
    total = len(points)
    done = sum(1 for pid in points if claims[pid])
    out = [f"# {title}", "",
           f"**{done}/{total} ý chính đã có video.** Bảng sinh bằng `python -m lesson_video coverage` từ "
           "`points.yaml` (ý chính lấy từ tài liệu nguồn) và kịch bản `ep*.yaml`; mỗi ý có chữ bắt buộc "
           "(`expect`) mà lệnh `check` đã kiểm là có mặt trong cảnh dạy ý đó. Thời điểm lấy từ `lessons.json` "
           "sau khi dựng.", ""]
    rows_by_ep: dict[int, int] = {}
    for pid in points:
        for c in claims[pid]:
            rows_by_ep[c["ep"]] = rows_by_ep.get(c["ep"], 0) + 1
    out += ["## Theo video", "", "| Video | Dài | Chương | Ý chính |", "|---|---:|---:|---:|"]
    for p, L in data["lessons"]:
        m = data["manifest"].get(L.id)
        dur = mmss(m["duration"]) if m else "chưa dựng"
        chap = len(m["chapters"]) if m else len(L.chapters())
        out.append(f"| Ep{L.ep:02d} · {L.title} | {dur} | {chap} | {rows_by_ep.get(L.ep, 0)} |")
    out.append("")
    if data.get("scope"):
        sc_ok = sum(1 for v in data["scope"].values() if v)
        out += ["## Theo mục của tài liệu nguồn", "",
                f"**{sc_ok}/{len(data['scope'])} mục có video.** Mỗi mục là một heading trong phạm vi của tuần; "
                "mục có video khi nằm trong `covers` của video hoặc là nguồn của một ý chính đã dạy.", "",
                "| | Mục | Video |", "|:---:|---|---|"]
        for a, eps in data["scope"].items():
            f = a.split("#")[0]
            title = data["titles"].get(a, a)
            link = f"{link_prefix}{a}" if link_prefix else a
            out.append(f"| {'✅' if eps else '❌'} | [{f.split('-')[0]} · {title}]({link}) | "
                       f"{', '.join(f'Ep{e:02d}' for e in sorted(eps)) or '**chưa có**'} |")
        out.append("")
    sections: dict[str, list[str]] = {}
    for pid, pt in points.items():
        sections.setdefault(pt["src"] + "\u0000" + pt["section"], []).append(pid)
    out += ["## Theo mục nguồn", ""]
    for key, pids in sections.items():
        src, sec = key.split("\u0000")
        f, _, anchor = src.partition("#")
        link = f"{link_prefix}{src}" if link_prefix else src
        out += [f"### {sec}", "", f"Nguồn: [{f}{'#' + anchor if anchor else ''}]({link})", "",
                "| | Ý chính | Video · thời điểm |", "|:---:|---|---|"]
        for pid in pids:
            pt = points[pid]
            cs = claims[pid]
            where = ", ".join(f"Ep{c['ep']:02d}" + (f" · {mmss(c['t'])}" if c["t"] is not None else "")
                              + (f" ({c['chapter']})" if c["chapter"] else "") for c in cs) or "**chưa có**"
            out.append(f"| {'✅' if cs else '❌'} | {pt['text']} | {where} |")
        out.append("")
    missing = [pid for pid in points if not claims[pid]]
    if missing:
        out += ["## Còn thiếu", ""] + [f"- `{pid}` — {points[pid]['text']}" for pid in missing] + [""]
    return "\n".join(out)


TOKEN = re.compile(r"\b[A-Za-z][A-Za-z0-9]*\b")
# mã của lộ trình (P09, V2, L1, Ep03…) và chữ hoa dùng để nhấn mạnh, không phải chữ viết tắt
IGNORE = re.compile(r"^(?:P\d\d|D\d\d|V\d+|L\d|Ep\d\d|EP\d\d|NOT|AND|OR|THE|BAD|FIX|ONE|ALL|NEW|YES|NO|"
                    r"SELECT|UPDATE|WHERE|FROM|LIMIT|INFO|DEBUG|FINE|WARN|ERROR|README|TODO|SKIP|LOCKED|FOR|INSERT|"
                    r"KB|MB|GB|TB|Mbit)$")  # đơn vị đo: giải thích một lần ở Ep01 và trong bảng chữ viết tắt


def acronym_like(t: str) -> bool:
    letters = [c for c in t if c.isalpha()]
    upper = sum(c.isupper() for c in letters)
    return upper >= 2 and upper / max(len(letters), 1) >= 0.5


def glossary_terms(md_path: Path) -> set[str]:
    terms: set[str] = set()
    for line in Path(md_path).read_text(encoding="utf-8").splitlines():
        if not line.startswith("| ") or line.startswith("| Viết tắt") or line.startswith("|---"):
            continue
        cell = line.split("|")[1]
        cell = re.sub(r"[*`]", "", cell)
        for part in re.split(r"[,/…]| … |\s+", cell):
            part = part.strip(" .()")
            if part:
                terms.add(part)
                terms.update(p for p in part.split("-") if p)
    return terms


def acronym_report(lesson: sc.Lesson, terms: set[str]) -> list[str]:
    """Chữ viết tắt trên màn hình hoặc trong lời đọc: phải có trong bảng chữ viết tắt và trong thẻ của video."""
    carded = {str(it["abbr"]).strip() for s in lesson.scenes if s.kind == "acronyms" for it in s.items}
    carded |= {p.strip() for c in list(carded) for p in re.split(r"[,/·…]|\s+", c) if p.strip()}
    found: dict[str, int] = {}
    for i, scene in enumerate(lesson.scenes):
        if scene.kind == "acronyms":
            continue
        blob = "\n".join([scene.heading, scene.sub, scene.q, scene.note]
                         + [str(v) for it in scene.items for k, v in it.items() if k not in ("lines", "lang")]
                         + [c for r in scene.rows for c in r] + scene.columns + [c for r in scene.stats for c in r]
                         + [n.label + " " + n.sub for n in scene.nodes] + [e.label for e in scene.edges]
                         + [ln.text for ln in scene.lines])
        for m in TOKEN.finditer(blob):
            t = m.group(0)
            if not acronym_like(t) or IGNORE.match(t):
                continue
            found.setdefault(t, i)
    problems = []
    for t, i in sorted(found.items(), key=lambda kv: kv[1]):
        base = t[:-1] if t.endswith("s") and t[:-1] in terms else t
        if base not in terms:
            problems.append(f"cảnh {i}: '{t}' không có trong bảng chữ viết tắt")
        elif base not in carded:
            problems.append(f"cảnh {i}: '{t}' chưa có trong thẻ chữ viết tắt của video")
    return problems
