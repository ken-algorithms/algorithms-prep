#!/usr/bin/env python3
"""Build a static, offline HTML site from every .md file in this repo.

    uv run --with markdown --with pymdown-extensions python3 build_site.py

Output goes to ./site/ — open ./site/index.html in a browser. No CDN, no network.
Re-run any time you add or edit a markdown file.
"""
from __future__ import annotations

import html
import json
import re
import shutil
import unicodedata
from dataclasses import dataclass, field
from pathlib import Path

import markdown

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "site"
SKIP_DIRS = {".git", "site", "__pycache__", "target", ".venv", "node_modules", ".idea"}

# Nav grouping: (directory prefix, section label, sort weight)
SECTIONS = [
    ("nab-prep", "NAB", 10),
    ("katalon-prep/katalon-system-design", "System Design", 20),
    ("katalon-prep/katalon-self-questions", "Self Questions", 30),
    ("katalon-prep/katalon-prep-common", "Khái niệm chung", 40),
    ("katalon-prep/katalon-prep-java", "Java", 50),
    ("katalon-prep/katalon-prep-python", "Python", 60),
    ("katalon-prep", "Katalon", 15),
    ("leetcode-38-bai-java", "LeetCode Java", 70),
    ("leetcode-38-bai", "LeetCode", 71),
    ("", "Gốc", 5),
]


def gh_slug(text: str, separator: str = "-") -> str:
    """GitHub's heading-anchor algorithm — must match anchors hand-written in the .md files."""
    text = re.sub(r"`", "", text.strip())
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)      # [label](url) -> label
    text = re.sub(r"<[^>]+>", "", text)                        # strip inline html
    text = text.lower()
    text = re.sub(r"[^\w\s-]", "", text, flags=re.UNICODE)
    return re.sub(r"\s", separator, text)


@dataclass
class Page:
    src: Path
    rel: str                      # e.g. "nab-prep/01-nab-research.md"
    out_rel: str                  # e.g. "nab-prep/01-nab-research.html"
    title: str
    section: str
    weight: int
    body: str = ""
    toc: list = field(default_factory=list)
    words: int = 0
    text: str = ""


def first_heading(md_text: str) -> str | None:
    fence = False
    for line in md_text.splitlines():
        if line.startswith("```"):
            fence = not fence
            continue
        if fence:
            continue
        m = re.match(r"^#\s+(.*)$", line)
        if m:
            t = re.sub(r"`", "", m.group(1)).strip()
            t = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", t)
            return t
    return None


def section_for(rel: str) -> tuple[str, int]:
    best = ("Gốc", 999)
    best_len = -1
    for prefix, label, weight in SECTIONS:
        if prefix == "":
            if "/" not in rel and best_len < 0:
                best, best_len = (label, weight), 0
            continue
        if rel.startswith(prefix + "/") and len(prefix) > best_len:
            best, best_len = (label, weight), len(prefix)
    return best


def collect() -> list[Page]:
    pages: list[Page] = []
    for p in sorted(ROOT.rglob("*.md")):
        if any(part in SKIP_DIRS for part in p.relative_to(ROOT).parts):
            continue
        rel = p.relative_to(ROOT).as_posix()
        raw = p.read_text(encoding="utf-8", errors="ignore")
        title = first_heading(raw) or p.stem
        label, weight = section_for(rel)
        pages.append(Page(
            src=p, rel=rel, out_rel=rel[:-3] + ".html",
            title=title, section=label, weight=weight,
        ))
    return pages


LINK_RE = re.compile(r'(?<=\]\()([^)\s]+?)(\)|\s)')


def resolve_rel(page_dir: Path, path: str) -> str | None:
    """Repo-relative posix path for a link target, or None if it escapes the repo."""
    try:
        return (page_dir / path).resolve().relative_to(ROOT).as_posix()
    except ValueError:
        return None


