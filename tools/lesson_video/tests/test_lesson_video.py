"""Test công cụ dựng video: chạy bằng bộ đọc im lặng, không cần model Kokoro.

    cd tools && uv run --with-requirements lesson_video/requirements.txt --with pytest pytest lesson_video/tests -q
"""
from __future__ import annotations

from pathlib import Path

import pytest

from lesson_video import coverage, script as sc, timeline, tts
from lesson_video.slides import H, Slides, W
from lesson_video.speak import speakable

FIX = Path(__file__).parent / "fixtures"
SAMPLE = FIX / "ep99-sample.yaml"
REPO = Path(__file__).resolve().parents[3]
LESSONS = REPO / "java-system-design" / "video-gd1" / "lessons"


def test_sample_loads_and_checks_clean():
    lesson = sc.load(SAMPLE)
    assert lesson.speakers["tom"].voice_for("kokoro") == "am_michael"
    assert sc.check(lesson, SAMPLE, sc.load_points(FIX / "points.yaml")) == []


def test_every_scene_kind_renders():
    lesson = sc.load(SAMPLE)
    kinds = {s.kind for s in lesson.scenes}
    assert kinds == set(sc.ITEM_KEYS), f"mẫu thiếu kiểu cảnh: {set(sc.ITEM_KEYS) - kinds}"
    tl = timeline.build(lesson, tts.Silent(), say=lambda s: None)
    slides = Slides(lesson)
    for fr in tl.frames:
        img = slides.render(fr, fr.start / tl.total)
        assert img.size == (W, H)


def test_timeline_has_chapters_and_scene_starts():
    lesson = sc.load(SAMPLE)
    tl = timeline.build(lesson, tts.Silent(), say=lambda s: None)
    assert [name for _, name in tl.chapters][:3] == ["Intro", "Acronyms", "Conversions"]
    assert len(tl.scene_starts) == len(lesson.scenes)
    assert tl.scene_starts == sorted(tl.scene_starts)
    # khoảng lặng đếm ngược của cảnh exercise thành các khung có số đếm
    assert any(fr.countdown == 3 for fr in tl.frames)


def test_point_expect_is_enforced(tmp_path):
    bad = SAMPLE.read_text(encoding="utf-8").replace("86,400", "eighty-six thousand")
    p = tmp_path / "ep98.yaml"
    p.write_text(bad.replace('sources: "."', f'sources: "{FIX}"'), encoding="utf-8")
    problems = sc.check(sc.load(p), p, sc.load_points(FIX / "points.yaml"))
    assert any("86,400" in m for m in problems)


def test_unknown_anchor_is_reported(tmp_path):
    bad = SAMPLE.read_text(encoding="utf-8").replace("src.md#1-quy-đổi", "src.md#khong-co")
    p = tmp_path / "ep97.yaml"
    p.write_text(bad.replace('sources: "."', f'sources: "{FIX}"'), encoding="utf-8")
    assert any("khong-co" in m for m in sc.check(sc.load(p), p))


def test_focus_out_of_range_is_rejected(tmp_path):
    p = tmp_path / "ep96.yaml"
    p.write_text("""id: ep96
ep: 96
title: t
scenes:
  - kind: bullets
    items: [{t: a}]
    lines:
      - tom: hello
        focus: 3
""", encoding="utf-8")
    with pytest.raises(SystemExit):
        sc.load(p)


