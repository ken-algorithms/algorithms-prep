"""Dòng lệnh `python -m lesson_video` (chạy trong thư mục tools/).

    check    <kịch bản.yaml>… [--points points.yaml] [--glossary bang-chu-viet-tat.md]
    build    <kịch bản.yaml>… [--engine kokoro|silent] [--speed 0.9] [--model F.onnx] [--voices DIR]
                              [--out DIR] [--work DIR] [--no-cache]
    frames   <kịch bản.yaml> --out DIR        ảnh PNG cuối mỗi cảnh (dựng im lặng), để soát bố cục
    phon     <kịch bản.yaml>…                 phiên âm Kokoro của các câu có số, ký hiệu, chữ viết tắt
    coverage <thư mục lessons> --points points.yaml [--week 1] [--out FILE.md]

`build` ghi <id>.mp4 cạnh kịch bản và cập nhật lessons.json; có `--out` thì chỉ ghi video vào thư mục đó (xem thử).
Tiếng từng câu được lưu ở ~/.cache/lesson_video/tts nên dựng lại chỉ đọc những câu đã đổi.
"""
from __future__ import annotations

import argparse
import re
import sys
import tempfile
from pathlib import Path

from . import coverage, script as sc, timeline, tts, video
from .slides import Slides


def _default_points(path: Path):
    p = Path(path).resolve().parent / "points.yaml"
    return sc.load_points(p) if p.exists() else None


def _check(args) -> int:
    bad = 0
    terms = coverage.glossary_terms(Path(args.glossary)) if args.glossary else None
    for p in args.lessons:
        lesson = sc.load(Path(p))
        points = sc.load_points(Path(args.points)) if args.points else _default_points(Path(p))
        problems = sc.check(lesson, Path(p), points)
        if terms is not None:
            problems += coverage.acronym_report(lesson, terms)
        for msg in problems:
            print(f"{p}: {msg}")
        bad += bool(problems)
        tl = timeline.build(lesson, tts.Silent(), say=lambda s: None)
        words = sum(len(ln.spoken.split()) for s in lesson.scenes for ln in s.lines if ln.speaker)
        lines = sum(1 for s in lesson.scenes for ln in s.lines if ln.speaker)
        print(f"{p}: {'OK' if not problems else 'CÓ LỖI'} — {len(lesson.scenes)} cảnh, {lines} câu, {words} từ, "
              f"~{coverage.mmss(tl.total)} (ước tính), {len(lesson.chapters())} chương, "
              f"{sum(len(s.points) for s in lesson.scenes)} ý chính")
    return 3 if bad else 0


def _build(args) -> int:
    engine = tts.get(args.engine, speed=args.speed, model=args.model, voices=args.voices)
    cache = None if args.no_cache else Path(args.cache)
    print(f"bộ đọc: {engine.name} ({engine.tag})")
    for p in args.lessons:
        path = Path(p).resolve()
        lesson = sc.load(path)
        problems = sc.check(lesson, path, _default_points(path))
        if problems:
            print("\n".join(f"{p}: {m}" for m in problems))
            return 3
        print(f"{lesson.id}: đọc ({engine.name})")
        tl = timeline.build(lesson, engine, cache_dir=cache)
        voices = {k: engine.describe(s.voice_for(engine.name)) for k, s in lesson.speakers.items()}
        slides = Slides(lesson, {k: f"{lesson.speakers[k].role} · {v}" for k, v in voices.items()})
        out = (Path(args.out).resolve() if args.out else path.parent) / f"{lesson.id}.mp4"
        with tempfile.TemporaryDirectory(prefix="lesson-") as tmp:
            work = Path(args.work).resolve() / lesson.id if args.work else Path(tmp)
            video.encode(lesson, tl, slides, out, work)
        got = video.probe(out)
        if got and abs(got - tl.total) > 1.0:
            print(f"{lesson.id}: video dài {got:.1f}s, lệch so với lời đọc {tl.total:.1f}s", file=sys.stderr)
            return 3
        print(f"{lesson.id}: {out} — {coverage.mmss(tl.total)}, {out.stat().st_size / 1e6:.1f} MB, "
              f"{len(tl.chapters)} chương")
        if not args.out:
            cover = video.poster(out)
            pdb = sc.load_points(path.parent / "points.yaml") if (path.parent / "points.yaml").exists() else None
            entry = video.manifest_entry(lesson, tl, out, path.name, voices, engine.name, pdb, cover)
            print(f"  cập nhật {video.write_manifest(path.parent, entry)}")
    return 0


