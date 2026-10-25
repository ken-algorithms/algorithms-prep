#!/usr/bin/env python3
"""Content checks for tutorial/: what the lessons promise must match the lab on disk.

Only the standard library is needed. It checks, for every tutorial track (tutorial/<id>/):
1. the lesson table in README.md ("## Danh sách bài") lists every NN-*.md file exactly once, in order, and each
   lesson starts with "# Bài NN —", has a **Thời lượng:** line, the three closing sections, and links to the
   previous and next lesson;
2. every code block marked 📄 **`path`** is byte-for-byte the file in the track (after BOM/CRLF normalisation), and
   every block marked ✂️ **`path`** (excerpt) appears verbatim inside that file;
3. every relative link resolves to a file or folder, and every #anchor exists (GitHub's heading-slug rules);
4. the lab's numbers hold: the test files declare as many test cases as results/test-output.txt reports, and the
   README says the same number;
5. nothing from the machine the lab was verified on leaks into the text (temp paths, the registry mirror), and the
   shell scripts are executable.

Run: python3 tests/test_tutorial_content.py   (or pytest tests/test_tutorial_content.py)
"""
from __future__ import annotations

import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TUTORIAL = ROOT / "tutorial"
TRACKS = sorted(p for p in TUTORIAL.iterdir() if p.is_dir() and (p / "README.md").exists())
ROW = re.compile(r"^\|\s*(\d{2})\s*\|\s*\[([^\]]+)\]\(([^)]+\.md)\)\s*\|")
MARK = re.compile(r"^(📄|✂️) \*\*`([^`]+)`\*\*")
FENCE = re.compile(r"^(\s*)(```|~~~)")
LINK = re.compile(r"(?<!!)\[[^\]]*\]\(([^)\s]+)\)")


def norm(text: str) -> str:
    return text.lstrip("﻿").replace("\r\n", "\n").rstrip("\n")


def lessons(track: Path) -> list[Path]:
    return sorted(track.glob("[0-9][0-9]-*.md"))


def md_files(track: Path) -> list[Path]:
    return sorted(p for p in track.rglob("*.md") if not {"bin", "obj"} & set(p.relative_to(track).parts))


def blocks(md: str) -> list[tuple[int, str, str]]:
    """(dòng mở fence, ngôn ngữ, nội dung) của mọi khối code; nội dung đã bỏ thụt lề của fence."""
    out, lines, i = [], md.split("\n"), 0
    while i < len(lines):
        m = FENCE.match(lines[i])
        if not m:
            i += 1
            continue
        indent, fence, start = len(m.group(1)), m.group(2), i
        lang = lines[i].strip()[len(fence):].strip()
        body = []
        i += 1
        while i < len(lines) and not lines[i].strip().startswith(fence):
            body.append(lines[i][indent:] if lines[i][:indent].strip() == "" else lines[i])
            i += 1
        out.append((start, lang, "\n".join(body)))
        i += 1
    return out


def outside_code(md: str) -> str:
    """Văn bản bỏ hết khối code (link, heading trong code không tính)."""
    keep, fence = [], None
    for line in md.split("\n"):
        m = FENCE.match(line)
        if fence is None and m:
            fence = m.group(2)
            keep.append("")
            continue
        if fence and line.strip().startswith(fence):
            fence = None
            keep.append("")
            continue
        keep.append("" if fence else line)
    return "\n".join(keep)


def gh_slug(text: str) -> str:
    """Cùng thuật toán với web/build.py và GitHub."""
    text = re.sub(r"`", "", text.strip())
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)
    text = re.sub(r"<[^>]+>", "", text).lower()
    text = re.sub(r"[^\w\s-]", "", text, flags=re.UNICODE)
    return re.sub(r"\s", "-", text)


def anchors(md: str) -> set[str]:
    seen: set[str] = set()
    for line in outside_code(md).split("\n"):
        m = re.match(r"^#{1,6}\s+(.*?)\s*#*\s*$", line)
        if not m:
            continue
        base = slug = gh_slug(m.group(1))
        n = 0
        while slug in seen or not slug:
            n += 1
            slug = f"{base}-{n}"
        seen.add(slug)
    seen |= set(re.findall(r'<a\s+(?:id|name)="([^"]+)"', md))
    return seen


def test_lesson_table_matches_files():
    assert TRACKS, "no tutorial track found"
    for track in TRACKS:
        readme = (track / "README.md").read_text(encoding="utf-8")
        section = readme.split("## Danh sách bài", 1)[1].split("\n## ", 1)[0]
        rows = [(m.group(1), m.group(3)) for m in map(ROW.match, section.split("\n")) if m]
        files = [p.name for p in lessons(track)]
        assert [f for _, f in rows] == files, (track.name, rows, files)
        assert [n for n, _ in rows] == [f"{i:02d}" for i in range(1, len(rows) + 1)], rows
        for i, (n, f) in enumerate(rows):
            md = (track / f).read_text(encoding="utf-8")
            assert md.startswith(f"# Bài {n} — "), (f, md[:60])
            assert re.search(r"^> \*\*Thời lượng:\*\* .*\d+ phút", md, re.M), f"{f}: missing **Thời lượng:**"
            heads = re.findall(r"^## (?:\d+\. )?(.+)$", outside_code(md), re.M)
            for need in ("Lỗi hay gặp", "Tự kiểm tra", "Ranh giới trung thực"):
                assert any(h.startswith(need) for h in heads), f"{f}: missing section {need}"
            nav = next(line for line in md.split("\n") if line.startswith(">") and "**Tiếp theo:**" in line)
            if i > 0:
                assert f"({rows[i - 1][1]})" in nav, (f, nav)
            if i + 1 < len(rows):
                assert f"({rows[i + 1][1]})" in nav, (f, nav)
            else:
                assert "(README.md)" in nav, (f, nav)
        print(f"OK  {track.name}: {len(rows)} lessons, table == files, headers and prev/next links")