@pytest.mark.parametrize("text, spoken", [
    ("1 day ≈ 10⁵ s", "1 day about 10 to the fifth seconds"),
    ("P01–P07 cost 10–100 ns", "P 1 to P 7 cost 10 to 100 nanoseconds"),
    ("99.9% of 1.5M users", "99 point 9% of 1 point 5 million users"),
    ("~0.5 ms, × 2.4", "about 0 point 5 milliseconds, times 2 point 4"),
    ("Version A and profile A", "Version eigh and profile eigh"),
    ("ReDoS, SaaS, PACELC, OIDC", "ree-doss, sass, pass-elk, O I D C"),
    ("a 600 B record, 0.9 GB/day", "a 600 bytes record, 0 point 9 gigabytes a day"),
    ("24 tests/s, 160 Mbit/s", "24 tests per second, 160 megabits per second"),
    ("Alex Xu — Ho Chi Minh City", "Alex Shoo, Ho Chee Minh City"),
    ("R + W > N, max.poll.records", "R plus W greater than N, max poll records"),
    ("ISR, eKYC, etcd", "I S R, e K Y C, et-see-dee"),
    ("lab 5B and lab 10B, 1B clicks", "lab 5-B and lab 10-B, 1 billion clicks"),
])
def test_speakable(text, spoken):
    assert speakable(text) == spoken


def test_glossary_check_flags_missing_card(tmp_path):
    lesson = sc.load(SAMPLE)
    terms = {"RPS", "CCU", "DAU", "RTT", "SPOF", "JSON"}
    problems = coverage.acronym_report(lesson, terms)
    # SLA không có trong bảng; DB có trong cảnh nhưng thẻ của video không có
    assert any("'SPOF'" in m for m in problems) is False
    assert any("không có trong bảng" in m for m in problems)


SERIES = sorted((REPO / "java-system-design").glob("video-gd*/lessons"))


@pytest.mark.skipif(not SERIES, reason="chưa có kịch bản bộ video")
@pytest.mark.parametrize("lessons_dir", SERIES, ids=lambda d: d.parent.name)
def test_real_scripts_pass_check(lessons_dir):
    """Mọi kịch bản của mọi bộ: anchor, chữ bắt buộc, chữ viết tắt (bảng của giai đoạn 1 + bảng chữ mới của bộ)."""
    points = sc.load_points(lessons_dir / "points.yaml")
    glossaries = {REPO / "java-system-design" / "video-gd1" / "01-bang-chu-viet-tat.md",
                  lessons_dir.parent / "01-bang-chu-viet-tat.md"}
    terms = set().union(*(coverage.glossary_terms(g) for g in glossaries if g.exists()))
    for p in sorted(lessons_dir.glob("ep*.yaml")):
        lesson = sc.load(p)
        assert sc.check(lesson, p, points) == [], p.name
        assert coverage.acronym_report(lesson, terms) == [], p.name


@pytest.mark.skipif(not LESSONS.exists(), reason="chưa có kịch bản bộ video")
def test_week1_points_all_covered():
    data = coverage.collect(LESSONS, LESSONS / "points.yaml", week=1)
    missing = [pid for pid, claims in data["claims"].items() if not claims]
    assert missing == []


def test_series_defaults_apply(tmp_path):
    (tmp_path / "series.yaml").write_text(
        'series: "Java System Design · Phase 2"\n'
        'speakers:\n  emma: {name: Emma, role: narrator, color: "#A3346B", voice: {kokoro: af_heart}}\n',
        encoding="utf-8")
    p = tmp_path / "ep01.yaml"
    p.write_text("id: ep01\nep: 1\ntitle: t\nscenes:\n  - kind: bullets\n    items: [{t: a}]\n"
                 "    lines:\n      - emma: hello\n", encoding="utf-8")
    lesson = sc.load(p)
    assert lesson.series.endswith("Phase 2")
    assert lesson.speakers["emma"].voice_for("kokoro") == "af_heart"
    assert "tom" not in lesson.speakers


def test_acronym_starting_with_digit_is_checked():
    lesson = sc.Lesson.model_validate({"id": "x", "ep": 1, "title": "t", "scenes": [
        {"kind": "acronyms", "items": [{"abbr": "RPS", "full": "requests per second"}], "lines": [{"tom": "hi"}]},
        {"kind": "bullets", "items": [{"t": "Avoid 2PC at 500 RPS with 64MB pages"}], "lines": [{"tom": "ok"}]}]})
    problems = coverage.acronym_report(lesson, {"RPS", "2PC"})
    assert any("'2PC'" in m and "thẻ" in m for m in problems)
    assert not any("MB" in m for m in problems)
