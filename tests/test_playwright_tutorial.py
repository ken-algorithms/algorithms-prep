#!/usr/bin/env python3
"""Playwright test of the Tutorial tab: lesson cards, the markdown popup, Copy buttons, progress.

Everything it expects comes from web/build.py (TUT_TRACKS: which tutorial/<id>/ folders are shown), from each track
README's lesson tables and from the lesson files themselves, so adding a lesson needs no edit here. It checks:
1. the tab is labelled "Tutorial", the cards match the README tables in order (number, title, file name, part
   headings), and the counters start at 0;
2. every lesson opens in the popup and renders fully: title, every code block wrapped with a Copy button, every
   📄/✂️ file line moved into its block's header, every table, mermaid as SVG without a syntax error, no raw **;
   on a 390 px phone neither the tab nor any lesson popup scrolls sideways;
3. Copy puts exactly the lab file's text on the clipboard (Clipboard API), the execCommand fallback copies the same
   text when the Clipboard API is missing, and the button confirms with "Đã chép ✓";
4. a link to another lesson opens inside the popup and ← goes back; "Bắt đầu" opens lesson 01;
5. "Đã học" in the popup ticks the card and both counters, survives a reload, and the button moves on to lesson 02;
6. the Tài liệu tab lists the files under a "Tutorial" group.

Fonts, mermaid and highlight.js come from CDNs. Without network, point MERMAID_JS at a local mermaid.min.js (v10)
and HLJS_DIR at a folder holding highlight.min.js and languages/{dockerfile,http}.min.js
(npm package @highlightjs/cdn-assets@11.9.0); the page then loads those.
Run: python3 tests/test_playwright_tutorial.py   (screenshots go to $SHOTS, default: a temp folder)
"""
from __future__ import annotations

import os
import re
import tempfile
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent.parent
ROW = re.compile(r"^\|\s*(\d{2})\s*\|\s*\[([^\]]+)\]\(([^)]+\.md)\)")
MARK = re.compile(r"^(📄|✂️) \*\*`([^`]+)`\*\*", re.M)


def tracks() -> list[str]:
    """Các bộ tutorial theo đúng thứ tự web/build.py hiển thị (TUT_TRACKS)."""
    return re.findall(r'\{"id": "([^"]+)"', (ROOT / "web" / "build.py").read_text(encoding="utf-8")
                      .split("TUT_TRACKS = [", 1)[1].split("\n]", 1)[0])


def lessons() -> list[dict]:
    """Mỗi bài: số, tên, file, khoá tài liệu, tên phần, theo đúng thứ tự các bảng trong README của từng bộ."""
    out = []
    for tid in tracks():
        readme = (ROOT / "tutorial" / tid / "README.md").read_text(encoding="utf-8")
        section = readme.split("## Danh sách bài", 1)[1].split("\n## ", 1)[0]
        part = None
        for line in section.split("\n"):
            if line.startswith("### "):
                part = line[4:].strip()
            m = ROW.match(line)
            if m:
                out.append({"n": m.group(1), "t": m.group(2), "file": m.group(3), "part": part,
                            "doc": f"tutorial/{tid}/{m.group(3)}", "dir": ROOT / "tutorial" / tid})
    return out


def expected(md: str) -> dict:
    """Đếm từ file nguồn: khối code thường, mermaid, bảng (kể cả trong blockquote), dòng 📄/✂️ trên khối code."""
    code = mermaid = tables = 0
    fence = None
    for line in md.splitlines():
        line = re.sub(r"^(\s*>\s?)+", "", line)
        m = re.match(r"^\s*(```|~~~)\s*([\w+-]*)", line)
        if m and fence is None:
            fence = m.group(1)
            mermaid += m.group(2) == "mermaid"
            code += m.group(2) != "mermaid"
        elif fence and line.strip().startswith(fence):
            fence = None
        elif fence is None and re.match(r"^\s*\|(\s*:?-+:?\s*\|)+\s*$", line):   # kể cả bảng thụt lề trong danh sách
            tables += 1
    return {"code": code, "mermaid": mermaid, "tables": tables, "marks": len(MARK.findall(md))}


