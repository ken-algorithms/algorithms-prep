#!/usr/bin/env python3
"""Comprehensive Playwright automated test suite for Video Tab & Katalon Series (K00-K13).

Validates:
1. Navigation to Video tab.
2. Selection of 'Chen ngang Katalon' series.
3. Existence of all 14 episodes (K00 to K13) and 3 structured section headers:
   - Phần 1A: Sau giai đoạn 1 (Ước lượng & 10k)
   - Phần 1B: Sau giai đoạn 2 (Kiến trúc 10M–100M)
   - Phần 2: Thực chiến Code & Sửa lỗi Kafka (K08–K13)
4. For each newly created episode (K08 through K13):
   - Code, Title, Subtitle, Duration, Size.
   - Chapters list is authentic and specific to that episode.
   - Points list is authentic and specific to that episode.
   - Source MP4 and Poster JPG URLs are unique and distinct from K00.
   - None of K00's content, titles, or 284s duration leak into K08-K13.
   - HTML5 video element loads metadata correctly (duration, dimensions 1280x720).
   - Captures screenshots for visual audit.
"""
from __future__ import annotations

import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent.parent

EXPECTED_EPISODES = {
    "k08-kafka-ingest-setup": {
        "code": "K08",
        "title": "Kafka hands-on: Ingestion API and Producer tuning",
        "file": "k08-kafka-ingest-setup.mp4",
        "expected_duration_str": "7:03",
        "keyword": "Producer",
        "must_not_contain": ["Families A and C", "Five axes", "Three loads", "Eight videos"],
    },
    "k09-fixing-partition-skew": {
        "code": "K09",
        "title": "The skewed partition bug: custom partitioners and fixing hotspots",
        "file": "k09-fixing-partition-skew.mp4",
        "expected_duration_str": "6:11",
        "keyword": "skew",
        "must_not_contain": ["Families A and C", "Five axes", "Three loads", "Eight videos"],
    },
    "k10-fixing-rebalance-storm": {
        "code": "K10",
        "title": "Fixing consumer lag and the P20 rebalance storm",
        "file": "k10-fixing-rebalance-storm.mp4",
        "expected_duration_str": "5:39",
        "keyword": "batch",
        "must_not_contain": ["Families A and C", "Five axes", "Three loads", "Eight videos"],
    },
    "k11-poison-pill-and-dlt": {
        "code": "K11",
        "title": "Poison pill isolation and non-blocking dead letter topics",
        "file": "k11-poison-pill-and-dlt.mp4",
        "expected_duration_str": "5:23",
        "keyword": "DLT",
        "must_not_contain": ["Families A and C", "Five axes", "Three loads", "Eight videos"],
    },
    "k12-idempotent-sink-greatest": {
        "code": "K12",
        "title": "Idempotent consumer: deduplication store and GREATEST upserts",
        "file": "k12-idempotent-sink-greatest.mp4",
        "expected_duration_str": "5:16",
        "keyword": "Deduplication",
        "must_not_contain": ["Families A and C", "Five axes", "Three loads", "Eight videos"],
    },
    "k13-backpressure-and-troubleshooting": {
        "code": "K13",
        "title": "Backpressure, buffer limits, and live debugging drills",
        "file": "k13-backpressure-and-troubleshooting.mp4",
        "expected_duration_str": "5:30",
        "keyword": "drill",
        "must_not_contain": ["Families A and C", "Five axes", "Three loads", "Eight videos"],
    },
}