def rewrite_links(md_text: str, page_dir: Path, known_md: set[str],
                  code_files: set[str], dir_pages: set[str]) -> str:
    """Point .md links at generated .html, and source-code links at their code-view page."""
    def sub(m: re.Match) -> str:
        target, tail = m.group(1), m.group(2)
        if target.startswith(("http://", "https://", "mailto:", "#")):
            return target + tail
        path, sep, anchor = target.partition("#")
        if not path:
            return target + tail
        if path.endswith(".md"):
            return path[:-3] + ".html" + sep + anchor + tail
        rel = resolve_rel(page_dir, path.rstrip("/"))
        if rel is None:
            return target + tail
        # directory -> its README page, else a generated listing
        if rel + "/README.md" in known_md:
            return path.rstrip("/") + "/README.html" + sep + anchor + tail
        if rel in dir_pages:
            return path.rstrip("/") + "/index.html" + sep + anchor + tail
        # source file -> its code-view page
        if rel in code_files:
            return path + ".html" + sep + anchor + tail
        return target + tail
    return LINK_RE.sub(sub, md_text)


def scan_targets(pages: list[Page], known_md: set[str]) -> tuple[set[str], set[str]]:
    """Find every local link target that isn't markdown, so we can make it viewable."""
    code_files: set[str] = set()
    dir_pages: set[str] = set()
    for page in pages:
        raw = page.src.read_text(encoding="utf-8", errors="ignore")
        for m in LINK_RE.finditer(raw):
            target = m.group(1)
            if target.startswith(("http://", "https://", "mailto:", "#")):
                continue
            path = target.partition("#")[0].rstrip("/")
            if not path or path.endswith(".md"):
                continue
            rel = resolve_rel(page.src.parent, path)
            if rel is None:
                continue
            abs_p = ROOT / rel
            if abs_p.is_dir():
                if rel + "/README.md" not in known_md:
                    dir_pages.add(rel)
            elif abs_p.is_file():
                code_files.add(rel)
    return code_files, dir_pages


def render(pages: list[Page], code_files: set[str], dir_pages: set[str]) -> None:
    known = {p.rel for p in pages}
    md = markdown.Markdown(extensions=[
        "tables", "fenced_code", "toc", "attr_list", "sane_lists",
        "md_in_html", "footnotes", "pymdownx.superfences", "pymdownx.tasklist",
    ], extension_configs={
        "toc": {"slugify": gh_slug, "permalink": "¶", "toc_depth": "2-3"},
    })
    for page in pages:
        raw = page.src.read_text(encoding="utf-8", errors="ignore")
        page.words = len(raw.split())
        body_md = rewrite_links(raw, page.src.parent, known, code_files, dir_pages)
        md.reset()
        page.body = md.convert(body_md)
        page.toc = [
            {"level": t["level"], "id": t["id"], "name": re.sub(r"¶$", "", t["name"])}
            for t in getattr(md, "toc_tokens", [])
        ]
        plain = re.sub(r"<[^>]+>", " ", page.body)
        plain = html.unescape(re.sub(r"\s+", " ", plain))
        page.text = plain[:3000]


