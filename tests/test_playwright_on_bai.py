#!/usr/bin/env python3
"""Playwright test of the Ôn bài tab: the lesson list and the markdown popup.

Everything it expects comes from web/build.py (ONBAI_PHASES: which on-bai/<phase>/ folders are shown, in order), from
each phase README's lesson table and from the lesson files themselves, so adding a lesson or a phase does not need an
edit here. It checks:
1. the tab is labelled "Ôn bài", lessons are the default view, the cards match the README tables in order, and every
   lesson file on disk is in a table;
2. every lesson opens in the popup and renders like the Tài liệu tab: title, every code block, every table,
   markdown inside <details> (no raw ** left), and every mermaid block as an SVG without a syntax error;
3. a link to another lesson opens inside the popup, ← goes back, an in-page anchor scrolls the popup;
4. "Đã ôn" in the popup ticks the card and both counters, and survives a reload;
5. Esc closes the popup; "Mở toàn trang" opens the same file in the Tài liệu tab;
6. the lecture videos are still reachable from the tab.

Mermaid comes from a CDN. Without network, point MERMAID_JS at a local mermaid.min.js (v10) and the page loads that.
Run: python3 tests/test_playwright_on_bai.py   (screenshots go to $SHOTS, default: a temp folder)
"""
from __future__ import annotations

import os
import re
import tempfile
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent.parent
ROW = re.compile(r"^\|\s*(\d{2})\s*\|\s*\[([^\]]+)\]\(([^)]+\.md)\)", re.M)


def phases() -> list[str]:
    """Các giai đoạn ôn bài theo đúng thứ tự web/build.py hiển thị (ONBAI_PHASES)."""
    return re.findall(r'\{"id": "([^"]+)"', (ROOT / "web" / "build.py").read_text(encoding="utf-8")
                      .split("ONBAI_PHASES = [", 1)[1].split("\n]", 1)[0])


def lessons() -> list[tuple[str, str, str]]:
    """(số, tên, khoá tài liệu) theo đúng thứ tự bảng trong README của từng giai đoạn."""
    return [(m.group(1), m.group(2), f"on-bai/{ph}/{m.group(3)}")
            for ph in phases()
            for m in ROW.finditer((ROOT / "on-bai" / ph / "README.md").read_text(encoding="utf-8"))]


def expected_blocks(md: str) -> tuple[int, int, int]:
    """(code block thường, mermaid, bảng) đếm từ file nguồn."""
    code = mermaid = tables = 0
    fence = None
    for line in md.splitlines():
        m = re.match(r"^(```|~~~)(\w*)", line)
        if m and fence is None:
            fence = m.group(1)
            if m.group(2) == "mermaid":
                mermaid += 1
            else:
                code += 1
        elif fence and line.startswith(fence):
            fence = None
        elif fence is None and re.match(r"^\|(\s*:?-+:?\s*\|)+\s*$", line):   # dòng phân cách: |---|:-:|
            tables += 1
    return code, mermaid, tables