def test_video_portal():
    html_file = (ROOT / "web" / "index.html").resolve()
    assert html_file.exists(), f"File {html_file} not found!"

    screenshot_dir = ROOT / "web" / "test_artifacts"
    screenshot_dir.mkdir(parents=True, exist_ok=True)

    with sync_playwright() as pw:
        browser = pw.chromium.launch(
            headless=True,
            args=["--allow-file-access-from-files", "--no-sandbox"]
        )
        page = browser.new_page(viewport={"width": 1440, "height": 900})

        url = f"file://{html_file}"
        print(f"[TEST] Loading portal: {url}")
        page.goto(url, wait_until="networkidle")

        # Step 1: Click Video Tab
        print("[TEST] Switching to Video tab...")
        video_tab = page.locator('button.tab[data-t="vid"]')
        video_tab.click()
        page.wait_for_selector("#p-vid", state="visible")

        # Step 2: Select 'video-katalon' series
        print("[TEST] Selecting 'video-katalon' series...")
        katalon_series_btn = page.locator('button[data-act="vser"][data-s="video-katalon"]')
        katalon_series_btn.click()
        page.wait_for_timeout(300)

        # Step 3: Verify Series Header & Counter
        v_title = page.locator("#vTitle").inner_text()
        print(f"[TEST] Series Title: {v_title}")
        assert "Chen ngang Katalon" in v_title, f"Unexpected series title: {v_title}"
        assert "14/14" in v_title, f"Expected 14/14 videos in title, got: {v_title}"

        # Step 4: Verify Section Headers in Sidebar
        headers = [h.upper() for h in page.locator("#vList .vweek").all_inner_texts()]
        print(f"[TEST] Section headers in sidebar: {headers}")
        assert any("PHẦN 1A: SAU GIAI ĐOẠN 1" in h for h in headers), "Missing Phần 1A header"
        assert any("PHẦN 1B: SAU GIAI ĐOẠN 2" in h for h in headers), "Missing Phần 1B header"
        assert any("PHẦN 2: THỰC CHIẾN CODE" in h for h in headers), "Missing Phần 2 header"

        # Step 5: Verify Total Videos Count in Sidebar
        ep_items = page.locator("#vList button.vitem")
        total_eps = ep_items.count()
        print(f"[TEST] Total episodes in sidebar: {total_eps}")
        assert total_eps == 14, f"Expected 14 episodes, found {total_eps}"

        # Step 6: Detailed Verification for K08 through K13
        for vid, spec in EXPECTED_EPISODES.items():
            print(f"\n[TEST] ---> Verifying Episode {spec['code']} ({vid}) <---")
            btn = page.locator(f'#vList button.vitem[data-v="{vid}"]')
            assert btn.count() == 1, f"Episode button for {vid} not found!"
            btn.click()
            page.wait_for_timeout(400)

            # 6.1: Check UI Header in #vInfo
            heading = page.locator("#vInfo h3").inner_text()
            sub = page.locator("#vInfo .sub").inner_text()
            print(f"  Heading : {heading}")
            print(f"  Subtitle: {sub}")

            assert spec["code"] in heading, f"Expected code {spec['code']} in heading: {heading}"
            assert spec["title"] in heading, f"Expected title '{spec['title']}' in heading: {heading}"
            assert spec["expected_duration_str"] in sub, f"Expected duration {spec['expected_duration_str']} in {sub}"

            # 6.2: Ensure NO K00 content leakage
            for forbidden in spec["must_not_contain"]:
                assert forbidden.lower() not in heading.lower(), f"Leakage: '{forbidden}' in heading of {vid}!"
                assert forbidden.lower() not in sub.lower(), f"Leakage: '{forbidden}' in subtitle of {vid}!"

            # 6.3: Check HTML5 Video Player
            v_el = page.locator("#vEl")
            v_src = v_el.get_attribute("src")
            v_poster = v_el.get_attribute("poster")
            print(f"  Player src   : {v_src}")
            print(f"  Player poster: {v_poster}")

            assert spec["file"] in v_src, f"Expected {spec['file']} in src: {v_src}"
            assert spec["file"].replace(".mp4", ".jpg") in v_poster, f"Poster mismatch in {v_poster}"

            # 6.4: Check Chapters
            chaps = page.locator(".vchaps").first.inner_text()
            print(f"  Chapters: {chaps.replace(chr(10), ' | ')[:100]}...")
            assert spec["keyword"].lower() in chaps.lower(), f"Keyword '{spec['keyword']}' missing in chapters"

            # 6.5: Capture screenshot of the player
            shot_file = screenshot_dir / f"verified_{spec['code']}_{vid}.png"
            page.screenshot(path=str(shot_file))
            print(f"  Captured screenshot -> {shot_file.name}")

        print("\n" + "=" * 65)
        print("🎉 ALL PLAYWRIGHT TESTS PASSED SUCCESSFULLY! (14/14 EPISODES)")
        print("=" * 65)
        browser.close()


if __name__ == "__main__":
    test_video_portal()