CSS = """
:root{--bg:#fbfbfd;--fg:#1c1c22;--muted:#6b6b78;--line:#e3e3ea;--card:#fff;--accent:#0b63d6;
--accent-soft:#e8f0fd;--code-bg:#f4f4f8;--warn:#b45309;--ok:#0f7a4d;--sidebar:#f5f5f8}
:root[data-theme=dark]{--bg:#121216;--fg:#e6e6ec;--muted:#9a9aa8;--line:#2a2a33;--card:#1a1a20;
--accent:#6aa8ff;--accent-soft:#17243b;--code-bg:#1e1e26;--warn:#f0b429;--ok:#4ade80;--sidebar:#16161c}
*{box-sizing:border-box}
html,body{margin:0;padding:0}
body{background:var(--bg);color:var(--fg);font:16px/1.68 -apple-system,BlinkMacSystemFont,"Segoe UI",
"Helvetica Neue",Arial,"Noto Sans",sans-serif;-webkit-font-smoothing:antialiased}
a{color:var(--accent);text-decoration:none}
a:hover{text-decoration:underline}
.layout{display:grid;grid-template-columns:290px minmax(0,1fr);min-height:100vh}
/* sidebar */
.side{background:var(--sidebar);border-right:1px solid var(--line);padding:18px 14px 60px;
position:sticky;top:0;height:100vh;overflow-y:auto}
.brand{font-weight:700;font-size:15px;letter-spacing:-.01em;display:block;margin-bottom:2px;color:var(--fg)}
.brand small{display:block;font-weight:400;color:var(--muted);font-size:12px;margin-top:2px}
.search{width:100%;margin:14px 0 10px;padding:8px 10px;border:1px solid var(--line);border-radius:8px;
background:var(--card);color:var(--fg);font-size:13px}
.search:focus{outline:2px solid var(--accent-soft);border-color:var(--accent)}
.navsec{margin-top:14px}
.navsec>h4{margin:0 0 6px;font-size:11px;letter-spacing:.09em;text-transform:uppercase;color:var(--muted)}
.navsec a{display:block;padding:5px 9px;border-radius:7px;font-size:13.5px;color:var(--fg);
line-height:1.4;margin-bottom:1px}
.navsec a:hover{background:var(--accent-soft);text-decoration:none}
.navsec a.active{background:var(--accent);color:#fff;font-weight:600}
.navsec a .p{display:block;font-size:11px;color:var(--muted);margin-top:1px}
.navsec a.active .p{color:rgba(255,255,255,.8)}
.hidden{display:none !important}
/* content */
.main{padding:0;min-width:0}
.topbar{position:sticky;top:0;z-index:5;background:var(--bg);border-bottom:1px solid var(--line);
padding:10px 28px;display:flex;gap:12px;align-items:center;justify-content:space-between}
.crumb{font-size:12.5px;color:var(--muted);min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.btn{border:1px solid var(--line);background:var(--card);color:var(--fg);border-radius:7px;
padding:5px 11px;font-size:12.5px;cursor:pointer;white-space:nowrap}
.btn:hover{border-color:var(--accent)}
.wrap{display:grid;grid-template-columns:minmax(0,1fr) 232px;gap:30px;padding:26px 28px 90px;max-width:1400px}
article{min-width:0}
article h1{font-size:29px;line-height:1.25;margin:.1em 0 .5em;letter-spacing:-.02em}
article h2{font-size:21px;margin:1.9em 0 .55em;padding-top:.45em;border-top:1px solid var(--line);letter-spacing:-.01em}
article h3{font-size:16.5px;margin:1.5em 0 .45em}
article h4{font-size:14.5px;margin:1.2em 0 .4em;color:var(--muted)}
article .headerlink{opacity:0;margin-left:.35em;font-weight:400;text-decoration:none}
article h2:hover .headerlink,article h3:hover .headerlink{opacity:.45}
article p{margin:.7em 0}
article ul,article ol{padding-left:1.35em;margin:.7em 0}
article li{margin:.28em 0}
article code{background:var(--code-bg);padding:.14em .38em;border-radius:5px;font-size:.87em;
font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace}
article pre{background:var(--code-bg);border:1px solid var(--line);border-radius:10px;padding:13px 15px;
overflow-x:auto;font-size:12.7px;line-height:1.55}
article pre code{background:none;padding:0;font-size:inherit}
article blockquote{margin:1em 0;padding:.65em 1em;border-left:3px solid var(--accent);
background:var(--accent-soft);border-radius:0 9px 9px 0}
article blockquote p:first-child{margin-top:0}
article blockquote p:last-child{margin-bottom:0}
.tablewrap{overflow-x:auto;margin:1em 0;border:1px solid var(--line);border-radius:10px}
article table{border-collapse:collapse;width:100%;font-size:13.5px}
article th,article td{border-bottom:1px solid var(--line);padding:8px 11px;text-align:left;vertical-align:top}
article th{background:var(--code-bg);font-weight:650;white-space:nowrap}
article tr:last-child td{border-bottom:none}
article img{max-width:100%}
article hr{border:none;border-top:1px solid var(--line);margin:2em 0}
article details{margin:.8em 0;border:1px solid var(--line);border-radius:9px;padding:9px 13px;background:var(--card)}
article summary{cursor:pointer;font-weight:600}
/* right toc */
.toc{position:sticky;top:70px;align-self:start;max-height:calc(100vh - 100px);overflow-y:auto;
font-size:12.5px;border-left:1px solid var(--line);padding-left:14px}
.toc h5{margin:0 0 7px;font-size:10.5px;letter-spacing:.09em;text-transform:uppercase;color:var(--muted)}
.toc a{display:block;padding:3px 0;color:var(--muted);line-height:1.4}
.toc a:hover{color:var(--accent);text-decoration:none}
.toc a.lv3{padding-left:11px;font-size:12px}
/* home */
.hero{padding:44px 28px 12px;max-width:1080px}
.hero h1{font-size:34px;margin:0 0 8px;letter-spacing:-.02em}
.hero p.lead{font-size:16px;color:var(--muted);margin:0 0 24px;max-width:70ch}
.stats{display:flex;gap:10px;flex-wrap:wrap;margin-bottom:30px}
.stat{background:var(--card);border:1px solid var(--line);border-radius:11px;padding:11px 16px;min-width:104px}
.stat b{display:block;font-size:21px;letter-spacing:-.01em}
.stat span{font-size:11.5px;color:var(--muted)}
.tracks{display:grid;grid-template-columns:repeat(auto-fit,minmax(310px,1fr));gap:16px;
padding:0 28px 60px;max-width:1080px}
.track{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:19px 21px}
.track h3{margin:0 0 4px;font-size:18px}
.track .sub{color:var(--muted);font-size:13px;margin:0 0 13px}
.track ol{margin:0;padding-left:1.2em;font-size:14px}
.track li{margin:.34em 0}
.pill{display:inline-block;font-size:10.5px;padding:2px 8px;border-radius:999px;
background:var(--accent-soft);color:var(--accent);font-weight:650;margin-left:6px;vertical-align:middle}
#results{padding:0 14px}
#results .r{display:block;padding:7px 9px;border-radius:7px;font-size:13px;margin-bottom:2px}
#results .r:hover{background:var(--accent-soft);text-decoration:none}
#results .r small{display:block;color:var(--muted);font-size:11px}
@media(max-width:1080px){.wrap{grid-template-columns:minmax(0,1fr)}.toc{display:none}}
@media(max-width:820px){.layout{grid-template-columns:1fr}.side{position:static;height:auto}}
"""