def norm(text: str) -> str:
    return text.lstrip("﻿").replace("\r\n", "\n").rstrip("\n")


def route_cdns(page) -> None:
    page.route("**/fonts.googleapis.com/**", lambda r: r.abort())
    page.route("**/fonts.gstatic.com/**", lambda r: r.abort())
    mer = os.environ.get("MERMAID_JS")
    if mer:
        body = Path(mer).read_bytes()
        page.route("**/mermaid.min.js", lambda r: r.fulfill(body=body, content_type="application/javascript"))
    hl = os.environ.get("HLJS_DIR")
    if hl:
        base = Path(hl)
        page.route("**/highlight.js/*/highlight.min.js", lambda r: r.fulfill(
            body=(base / "highlight.min.js").read_bytes(), content_type="application/javascript"))
        page.route("**/highlight.js/*/languages/*.min.js", lambda r: r.fulfill(
            body=(base / "languages" / r.request.url.rsplit("/", 1)[1]).read_bytes(),
            content_type="application/javascript"))


POPUP_STATE = """() => {
  const box = document.querySelector('#mdDoc'), sc = document.querySelector('#mdScroll');
  const plain = box.cloneNode(true);
  plain.querySelectorAll('pre, code, .mermaid, .cphead').forEach(x => x.remove());
  return {
    pre: box.querySelectorAll('pre').length,
    wrapped: box.querySelectorAll('.cpwrap > pre').length,
    copy: box.querySelectorAll('.cpwrap > .cphead .cpbtn[data-act="copy"]').length,
    files: box.querySelectorAll('.cphead .cplbl.file').length,
    leftMarks: [...box.querySelectorAll('p')].filter(p => /^(📄|✂️)/u.test(p.textContent.trim())
                 && p.nextElementSibling && p.nextElementSibling.classList.contains('cpwrap')).length,
    hl: box.querySelectorAll('code.hljs').length,
    mermaidDiv: box.querySelectorAll('.mermaid').length,
    svg: box.querySelectorAll('.mermaid svg').length,
    mermaidErr: [...box.querySelectorAll('.mermaid')].filter(d => /Syntax error|Parse error/i.test(d.textContent || '')).length,
    tables: box.querySelectorAll('.tw > table').length,
    rawBold: (plain.textContent.match(/\\*\\*/g) || []).length,
    sideways: sc.scrollWidth - sc.clientWidth,
  };
}"""