def _frames(args) -> int:
    path = Path(args.lesson).resolve()
    lesson = sc.load(path)
    tl = timeline.build(lesson, tts.Silent(), say=lambda s: None)
    slides = Slides(lesson, {k: f"{s.role} · Kokoro {s.voice_for('kokoro')}" for k, s in lesson.speakers.items()})
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    picks: dict[int, timeline.Frame] = {}
    for fr in tl.frames:
        if args.all:
            picks[len(picks)] = fr
        else:
            picks[fr.scene] = fr  # khung cuối của mỗi cảnh: đã hiện đủ
    for k, fr in picks.items():
        name = out / (f"{lesson.id}-f{k:03d}.png" if args.all else f"{lesson.id}-s{k:02d}.png")
        slides.render(fr, fr.start / tl.total).save(name)
    print(f"{len(picks)} ảnh → {out}")
    return 0


RISKY = re.compile(r"[0-9~→≈×÷/%^µ²]|\b[A-Z]{2,}|\b[a-z]+[A-Z]")


def _phon(args) -> int:
    from kokoro_onnx.tokenizer import Tokenizer
    tok = Tokenizer()
    for p in args.lessons:
        lesson = sc.load(Path(p))
        for i, s in enumerate(lesson.scenes):
            for ln in s.lines:
                if not ln.speaker or not (ln.say or RISKY.search(ln.text)):
                    continue
                print(f"[{lesson.id} cảnh {i}] {ln.spoken}\n    → {tok.phonemize(ln.spoken, 'en-us')}")
    return 0


def _coverage(args) -> int:
    data = coverage.collect(Path(args.lessons_dir), Path(args.points), args.week)
    md = coverage.to_markdown(data, args.title, args.link_prefix)
    if args.out:
        Path(args.out).write_text(md + "\n", encoding="utf-8")
        print(f"ghi {args.out}")
    else:
        print(md)
    missing = [pid for pid, cs in data["claims"].items() if not cs]
    missing += [a for a, eps in data["scope"].items() if not eps]
    return 3 if missing and args.strict else 0


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(prog="lesson_video", description="Dựng video bài giảng từ kịch bản YAML")
    sub = ap.add_subparsers(dest="cmd", required=True)
    c = sub.add_parser("check", help="kiểm tra kịch bản: cú pháp, anchor nguồn, ý chính, chữ viết tắt")
    c.add_argument("lessons", nargs="+")
    c.add_argument("--points", help="points.yaml (mặc định: cạnh kịch bản)")
    c.add_argument("--glossary", help="bảng chữ viết tắt .md để kiểm chữ viết tắt")
    b = sub.add_parser("build", help="đọc lời, vẽ slide, ghép video")
    b.add_argument("lessons", nargs="+")
    b.add_argument("--engine", default="kokoro", choices=["kokoro", "silent"])
    b.add_argument("--speed", type=float, default=0.9)
    b.add_argument("--model")
    b.add_argument("--voices")
    b.add_argument("--cache", default=str(tts.HOME / "tts"))
    b.add_argument("--no-cache", action="store_true")
    b.add_argument("--out", help="ghi video vào thư mục này, không cập nhật lessons.json")
    b.add_argument("--work", help="giữ khung hình và file tiếng ở thư mục này")
    f = sub.add_parser("frames", help="ảnh PNG cuối mỗi cảnh để soát bố cục")
    f.add_argument("lesson")
    f.add_argument("--out", required=True)
    f.add_argument("--all", action="store_true", help="mọi khung hình, không chỉ khung cuối mỗi cảnh")
    ph = sub.add_parser("phon", help="phiên âm các câu dễ đọc sai")
    ph.add_argument("lessons", nargs="+")
    v = sub.add_parser("coverage", help="bảng ý chính nào đã có video")
    v.add_argument("lessons_dir")
    v.add_argument("--points", required=True)
    v.add_argument("--week", type=int)
    v.add_argument("--title", default="Độ phủ nội dung của video")
    v.add_argument("--link-prefix", default="", help="tiền tố đường dẫn tới file nguồn trong bảng")
    v.add_argument("--out")
    v.add_argument("--strict", action="store_true", help="trả mã lỗi khi còn ý chính hay mục nguồn chưa có video")
    args = ap.parse_args(argv)
    try:
        return {"check": _check, "build": _build, "frames": _frames, "phon": _phon, "coverage": _coverage}[args.cmd](args)
    except tts.TTSError as e:
        print(e, file=sys.stderr)
        return 2