JS = """
(function(){
  var root=document.documentElement;
  try{var t=localStorage.getItem('prep-theme'); if(t) root.setAttribute('data-theme',t);}catch(e){}
  window.toggleTheme=function(){
    var cur=root.getAttribute('data-theme');
    if(!cur) cur = matchMedia('(prefers-color-scheme: dark)').matches ? 'dark':'light';
    var next = cur==='dark' ? 'light':'dark';
    root.setAttribute('data-theme',next);
    try{localStorage.setItem('prep-theme',next);}catch(e){}
  };
  // wrap tables so wide ones scroll instead of breaking the page
  document.querySelectorAll('article table').forEach(function(tb){
    if(tb.parentElement.classList.contains('tablewrap')) return;
    var w=document.createElement('div'); w.className='tablewrap';
    tb.parentNode.insertBefore(w,tb); w.appendChild(tb);
  });
  // sidebar filter + full-text search
  var box=document.getElementById('q'), res=document.getElementById('results'),
      nav=document.getElementById('nav');
  if(!box) return;
  function esc(s){return s.replace(/[&<>"]/g,function(c){
    return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
  box.addEventListener('input',function(){
    var q=box.value.trim().toLowerCase();
    if(!q){ nav.classList.remove('hidden'); res.classList.add('hidden'); res.innerHTML=''; return; }
    nav.classList.add('hidden'); res.classList.remove('hidden');
    var base=(window.SITE_BASE||'');
    var hits=[];
    (window.SEARCH_INDEX||[]).forEach(function(p){
      var hay=(p.t+' '+p.s+' '+p.x).toLowerCase();
      var i=hay.indexOf(q);
      if(i<0) return;
      var ctx=p.x.substr(Math.max(0,i-60),160);
      hits.push({p:p,i:i,ctx:ctx});
    });
    hits.sort(function(a,b){return a.i-b.i;});
    res.innerHTML = hits.length ? hits.slice(0,40).map(function(h){
      return '<a class="r" href="'+base+h.p.u+'">'+esc(h.p.t)+
             '<small>'+esc(h.p.s)+' — '+esc(h.ctx)+'…</small></a>';
    }).join('') : '<div style="padding:10px;color:var(--muted);font-size:13px">Không có kết quả</div>';
  });
  document.addEventListener('keydown',function(e){
    if(e.key==='/' && document.activeElement!==box){ e.preventDefault(); box.focus(); }
    if(e.key==='Escape' && document.activeElement===box){ box.value=''; box.dispatchEvent(new Event('input')); box.blur(); }
  });
})();
"""