def test_tutorial_tab_popup_and_copy():
    rows = lessons()
    assert rows, "no tutorial lesson found"
    for tid in tracks():
        on_disk = len(list((ROOT / "tutorial" / tid).glob("[0-9][0-9]-*.md")))
        assert on_disk == sum(r["doc"].startswith(f"tutorial/{tid}/") for r in rows), (tid, on_disk)
    print("[TEST] tracks:", tracks(), "lessons:", len(rows))
    shots = Path(os.environ.get("SHOTS") or tempfile.mkdtemp(prefix="tutorial-shots-"))
    shots.mkdir(parents=True, exist_ok=True)
    html_file = (ROOT / "web" / "index.html").resolve()

    with sync_playwright() as pw:
        browser = pw.chromium.launch(headless=True, executable_path=os.environ.get("CHROMIUM") or None,
                                     args=["--allow-file-access-from-files", "--no-sandbox"])
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        # Clipboard API trong trang file:// cần quyền; ghi lại văn bản để so, đường execCommand thử riêng ở bước 3
        ctx.add_init_script("""Object.defineProperty(navigator, 'clipboard', {configurable: true,
            value: {writeText: t => { window.__clip = t; return Promise.resolve(); }}});""")
        page = ctx.new_page()
        errors: list[str] = []
        page.on("pageerror", lambda e: errors.append(str(e)))
        route_cdns(page)
        page.goto(f"file://{html_file}", wait_until="load")
        has_mermaid = page.evaluate("() => !!window.mermaid")
        has_hljs = page.evaluate("() => !!window.hljs && !!hljs.getLanguage('csharp') && !!hljs.getLanguage('dockerfile')")
        print("[TEST] mermaid loaded:", has_mermaid, "· highlight.js loaded:", has_hljs)

        # 1. tab, thẻ khớp bảng README, bộ đếm bắt đầu từ 0
        tab = page.locator('button.tab[data-t="tut"]')
        assert tab.inner_text().startswith("Tutorial"), tab.inner_text()
        tab.click()
        page.wait_for_selector("#p-tut", state="visible")
        assert page.locator("#nTut").inner_text() == f"0/{len(rows)}"
        cards = page.locator("#tutBody .tutcard")
        assert cards.count() == len(rows), cards.count()
        for i, r in enumerate(rows):
            card = cards.nth(i)
            assert card.locator(".obn").inner_text() == r["n"]
            assert card.locator(".obtx > b").inner_text() == r["t"]
            assert card.locator(".obmain").get_attribute("data-doc") == r["doc"]
            assert r["file"] in card.locator(".tutfile").inner_text()
        parts = [p for p in dict.fromkeys(r["part"] for r in rows) if p]
        assert page.locator("#tutBody h2.sec").all_inner_texts() == parts, page.locator("#tutBody h2.sec").all_inner_texts()
        page.screenshot(path=str(shots / "tutorial-list.png"), full_page=True)

        # 2. từng bài mở trong popup, hiển thị đủ, mỗi khối code có nút Copy
        for i, r in enumerate(rows):
            cards.nth(i).locator(".obmain").click()
            page.wait_for_selector("#mdPop[open]")
            assert page.locator("#mdPath").inner_text() == r["doc"]
            assert page.locator("#mdTitle").inner_text() == page.evaluate("d => DOCS[d].t", r["doc"])
            assert page.locator("#mdSeenTxt").inner_text() == "Đã học" and page.locator("#mdSeenWrap").is_visible()
            want = expected((ROOT / r["doc"]).read_text(encoding="utf-8"))
            if has_mermaid and want["mermaid"]:
                page.wait_for_function("n => document.querySelectorAll('#mdDoc .mermaid svg').length >= n",
                                       arg=want["mermaid"], timeout=20000)
            got = page.evaluate(POPUP_STATE)
            want_pre = want["code"] if has_mermaid else want["code"] + want["mermaid"]
            ok = (got["pre"] == want_pre == got["wrapped"] == got["copy"] and got["files"] == want["marks"]
                  and got["leftMarks"] == 0 and got["tables"] == want["tables"] and got["rawBold"] == 0
                  and (not has_mermaid or (got["svg"] == want["mermaid"] and got["mermaidErr"] == 0)))
            print(f"{'OK ' if ok else 'BAD'} {r['n']} {r['t'][:40]:40} code {got['pre']}/{want_pre} · Copy {got['copy']}"
                  f" · file {got['files']}/{want['marks']} · bảng {got['tables']}/{want['tables']}"
                  f" · mermaid {got['svg']}/{want['mermaid']} · tô màu {got['hl']} · ** thô {got['rawBold']}")
            assert ok, (r["doc"], got, want)
            page.keyboard.press("Escape")
            page.wait_for_function("() => !document.querySelector('#mdPop').open")

        # 3. Copy: đúng nguyên văn file trong lab, cả đường Clipboard API lẫn execCommand
        lesson11 = next(r for r in rows if r["file"].startswith("11-"))
        page.locator(f'#tutBody .obmain[data-doc="{lesson11["doc"]}"]').click()
        page.wait_for_selector("#mdPop[open]")
        head = page.locator('#mdDoc .cphead:has(.cplbl.file:text-matches("OrdersApiTests\\\\.cs"))').first
        label = head.locator(".cplbl").inner_text()
        rel = re.search(r"(lab/\S+)", label).group(1)
        want_text = norm((lesson11["dir"] / rel).read_text(encoding="utf-8"))
        head.scroll_into_view_if_needed()
        btn = head.locator(".cpbtn")
        btn.click()
        page.wait_for_function("() => typeof window.__clip === 'string'")
        assert page.evaluate("() => window.__clip") == want_text, "Clipboard API copied different text"
        assert btn.inner_text() == "Đã chép ✓"
        page.screenshot(path=str(shots / "tutorial-copy.png"))
        page.evaluate("""() => {
            Object.defineProperty(navigator, 'clipboard', {configurable: true, value: undefined});
            window.__fallback = null;
            document.addEventListener('copy', e => { window.__fallback = e.target.value ?? String(getSelection()); });
        }""")
        page.wait_for_timeout(1700)   # nút trở về "Copy"
        assert btn.inner_text() == "Copy"
        btn.click()
        page.wait_for_function("() => window.__fallback !== null")
        assert page.evaluate("() => window.__fallback") == want_text, "execCommand fallback copied different text"
        assert btn.inner_text() == "Đã chép ✓", btn.inner_text()
        if has_hljs:
            assert page.locator("#mdDoc code.language-csharp.hljs .hljs-keyword").count() > 0, "C# not highlighted"

        # 4. liên kết sang bài khác mở TRONG popup, ← quay lại
        nxt = rows[rows.index(lesson11) + 1]["doc"]
        page.locator(f'#mdDoc a[data-doc="{nxt}"]').first.click()
        page.wait_for_function("d => document.querySelector('#mdPath').textContent === d", arg=nxt)
        assert page.locator("#p-docs").is_hidden(), "a link inside the popup must not switch to the Tài liệu tab"
        page.locator("#pBack").click()
        page.wait_for_function("d => document.querySelector('#mdPath').textContent === d", arg=lesson11["doc"])
        page.keyboard.press("Escape")
        page.wait_for_function("() => !document.querySelector('#mdPop').open")
        start = page.locator('#tutBody .rm-actions button[data-act="pop"]').first
        assert start.inner_text().startswith("Bắt đầu: bài 01"), start.inner_text()
        start.click()
        page.wait_for_function("d => document.querySelector('#mdPath').textContent === d", arg=rows[0]["doc"])

        # 5. "Đã học" trong popup → thẻ, hai bộ đếm, còn sau khi tải lại, nút chuyển sang bài 02
        page.locator("#mdSeen").check()
        page.keyboard.press("Escape")
        page.wait_for_function("() => !document.querySelector('#mdPop').open")
        assert "done" in page.locator(f'#tutBody .tutcard:has(.obmain[data-doc="{rows[0]["doc"]}"])').get_attribute("class")
        assert page.locator("#nTut").inner_text() == f"1/{len(rows)}"
        assert page.locator("#sTut").inner_text() == f"1/{len(rows)}"
        page.reload(wait_until="load")
        page.locator('button.tab[data-t="tut"]').click()
        assert page.locator(f'#tutBody input[data-act="tutseen"][data-doc="{rows[0]["doc"]}"]').is_checked()
        assert page.locator('#tutBody .rm-actions button[data-act="pop"]').first.inner_text().startswith("Học tiếp: bài 02")

        # 6. tab Tài liệu có nhóm Tutorial
        page.locator('button.tab[data-t="docs"]').click()
        groups = page.locator("#dnav .dgroup").all_text_contents()   # inner_text bị CSS viết hoa
        assert "Tutorial" in groups, groups
        ctx.close()

        # điện thoại 390 px: danh sách và mọi popup bài không cuộn ngang
        phone = browser.new_context(viewport={"width": 390, "height": 844})
        page = phone.new_page()
        page.on("pageerror", lambda e: errors.append(str(e)))
        route_cdns(page)
        page.goto(f"file://{html_file}", wait_until="load")
        page.locator('button.tab[data-t="tut"]').click()
        assert page.evaluate("() => document.documentElement.scrollWidth") <= 390
        page.screenshot(path=str(shots / "tutorial-phone.png"), full_page=True)
        for r in rows:
            page.evaluate("d => openPop(d)", r["doc"])
            page.wait_for_timeout(300 if not has_mermaid else 900)
            side = page.evaluate(POPUP_STATE)["sideways"]
            assert side <= 0, f"{r['doc']}: popup scrolls sideways by {side}px on a phone"
            page.keyboard.press("Escape")
        print(f"[TEST] phone 390px: no sideways scroll in the tab or in {len(rows)} popups")
        phone.close()

        assert not errors, errors
        browser.close()
    print(f"PASS: {len(rows)} bài, popup đủ code/bảng/mermaid, nút Copy chép đúng file lab, tiến độ lưu được")


if __name__ == "__main__":
    test_tutorial_tab_popup_and_copy()