def test_marked_code_blocks_match_the_lab():
    for track in TRACKS:
        full = part = 0
        for md_path in lessons(track) + [track / "README.md"]:
            md = md_path.read_text(encoding="utf-8")
            lines = md.split("\n")
            starts = {start: (lang, body) for start, lang, body in blocks(md)}
            for i, line in enumerate(lines):
                m = MARK.match(line)
                if not m:
                    continue
                kind, rel = m.groups()
                j = i + 1
                while j < len(lines) and not lines[j].strip():
                    j += 1
                assert j in starts, f"{md_path.name}:{i + 1}: {kind} {rel} is not followed by a code block"
                target = track / rel
                assert target.is_file(), f"{md_path.name}:{i + 1}: {rel} does not exist"
                want = norm(target.read_text(encoding="utf-8"))
                got = norm(starts[j][1])
                if kind == "📄":
                    assert got == want, f"{md_path.name}:{i + 1}: block differs from {rel}"
                    full += 1
                else:
                    assert got in want, f"{md_path.name}:{i + 1}: excerpt not found verbatim in {rel}"
                    part += 1
        assert full > 0, track.name
        print(f"OK  {track.name}: {full} full files (📄) and {part} excerpts (✂️) match the lab byte for byte")


def test_links_and_anchors_resolve():
    for track in TRACKS:
        checked = 0
        cache: dict[Path, set[str]] = {}
        for md_path in md_files(track) + [TUTORIAL / "README.md"]:
            md = md_path.read_text(encoding="utf-8")
            for target in LINK.findall(outside_code(md)):
                if target.startswith(("http://", "https://", "mailto:")):
                    continue
                path, _, anchor = target.partition("#")
                dest = (md_path.parent / path).resolve() if path else md_path.resolve()
                assert dest.exists(), f"{md_path.relative_to(ROOT)}: broken link {target}"
                if anchor and dest.suffix == ".md":
                    if dest not in cache:
                        cache[dest] = anchors(dest.read_text(encoding="utf-8"))
                    assert anchor in cache[dest], f"{md_path.relative_to(ROOT)}: no #{anchor} in {dest.name}"
                checked += 1
        print(f"OK  {track.name}: {checked} relative links resolve, anchors included")


def test_lab_numbers_agree():
    lab = TUTORIAL / "dotnet8-rider" / "lab" / "OrderFlow"
    if not lab.exists():
        return
    src = "\n".join(p.read_text(encoding="utf-8") for p in (lab / "tests").rglob("*.cs")
                    if not {"bin", "obj"} & set(p.parts))
    declared = len(re.findall(r"\[Fact\]", src)) + len(re.findall(r"\[InlineData\(", src))
    out = (lab / "results" / "test-output.txt").read_text(encoding="utf-8")
    passed = len(re.findall(r"^Passed ", out, re.M))
    totals = sum(int(n) for n in re.findall(r"^Total tests: (\d+)", out, re.M))
    assert declared == passed == totals, (declared, passed, totals)
    assert "Failed" not in out
    readme = (lab.parent / "README.md").read_text(encoding="utf-8")
    assert f"{passed} test chạy thật" in readme.split("\n", 1)[0], readme.split("\n", 1)[0]
    assert f"{passed}/{passed} pass" in readme, "lab README result row out of date"
    print(f"OK  OrderFlow: {declared} test cases declared == {passed} passed in results/test-output.txt == README")


def test_no_machine_leaks_and_scripts_are_executable():
    bad = re.compile(r"/tmp/claude|scratchpad|mirror\.gcr\.io")
    for p in TUTORIAL.rglob("*"):
        if not p.is_file() or {"bin", "obj"} & set(p.parts):
            continue
        if p.suffix in {".md", ".txt", ".sh", ".cs", ".json", ".yml", ".http", ".csproj"}:
            hit = bad.search(p.read_text(encoding="utf-8", errors="ignore"))
            assert not hit, f"{p.relative_to(ROOT)} contains {hit.group(0)!r}"
        if p.suffix == ".sh":
            assert os.access(p, os.X_OK), f"{p.relative_to(ROOT)} is not executable"
    print("OK  no temp paths or registry mirrors in tutorial/, every .sh is executable")


if __name__ == "__main__":
    test_lesson_table_matches_files()
    test_marked_code_blocks_match_the_lab()
    test_links_and_anchors_resolve()
    test_lab_numbers_agree()
    test_no_machine_leaks_and_scripts_are_executable()
    print("PASS")