def nav_html(pages: list[Page], current: str | None, base: str) -> str:
    groups: dict[tuple[int, str], list[Page]] = {}
    for p in pages:
        groups.setdefault((p.weight, p.section), []).append(p)
    out = ['<div id="nav">']
    for (_, label), items in sorted(groups.items()):
        items.sort(key=lambda x: (x.rel.count("/"), x.rel))
        out.append(f'<div class="navsec"><h4>{html.escape(label)}</h4>')
        for p in items:
            cls = " active" if p.rel == current else ""
            short = p.rel.rsplit("/", 1)[-1]
            out.append(
                f'<a class="nav{cls}" href="{base}{p.out_rel}">{html.escape(p.title[:70])}'
                f'<span class="p">{html.escape(short)}</span></a>'
            )
        out.append("</div>")
    out.append("</div>")
    out.append('<div id="results" class="hidden"></div>')
    return "\n".join(out)


def shell(*, title: str, base: str, nav: str, topbar: str, content: str) -> str:
    return f"""<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)}</title>
<link rel="stylesheet" href="{base}assets/style.css">
<script>window.SITE_BASE="{base}";</script>
<script src="{base}assets/search-index.js" defer></script>
</head>
<body>
<div class="layout">
  <aside class="side">
    <a class="brand" href="{base}index.html">Interview Prep<small>Katalon · NAB · Algorithms</small></a>
    <input id="q" class="search" type="search" placeholder="Tìm kiếm…  (phím /)" autocomplete="off">
    {nav}
  </aside>
  <div class="main">
    <div class="topbar">
      <div class="crumb">{topbar}</div>
      <button class="btn" onclick="toggleTheme()">Sáng / Tối</button>
    </div>
    {content}
  </div>
</div>
<script src="{base}assets/app.js" defer></script>
</body>
</html>
"""