def test_on_bai_tab_and_popup():
    rows = lessons()
    on_disk = sum(len(list((ROOT / "on-bai" / ph).glob("[0-9][0-9]-*.md"))) for ph in phases())
    print("[TEST] phases:", phases(), "lessons:", len(rows))
    assert len(rows) == on_disk, f"README tables list {len(rows)} lessons, folders have {on_disk} lesson files"
    shots = Path(os.environ.get("SHOTS") or tempfile.mkdtemp(prefix="onbai-shots-"))
    shots.mkdir(parents=True, exist_ok=True)
    html_file = (ROOT / "web" / "index.html").resolve()
    local_mermaid = os.environ.get("MERMAID_JS")

    with sync_playwright() as pw:
        exe = os.environ.get("CHROMIUM") or None
        browser = pw.chromium.launch(headless=True, executable_path=exe,
                                     args=["--allow-file-access-from-files", "--no-sandbox"])
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        errors: list[str] = []
        page.on("pageerror", lambda e: errors.append(str(e)))
        page.route("**/fonts.googleapis.com/**", lambda r: r.abort())
        page.route("**/fonts.gstatic.com/**", lambda r: r.abort())
        if local_mermaid:
            body = Path(local_mermaid).read_bytes()
            page.route("**/mermaid.min.js", lambda r: r.fulfill(body=body, content_type="application/javascript"))
        page.goto(f"file://{html_file}", wait_until="load")
        has_mermaid = page.evaluate("() => !!window.mermaid")
        print("[TEST] mermaid loaded:", has_mermaid)

        # 1. tab, mặc định là danh sách bài, thẻ khớp bảng README
        tab = page.locator('button.tab[data-t="onbai"]')
        assert tab.inner_text().startswith("Ôn bài"), tab.inner_text()
        assert page.locator('button.tab[data-t="vid"]').count() == 0, "tab Video cũ vẫn còn"
        tab.click()
        page.wait_for_selector("#obLessons", state="visible")
        assert page.locator("#obVideos").is_hidden()
        assert page.locator("#nOnbai").inner_text() == f"0/{len(rows)}"
        cards = page.locator("#obLessons .obcard")
        assert cards.count() == len(rows), cards.count()
        for i, (n, title, doc) in enumerate(rows):
            assert cards.nth(i).locator(".obn").inner_text() == n
            assert cards.nth(i).locator(".obtx > b").inner_text().startswith(title)
            assert cards.nth(i).locator(".obmain").get_attribute("data-doc") == doc
        page.screenshot(path=str(shots / "onbai-list.png"), full_page=True)

        # 2. từng bài mở trong popup, hiển thị đủ
        dialog = page.locator("#mdPop")
        for i, (n, title, doc) in enumerate(rows):
            cards.nth(i).locator(".obmain").click()
            page.wait_for_selector("#mdPop[open]")
            assert page.locator("#mdPath").inner_text() == doc
            assert page.locator("#mdTitle").inner_text() == page.evaluate("d => DOCS[d].t", doc)
            code, mermaid, tables = expected_blocks((ROOT / doc).read_text(encoding="utf-8"))
            if has_mermaid and mermaid:
                page.wait_for_function(
                    "n => document.querySelectorAll('#mdDoc .mermaid svg').length >= n", arg=mermaid, timeout=20000)
            got = page.evaluate("""() => {
                const box = document.querySelector('#mdDoc');
                const plain = box.cloneNode(true);
                plain.querySelectorAll('pre, code, .mermaid').forEach(x => x.remove());
                return {
                  pre: box.querySelectorAll('pre').length,
                  mermaidDiv: box.querySelectorAll('.mermaid').length,
                  svg: box.querySelectorAll('.mermaid svg').length,
                  mermaidErr: [...box.querySelectorAll('.mermaid')].filter(d =>
                      /Syntax error|Parse error/i.test(d.textContent || '') ||
                      /error/i.test((d.querySelector('svg') || {getAttribute: () => ''}).getAttribute('aria-roledescription') || '')).length,
                  tables: box.querySelectorAll('.tw > table').length,
                  rawBold: (plain.textContent.match(/\\*\\*/g) || []).length,
                  details: box.querySelectorAll('details').length,
                };
            }""")
            want_pre = code if has_mermaid else code + mermaid
            ok = (got["pre"] == want_pre and got["tables"] == tables and got["rawBold"] == 0
                  and (not has_mermaid or (got["mermaidDiv"] == mermaid and got["svg"] == mermaid and got["mermaidErr"] == 0)))
            print(f"{'OK ' if ok else 'BAD'} {n} {title[:44]:44} code {got['pre']}/{want_pre} · mermaid {got['svg']}/{mermaid}"
                  f" · lỗi mermaid {got['mermaidErr']} · bảng {got['tables']}/{tables} · details {got['details']} · ** thô {got['rawBold']}")
            assert ok, (doc, got, (code, mermaid, tables))
            page.screenshot(path=str(shots / f"onbai-{n}.png"))
            page.keyboard.press("Escape")
            page.wait_for_function("() => !document.querySelector('#mdPop').open")
        assert not dialog.is_visible()

        # 3. liên kết sang bài khác mở TRONG popup, ← quay lại, anchor cuộn popup
        ledger = "on-bai/gd1-nen-tang/05-ledger-va-balance.md"
        pg04 = "on-bai/gd1-nen-tang/04-postgres-concurrency.md"
        page.locator(f'#obLessons .obmain[data-doc="{ledger}"]').click()
        page.wait_for_selector("#mdPop[open]")
        assert page.locator("#pBack").is_hidden()
        page.locator(f'#mdDoc a[data-doc="{pg04}"]').first.click()
        page.wait_for_function("d => document.querySelector('#mdPath').textContent === d", arg=pg04)
        assert page.locator("#p-docs").is_hidden(), "liên kết trong popup không được nhảy sang tab Tài liệu"
        assert page.locator("#pBack").is_visible()
        page.locator("#pBack").click()
        page.wait_for_function("d => document.querySelector('#mdPath').textContent === d", arg=ledger)
        assert page.locator("#pBack").is_hidden()
        anchor = page.locator('#mdDoc a[data-act="anchor"]:not(.headerlink)').first   # link trong thân bài, không phải ¶
        target = anchor.get_attribute("data-anchor")
        anchor.click()
        page.wait_for_timeout(300)
        pos = page.evaluate("""a => {
            const sc = document.querySelector('#mdScroll');
            const t = document.querySelector('#mdDoc').querySelector('#' + CSS.escape(a));
            return {scroll: sc.scrollTop, top: t.getBoundingClientRect().top - sc.getBoundingClientRect().top};
        }""", target)
        print("[TEST] anchor", target, pos)
        assert pos["scroll"] > 1000 and -5 <= pos["top"] <= 60, pos   # cuộn xa xuống, đích nằm sát đầu khung popup

        # 4. "Đã ôn" trong popup → thẻ, hai bộ đếm, và còn sau khi tải lại
        page.locator("#mdSeen").check()
        page.keyboard.press("Escape")
        page.wait_for_function("() => !document.querySelector('#mdPop').open")
        assert page.locator(f'#obLessons .obcard:has(.obmain[data-doc="{ledger}"])').get_attribute("class").endswith("done")
        assert page.locator("#nOnbai").inner_text() == f"1/{len(rows)}"
        assert page.locator("#sOb").inner_text() == f"1/{len(rows)}"
        page.reload(wait_until="load")
        page.locator('button.tab[data-t="onbai"]').click()
        assert page.locator(f'#obLessons input[data-act="obseen"][data-doc="{ledger}"]').is_checked()

        # 5. "Mở toàn trang" → tab Tài liệu, đúng file; popup đóng
        page.locator(f'#obLessons .obmain[data-doc="{ledger}"]').click()
        page.wait_for_selector("#mdPop[open]")
        page.locator('#mdPop button[data-act="pfull"]').click()
        page.wait_for_selector("#p-docs", state="visible")
        assert page.locator("#docPath").inner_text() == ledger
        assert not page.evaluate("() => document.querySelector('#mdPop').open")

        # 6. video bài giảng vẫn ở trong tab
        page.locator('button.tab[data-t="onbai"]').click()
        page.locator('button[data-act="obview"][data-v="vid"]').click()
        page.wait_for_selector("#obVideos", state="visible")
        series = page.locator('#vSeries button[data-act="vser"]').count()
        print("[TEST] video series in the tab:", series)
        assert series >= 1 and page.locator("#vList button.vitem").count() > 0
        assert page.locator("#obLessons").is_hidden()

        assert not errors, errors
        browser.close()
    print(f"PASS: {len(rows)} bài, popup render đủ code/bảng/mermaid, điều hướng trong popup, tiến độ lưu được")


if __name__ == "__main__":
    test_on_bai_tab_and_popup()
