#!/usr/bin/env python3
"""Playwright test of the video view of the Ôn bài tab, for the Katalon interlude series (K00–K13, narrated by Emma).

Everything it expects comes from java-system-design/video-katalon/lessons/lessons.json, so a rebuild that changes a
length or a chapter does not need an edit here. It checks:
1. the series button, the 14/14 counter and the four part headers (A–D) in order;
2. for every video: code, title, length, chapter and key-point counts, poster and mp4 path match the manifest;
3. the narrator is Emma only, and none of the removed scripts (old K00–K13) is still listed;
4. every source link of the series (key points and covers) points at a document heading that exists.

Run: python3 tests/test_playwright_katalon_videos.py   (screenshots go to $SHOTS, default: a temp folder)
"""
from __future__ import annotations

import json
import os
import tempfile
from pathlib import Path

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent.parent
MANIFEST = ROOT / "java-system-design" / "video-katalon" / "lessons" / "lessons.json"
PARTS = ["PHẦN A", "PHẦN B", "PHẦN C", "PHẦN D"]
REMOVED = ["k00-interlude-orientation", "k03-10m-pipeline", "k08-kafka-ingest-setup", "k13-backpressure-and-troubleshooting"]


def mmss(t: float) -> str:
    t = int(t + 0.5)
    return f"{t // 60}:{t % 60:02d}"


def test_video_portal():
    lessons = json.loads(MANIFEST.read_text(encoding="utf-8"))["lessons"]
    assert len(lessons) == 14, f"manifest has {len(lessons)} lessons"
    shots = Path(os.environ.get("SHOTS") or tempfile.mkdtemp(prefix="katalon-shots-"))
    shots.mkdir(parents=True, exist_ok=True)
    html_file = (ROOT / "web" / "index.html").resolve()

    with sync_playwright() as pw:
        # CHROMIUM=/path/to/chrome when the installed Playwright expects a browser build that isn't downloaded
        exe = os.environ.get("CHROMIUM") or None
        browser = pw.chromium.launch(headless=True, executable_path=exe,
                                     args=["--allow-file-access-from-files", "--no-sandbox"])
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        errors: list[str] = []
        page.on("pageerror", lambda e: errors.append(str(e)))
        page.goto(f"file://{html_file}", wait_until="load")
        page.locator('button.tab[data-t="onbai"]').click()           # tab "Ôn bài": bài md mặc định, video là chế độ thứ hai
        page.wait_for_selector("#p-onbai", state="visible")
        page.locator('button[data-act="obview"][data-v="vid"]').click()
        page.wait_for_selector("#obVideos", state="visible")
        page.locator('button[data-act="vser"][data-s="video-katalon"]').click()
        page.wait_for_timeout(300)

        title = page.locator("#vTitle").inner_text()
        print("[TEST] series title:", title)
        assert "Chen ngang Katalon" in title and "14/14" in title, title

        headers = [h.upper() for h in page.locator("#vList .vweek").all_inner_texts()]
        print("[TEST] part headers:", headers)
        assert len(headers) == 4 and all(h.startswith(p) for h, p in zip(headers, PARTS)), headers

        items = page.locator("#vList button.vitem")
        assert items.count() == 14, items.count()
        listed = [items.nth(i).get_attribute("data-v") for i in range(14)]
        assert listed == [x["id"] for x in lessons], listed
        assert not set(REMOVED) & set(listed), "a removed video is still listed"

        refs = 0
        for i, x in enumerate(lessons):
            items.nth(i).click()
            page.wait_for_timeout(250)
            heading = page.locator("#vInfo h3").inner_text()
            sub = page.locator("#vInfo .sub").inner_text()
            chapters = page.locator('#vInfo .vchaps button[data-act="seek"]').count()
            points = page.locator("#vInfo .vpt").count()
            # a browser without H.264 (Playwright's own Chromium) swaps the <video> for a download link: accept either
            if page.locator("#vEl").count():
                src = page.get_attribute("#vEl", "src") or ""
            else:
                src = page.get_attribute("#vPlayer .miss a[download]", "href") or ""
            poster = items.nth(i).locator("img").get_attribute("src") or ""
            ok = (heading.startswith(f"{x['code']} · ") and x["title"] in heading and mmss(x["duration"]) in sub
                  and chapters == len(x["chapters"]) and points == len(x["points"])
                  and src.endswith(f"video-katalon/lessons/{x['file']}") and poster.endswith(x["poster"]))
            print(f"{'OK ' if ok else 'BAD'} {heading} · {mmss(x['duration'])} · {chapters} chapters · {points} points")
            assert ok, (heading, sub, chapters, points, src, poster)
            for doc, anchor in page.evaluate("""() => [...document.querySelectorAll(
                    '#vInfo .vpt .src, #vInfo .vchaps button[data-act="doc"]')].map(b => [b.dataset.doc, b.dataset.anchor || ''])"""):
                found = page.evaluate("([d, a]) => !!(DOCS[d] && (!a || DOCS[d].h.includes(`id=\"${a}\"`)))", [doc, anchor])
                assert found, f"{x['code']}: source {doc}#{anchor} does not resolve"
                refs += 1
            page.screenshot(path=str(shots / f"{x['code']}.png"))

        voice = page.locator("#vEyebrow").inner_text()
        print("[TEST] eyebrow:", voice, "· source links resolved:", refs, "· screenshots:", shots)
        assert "EMMA" in voice.upper() and "TOM" not in voice.upper(), voice
        assert not errors, errors
        browser.close()
    print("PASS: 14/14 videos, 4 parts, Emma only,", refs, "source links resolve")


if __name__ == "__main__":
    test_video_portal()