def build_home(pages: list[Page]) -> str:
    total_words = sum(p.words for p in pages)
    def find(rel: str) -> Page | None:
        return next((p for p in pages if p.rel == rel), None)

    def li(rel: str, note: str = "", pill: str = "") -> str:
        p = find(rel)
        if not p:
            return ""
        tag = f'<span class="pill">{pill}</span>' if pill else ""
        sub = f" — <span style='color:var(--muted)'>{html.escape(note)}</span>" if note else ""
        return f'<li><a href="{p.out_rel}">{html.escape(p.title)}</a>{tag}{sub}</li>'

    nab = "".join([
        li("nab-prep/01-nab-research.md", "công ty, JD, 5 vòng, độ tin cậy từng nguồn"),
        li("nab-prep/02-gap-analysis.md", "JD đối chiếu hồ sơ thật", "⭐"),
        li("nab-prep/03-ke-hoach-on-tap.md", "5 tuần, theo từng vòng"),
        li("nab-prep/04-kafka-event-driven-saga.md", "gap kỹ thuật lớn nhất"),
        li("nab-prep/05-english-interview.md", "rủi ro số một", "⭐"),
    ])
    kat = "".join([
        li("katalon-prep/CV-BASED-ANSWERS.md", "thứ tự dự án, câu chuyện thật"),
        li("katalon-prep/AI-STACK-INTERVIEW-ANSWERS.md", "AI agent, vector, 61 câu luyện", "⭐"),
        li("katalon-prep/katalon-system-design/README.md", "7 họ bài, 5 trục nhận diện", "⭐"),
        li("katalon-prep/katalon-self-questions/README.md", "câu hỏi tự ghi, có code chạy"),
        li("katalon-prep/README.md", "workspace code: 313 test"),
    ])
    design = "".join([
        li("katalon-prep/katalon-system-design/04-event-counting-10k.md", "bài đã hỏi thật ở Principal", "⭐"),
        li("katalon-prep/katalon-system-design/06-race-condition-balance-ledger.md", "lỗi thật + thiết kế lại + AWS", "⭐"),
        li("katalon-prep/katalon-system-design/01-truetest-journey-mining.md"),
        li("katalon-prep/katalon-system-design/02-distributed-test-execution.md"),
        li("katalon-prep/katalon-system-design/03-realtime-analytics-dashboard.md"),
        li("katalon-prep/katalon-system-design/05-he-thong-that-allinone-aws.md", "kho bằng chứng AWS"),
    ])
    hero = f"""
<div class="hero">
  <h1>Interview Prep</h1>
  <p class="lead">Toàn bộ tài liệu ôn phỏng vấn ở một chỗ — hai hướng song song
  (<b>NAB</b> và <b>Katalon</b>), sáu bài system design, và bộ LeetCode.
  Gõ <code>/</code> để tìm kiếm toàn văn.</p>
  <div class="stats">
    <div class="stat"><b>{len(pages)}</b><span>tài liệu</span></div>
    <div class="stat"><b>{total_words // 1000}k</b><span>từ</span></div>
    <div class="stat"><b>2</b><span>hướng ứng tuyển</span></div>
    <div class="stat"><b>7</b><span>họ bài design</span></div>
  </div>
</div>
<div class="tracks">
  <div class="track"><h3>NAB Innovation Centre</h3>
    <p class="sub">Senior / Lead Java Engineer · tra cứu 06/09/2026</p><ol>{nab}</ol></div>
  <div class="track"><h3>Katalon</h3>
    <p class="sub">Senior / Lead · AI + test automation</p><ol>{kat}</ol></div>
  <div class="track"><h3>System Design — dùng chung</h3>
    <p class="sub">~70% nội dung dùng được cho cả hai hướng</p><ol>{design}</ol></div>
</div>
"""
    return hero


def code_page(rel: str, pages: list[Page]) -> str:
    """A read-only view of a source file, so links from the docs stay inside the site."""
    src = ROOT / rel
    try:
        text = src.read_text(encoding="utf-8", errors="replace")
    except Exception as exc:                                    # binary, permissions, ...
        text = f"(không đọc được: {exc})"
    if len(text) > 400_000:
        text = text[:400_000] + "\n\n… (đã cắt bớt)"
    # "a/b/c.java" -> "a/b/c.java.html" nằm sâu 2 thư mục, nên KHÔNG cộng thêm 1.
    # (dir_page thì có cộng, vì "a/b" -> "a/b/index.html" sâu hơn một cấp.)
    base = "../" * rel.count("/")
    lines = text.count("\n") + 1
    body = (f'<div class="wrap"><article><h1>{html.escape(src.name)}</h1>'
            f'<p style="color:var(--muted);font-size:13px">'
            f'<code>{html.escape(rel)}</code> · {lines} dòng · chỉ đọc</p>'
            f'<pre><code>{html.escape(text)}</code></pre></article><div></div></div>')
    return shell(title=src.name + " · Interview Prep", base=base,
                 nav=nav_html(pages, None, base),
                 topbar=" / ".join(html.escape(x) for x in rel.split("/")),
                 content=body)


