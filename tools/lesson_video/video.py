"""Ghép slide + tiếng thành MP4 (H.264 + AAC, có chương) và ghi mục lục lessons.json.

Lấy từ `agents/lesson_video/video.py` của repo superken-ielts/ielts-target-5-5; mục lục ghi thêm `covers` và
`points` (ý chính nào dạy ở giây thứ mấy) để trang web và báo cáo độ phủ dùng.
"""
from __future__ import annotations

import json
import shutil
import subprocess
import wave
from pathlib import Path
from typing import Callable

from .script import Lesson
from .slides import Slides
from .timeline import Timeline

FPS = 15
MANIFEST = "lessons.json"
NOTE = ("Sinh bởi tools/lesson_video từ các file .yaml cùng thư mục — sửa kịch bản rồi chạy lại "
        "`python -m lesson_video build`, không sửa tay file này hay file .mp4.")


def need_ffmpeg() -> str:
    exe = shutil.which("ffmpeg")
    if not exe:
        raise SystemExit("Cần ffmpeg (Ubuntu: sudo apt install ffmpeg; macOS: brew install ffmpeg)")
    return exe


def _meta(lesson: Lesson, tl: Timeline) -> str:
    def esc(s: str) -> str:
        return "".join("\\" + ch if ch in "=;#\\\n" else ch for ch in s)

    out = [";FFMETADATA1", "title=" + esc(f"Ep{lesson.ep:02d} · {lesson.title}"),
           "comment=" + esc(lesson.subtitle)]
    ends = [t for t, _ in tl.chapters[1:]] + [tl.total]
    for (t, name), end in zip(tl.chapters, ends):
        out += ["[CHAPTER]", "TIMEBASE=1/1000", f"START={int(t * 1000)}", f"END={int(end * 1000)}", "title=" + esc(name)]
    return "\n".join(out) + "\n"


def encode(lesson: Lesson, tl: Timeline, slides: Slides, out: Path, work: Path,
           say: Callable[[str], None] = print) -> None:
    ffmpeg = need_ffmpeg()
    work.mkdir(parents=True, exist_ok=True)
    listing = ["ffconcat version 1.0"]
    last = None
    # Thời lượng mỗi ảnh làm tròn theo lưới khung hình trên mốc CỘNG DỒN: làm tròn riêng từng ảnh thì sai số
    # (tới nửa khung mỗi ảnh) cộng lại thành hơn 1 giây sau vài chục cảnh, video dài hơn phần tiếng.
    edge = lambda t: round(t * FPS) / FPS
    for i, fr in enumerate(tl.frames):
        name = f"f{i:04d}.png"
        slides.render(fr, fr.start / tl.total).save(work / name, compress_level=1)
        end = tl.total if i + 1 == len(tl.frames) else tl.frames[i + 1].start
        dur = max(edge(end) - edge(fr.start), 1 / FPS)
        listing += [f"file {name}", f"duration {dur:.6f}"]
        last = name
        if (i + 1) % 50 == 0 or i + 1 == len(tl.frames):
            say(f"  vẽ {i + 1}/{len(tl.frames)} khung hình")
    listing.append(f"file {last}")
    (work / "frames.txt").write_text("\n".join(listing) + "\n", encoding="utf-8")
    with wave.open(str(work / "voice.wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(tl.rate)
        w.writeframes(tl.audio.tobytes())
    (work / "meta.txt").write_text(_meta(lesson, tl), encoding="utf-8")
    out.parent.mkdir(parents=True, exist_ok=True)
    cmd = [ffmpeg, "-y", "-loglevel", "error",
           "-f", "concat", "-safe", "0", "-i", str(work / "frames.txt"),
           "-i", str(work / "voice.wav"), "-i", str(work / "meta.txt"),
           "-map", "0:v", "-map", "1:a", "-map_metadata", "2", "-map_chapters", "2",
           "-r", str(FPS), "-c:v", "libx264", "-preset", "medium", "-tune", "stillimage", "-crf", "26",
           "-pix_fmt", "yuv420p",
           "-af", "loudnorm=I=-16:TP=-1.5:LRA=11", "-c:a", "aac", "-b:a", "64k", "-ar", "44100", "-ac", "1",
           "-movflags", "+faststart", "-t", f"{tl.total:.3f}", str(out)]
    say(f"  mã hóa {out.name}")
    subprocess.run(cmd, check=True)


def probe(path: Path) -> float:
    exe = shutil.which("ffprobe")
    if not exe:
        return 0.0
    r = subprocess.run([exe, "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(path)],
                       capture_output=True, text=True, check=True)
    return float(r.stdout.strip() or 0)


def poster(mp4: Path, at: float = 2.0) -> Path:
    """Ảnh bìa JPG (khung hình ở giây `at`) cạnh file video, dùng cho danh sách video trên web."""
    out = mp4.with_suffix(".jpg")
    subprocess.run([need_ffmpeg(), "-y", "-loglevel", "error", "-ss", f"{at:.2f}", "-i", str(mp4),
                    "-frames:v", "1", "-vf", "scale=640:-2", "-q:v", "4", str(out)], check=True)
    return out


def manifest_entry(lesson: Lesson, tl: Timeline, mp4: Path, yaml_name: str, voices: dict[str, str],
                   engine: str, points_db: dict | None = None, cover: Path | None = None) -> dict:
    points, seen = [], set()
    for i, sc in enumerate(lesson.scenes):
        for pid in sc.points:
            if pid not in seen:
                seen.add(pid)
                pt = (points_db or {}).get(pid, {})
                points.append({"id": pid, "t": round(tl.scene_starts[i], 1), "text": pt.get("text", ""),
                               "src": pt.get("src", ""), "section": pt.get("section", "")})
    return {
        "id": lesson.id,
        "ep": lesson.ep,
        "week": lesson.week,
        "title": lesson.title,
        "subtitle": lesson.subtitle,
        "file": mp4.name,
        "poster": cover.name if cover else "",
        "source": yaml_name,
        "duration": round(tl.total, 1),
        "bytes": mp4.stat().st_size,
        "engine": engine,
        "voices": [f"{sp.name} ({sp.role}) — {voices[k]}" if sp.role else f"{sp.name} — {voices[k]}"
                   for k, sp in lesson.speakers.items()],
        "chapters": [{"t": round(t, 1), "title": name} for t, name in tl.chapters],
        "covers": lesson.all_covers(),
        "points": points,
    }


def write_manifest(lessons_dir: Path, entry: dict) -> Path:
    path = lessons_dir / MANIFEST
    data = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    rows = [x for x in data.get("lessons", []) if x.get("id") != entry["id"]] + [entry]
    rows.sort(key=lambda x: (x["ep"], x["id"]))
    data = {"schema": "jsd-lessons/1", "note": NOTE, "lessons": rows}
    path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    return path
