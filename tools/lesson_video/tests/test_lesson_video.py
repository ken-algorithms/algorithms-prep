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
    ("DDIA, chapter 11", "D D I eigh, chapter 11"),
    ("an SLO; use draw.io at work", "an S L O; use draw dot I O at work"),
    ("retriable or not retryable", "retry able or not retry able"),
    ("Client A locks it. A poll returns; then A's write and topic A.", "Client eigh locks it. A poll returns; then A's write and topic eigh."),
    ("K01 to K07 at 13 Gbit/s", "K 1 to K 7 at 13 gigabits per second"),
    ("PII; dedup by batch_id; Dedup hits; dedups stay", "P I I; dee-doop by batch_id; Dee-doop hits; dedups stay"),
    ("1 µs = 1.7 cores; 0.1 ms; 1 s; 11 ms", "1 microsecond = 1 point 7 cores; 0 point 1 milliseconds; 1 second; 11 milliseconds"),
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


SERIES = sorted((REPO / "java-system-design").glob("video-*/lessons"))
# bảng chữ viết tắt mà mỗi bộ được dùng: bộ sau dựa trên bảng của các bộ trước, mỗi bộ chỉ ghi chữ mới
GLOSSARIES = {"video-gd1": ["video-gd1"], "video-gd2": ["video-gd1", "video-gd2"],
              "video-katalon": ["video-gd1", "video-gd2", "video-katalon"]}


@pytest.mark.skipif(not SERIES, reason="chưa có kịch bản bộ video")
@pytest.mark.parametrize("lessons_dir", SERIES, ids=lambda d: d.parent.name)
def test_real_scripts_pass_check(lessons_dir):
    """Mọi kịch bản của mọi bộ: anchor, chữ bắt buộc, chữ viết tắt (bảng của bộ và của các bộ nó dựa vào)."""
    points = sc.load_points(lessons_dir / "points.yaml")
    names = GLOSSARIES.get(lessons_dir.parent.name, ["video-gd1", lessons_dir.parent.name])
    glossaries = [REPO / "java-system-design" / n / "01-bang-chu-viet-tat.md" for n in names]
    terms = set().union(*(coverage.glossary_terms(g) for g in glossaries if g.exists()))
    paths = sc.lesson_paths(lessons_dir)
    assert paths, lessons_dir
    for p in paths:
        lesson = sc.load(p)
        assert sc.check(lesson, p, points) == [], p.name
        assert coverage.acronym_report(lesson, terms) == [], p.name


@pytest.mark.skipif(not LESSONS.exists(), reason="chưa có kịch bản bộ video")
def test_week1_points_all_covered():
    data = coverage.collect(LESSONS, LESSONS / "points.yaml", week=1)
    missing = [pid for pid, claims in data["claims"].items() if not claims]
    assert missing == []


def test_phase2_points_and_headings_all_covered():
    lessons = REPO / "java-system-design" / "video-gd2" / "lessons"
    data = coverage.collect(lessons, lessons / "points.yaml")
    assert [pid for pid, claims in data["claims"].items() if not claims] == []
    assert [anchor for anchor, eps in data["scope"].items() if not eps] == []


KATALON = REPO / "java-system-design" / "video-katalon" / "lessons"


@pytest.mark.skipif(not KATALON.exists(), reason="chưa có bộ video chen ngang")
def test_interlude_points_and_headings_all_covered():
    data = coverage.collect(KATALON, KATALON / "points.yaml")
    assert [pid for pid, claims in data["claims"].items() if not claims] == []
    assert [anchor for anchor, eps in data["scope"].items() if not eps] == []
    assert {L.code for _, L in data["lessons"]} >= {"K00", "K07"}


def test_prefix_code_and_two_speaker_title_card(tmp_path):
    (tmp_path / "series.yaml").write_text(
        'series: "Java System Design · Katalon interlude"\nprefix: K\n'
        'speakers:\n  tom: {name: Tom, role: interviewer, color: "#1F5FAD", voice: {kokoro: am_michael}}\n'
        '  emma: {name: Emma, role: candidate, color: "#A3346B", voice: {kokoro: af_heart}}\n', encoding="utf-8")
    p = tmp_path / "k03-x.yaml"
    p.write_text("id: k03-x\nep: 3\ntitle: t\nscenes:\n  - kind: title\n    items: [{t: a}]\n"
                 "    lines:\n      - tom: Ten million a minute?\n      - emma: Numbers first.\n", encoding="utf-8")
    assert sc.lesson_paths(tmp_path) == [p]
    lesson = sc.load(p)
    assert lesson.code == "K03" and list(lesson.speakers) == ["tom", "emma"]
    tl = timeline.build(lesson, tts.Silent(), say=lambda s: None)
    slides = Slides(lesson, {"tom": "interviewer · Kokoro am_michael", "emma": "candidate · Kokoro af_heart"})
    assert all(slides.render(fr, 0.5).size == (W, H) for fr in tl.frames)


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


def test_table_font_shrinks_instead_of_overflowing_a_column():
    from PIL import Image, ImageDraw
    from lesson_video.slides import font, wrap
    name = "consumerCrashBeforeOffsetCommit_noDoubleEffect"
    lesson = sc.Lesson.model_validate({"id": "x", "ep": 1, "title": "t", "scenes": [
        {"kind": "table", "columns": ["Test", "Assert"], "widths": [4.6, 4.6],
         "rows": [[name, "the message comes back and is skipped"]], "lines": [{"tom": "ok"}]}]})
    slides = Slides(lesson)
    d = ImageDraw.Draw(Image.new("RGB", (W, H)))
    widths = [(W - 120) / 2] * 2
    size, _, _ = slides._table_layout(d, lesson.scenes[0], widths, 400, True)
    f = font("bold", size)
    assert all(d.textlength(ln, font=f) <= widths[0] - 22 for ln in wrap(d, name, f, widths[0] - 22))