def dir_page(rel: str, pages: list[Page]) -> str:
    """A simple listing for a linked directory that has no README."""
    d = ROOT / rel
    depth = rel.count("/") + 1
    base = "../" * depth
    rows = []
    for child in sorted(d.iterdir(), key=lambda c: (c.is_file(), c.name.lower())):
        if child.name.startswith(".") or child.name in SKIP_DIRS:
            continue
        name = child.name + ("/" if child.is_dir() else "")
        href = child.name + ("/index.html" if child.is_dir() else ".html")
        size = f"{child.stat().st_size:,} B" if child.is_file() else "thư mục"
        rows.append(f'<tr><td><a href="{html.escape(href)}">{html.escape(name)}</a></td>'
                    f'<td style="color:var(--muted)">{size}</td></tr>')
    body = (f'<div class="wrap"><article><h1>{html.escape(d.name)}/</h1>'
            f'<p style="color:var(--muted);font-size:13px"><code>{html.escape(rel)}</code></p>'
            f'<div class="tablewrap"><table><thead><tr><th>Tên</th><th>Kích thước</th></tr></thead>'
            f'<tbody>{"".join(rows)}</tbody></table></div></article><div></div></div>')
    return shell(title=d.name + "/ · Interview Prep", base=base,
                 nav=nav_html(pages, None, base),
                 topbar=" / ".join(html.escape(x) for x in rel.split("/")),
                 content=body)


def main() -> None:
    pages = collect()
    known_md = {p.rel for p in pages}
    code_files, dir_pages = scan_targets(pages, known_md)
    # A listing page links to its children, so those need pages too — and those
    # children may be directories with their own children. Expand to a fixed point.
    pending = list(dir_pages)
    while pending:
        d = pending.pop()
        for child in (ROOT / d).iterdir():
            if child.name.startswith(".") or child.name in SKIP_DIRS:
                continue
            rel = (Path(d) / child.name).as_posix()
            if child.is_file():
                if rel not in known_md:
                    code_files.add(rel)
            elif rel + "/README.md" not in known_md and rel not in dir_pages:
                dir_pages.add(rel)
                pending.append(rel)
    render(pages, code_files, dir_pages)

    if OUT.exists():
        shutil.rmtree(OUT)
    (OUT / "assets").mkdir(parents=True)
    (OUT / "assets" / "style.css").write_text(CSS, encoding="utf-8")
    (OUT / "assets" / "app.js").write_text(JS, encoding="utf-8")

    index = [{"u": p.out_rel, "t": p.title, "s": p.section, "x": p.text} for p in pages]
    (OUT / "assets" / "search-index.js").write_text(
        "window.SEARCH_INDEX=" + json.dumps(index, ensure_ascii=False) + ";",
        encoding="utf-8")

    for p in pages:
        depth = p.out_rel.count("/")
        base = "../" * depth
        toc_links = "".join(
            f'<a class="lv{t["level"]}" href="#{t["id"]}">{html.escape(t["name"][:70])}</a>'
            for t in p.toc)
        toc = f'<nav class="toc"><h5>Trong trang</h5>{toc_links}</nav>' if toc_links else "<div></div>"
        crumb = " / ".join(html.escape(x) for x in p.rel.split("/"))
        content = f'<div class="wrap"><article>{p.body}</article>{toc}</div>'
        out_file = OUT / p.out_rel
        out_file.parent.mkdir(parents=True, exist_ok=True)
        out_file.write_text(shell(
            title=p.title + " · Interview Prep", base=base,
            nav=nav_html(pages, p.rel, base), topbar=crumb, content=content,
        ), encoding="utf-8")

    for rel in sorted(code_files):
        f = OUT / (rel + ".html")
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(code_page(rel, pages), encoding="utf-8")

    for rel in sorted(dir_pages):
        f = OUT / rel / "index.html"
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text(dir_page(rel, pages), encoding="utf-8")

    (OUT / "index.html").write_text(shell(
        title="Interview Prep — Katalon · NAB", base="",
        nav=nav_html(pages, None, ""),
        topbar="Trang chủ", content=build_home(pages),
    ), encoding="utf-8")

    print(f"✓ {len(pages)} trang tài liệu + {len(code_files)} file code "
          f"+ {len(dir_pages)} thư mục → {OUT}")
    print(f"  mở: open {OUT / 'index.html'}")


if __name__ == "__main__":
    main()
