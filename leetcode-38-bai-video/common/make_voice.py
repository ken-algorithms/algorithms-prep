"""Sinh giọng đọc từng đoạn của MỘT bài → audio/<key>.wav + durations.json + script.md.

Chạy trong thư mục bài (build.sh tự làm): python ../../common/make_voice.py

    VOICE=namminh|linh|file|hoaimy   (mặc định namminh)   TEXT=phienam|tienganh   (mặc định phienam)

linh: giọng `say` có sẵn của macOS (offline, miễn phí).
file: dùng bản ghi của bạn trong recordings/<đoạn>.(m4a|mp3|wav|...) — tự cắt khoảng lặng đầu/cuối
      và cân âm lượng; đoạn nào chưa có bản ghi thì tạm dùng giọng Linh.
hoaimy/namminh: giọng neural tiếng Việt của Microsoft qua edge-tts (cần mạng, không cần key).
Đoạn nào không đổi giọng/lời/bản ghi thì giữ nguyên file cũ, không tạo lại.

    python ../../common/make_voice.py --recording-list   # tạo recordings/README.md: danh sách câu cần đọc
"""

import hashlib
import json
import os
import subprocess
import sys
import tempfile
import time
import wave
from pathlib import Path

HERE = Path.cwd()
sys.path.insert(0, str(HERE))
from narration import LESSON, SEGMENTS, TTS_EN  # noqa: E402

AUDIO = HERE / "audio"
MANIFEST = AUDIO / "manifest.json"
RECORDINGS = HERE / "recordings"
RECORDING_EXTS = (".m4a", ".mp3", ".wav", ".aiff", ".aif", ".flac", ".ogg", ".webm", ".mp4")
# Cắt khoảng lặng đầu + cuối (đảo chiều để cắt đuôi), rồi chuẩn hoá âm lượng cho đều giữa các đoạn
CLEANUP_FILTER = (
    "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.15,areverse,"
    "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.15,areverse,"
    "loudnorm=I=-16:TP=-1.5:LRA=11"
)

VOICES = {
    "hoaimy": ("edge", "vi-VN-HoaiMyNeural"),
    "namminh": ("edge", "vi-VN-NamMinhNeural"),
    "linh": ("say", "Linh"),
    "file": ("file", "recordings"),
}
EDGE_RETRIES = 8
EDGE_PAUSE = 1.5  # giây nghỉ giữa các đoạn — gọi dồn dập dễ bị dịch vụ Edge chặn tạm


def tts_text(key: str, mode: str) -> str:
    return TTS_EN.get(key, SEGMENTS[key][1]) if mode == "tienganh" else SEGMENTS[key][1]


def synth_edge(voice: str, text: str, wav: Path) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        mp3 = Path(tmp) / "out.mp3"
        for attempt in range(1, EDGE_RETRIES + 1):
            result = subprocess.run(
                ["edge-tts", "--voice", voice, "--text", text, "--write-media", str(mp3)],
                capture_output=True, text=True,
            )
            if result.returncode == 0 and mp3.exists() and mp3.stat().st_size > 0:
                break
            if attempt == EDGE_RETRIES:
                raise RuntimeError(f"edge-tts lỗi sau {EDGE_RETRIES} lần: {result.stderr.strip()[-300:]}")
            time.sleep(5 * attempt)
        subprocess.run(
            ["ffmpeg", "-loglevel", "error", "-y", "-i", str(mp3), "-ar", "24000", "-ac", "1", str(wav)],
            check=True,
        )


def synth_say(voice: str, text: str, wav: Path) -> None:
    subprocess.run(
        ["say", "-v", voice, "-r", "165", "--data-format=LEI16@24000", "-o", str(wav), text],
        check=True,
    )


def find_recording(key: str) -> Path | None:
    return next((RECORDINGS / f"{key}{ext}" for ext in RECORDING_EXTS if (RECORDINGS / f"{key}{ext}").exists()), None)


def import_recording(src: Path, wav: Path) -> None:
    subprocess.run(
        ["ffmpeg", "-loglevel", "error", "-y", "-i", str(src), "-af", CLEANUP_FILTER, "-ar", "24000", "-ac", "1", str(wav)],
        check=True,
    )


def write_recording_list() -> None:
    RECORDINGS.mkdir(exist_ok=True)
    lines = [
        f"# {LESSON} — danh sách câu cần ghi âm",
        "",
        "Mỗi đoạn một file, **tên file = cột Đoạn** (ví dụ `intro.m4a`, `step0.mp3`), bỏ vào thư mục này.",
        "Định dạng nào cũng được: m4a (Voice Memos iPhone/Mac), mp3, wav, aiff, flac, ogg, webm.",
        "Không cần cắt khoảng lặng đầu/cuối — script tự cắt và cân âm lượng.",
        "Thiếu đoạn nào thì đoạn đó tạm dùng giọng Linh. Build: `VOICE=file ./build.sh`.",
        "",
        "Đọc tự nhiên, thuật ngữ tiếng Anh đọc như bình thường (HashSet, true, false). Nên đọc ở phòng",
        "yên tĩnh, micro cách miệng ~15–20 cm, giữ cùng một chỗ ngồi cho mọi đoạn để giọng đồng đều.",
        "",
        "| # | Đoạn (tên file) | Câu đọc | Phụ đề trên màn hình |",
        "|---|---|---|---|",
    ]
    for i, (key, (caption, phonetic)) in enumerate(SEGMENTS.items(), 1):
        lines.append(f"| {i} | `{key}` | {TTS_EN.get(key, phonetic)} | {caption} |")
    (RECORDINGS / "README.md").write_text("\n".join(lines) + "\n")
    print(f"→ {RECORDINGS / 'README.md'} ({len(SEGMENTS)} đoạn)")


def main() -> None:
    if "--recording-list" in sys.argv:
        write_recording_list()
        return

    voice_key = os.environ.get("VOICE", "namminh")
    mode = os.environ.get("TEXT", "phienam")
    engine, voice = VOICES[voice_key]
    AUDIO.mkdir(exist_ok=True)
    manifest = json.loads(MANIFEST.read_text()) if MANIFEST.exists() else {}

    durations = {}
    generated = 0
    fallback = []
    for key in SEGMENTS:
        text = tts_text(key, mode)
        wav = AUDIO / f"{key}.wav"
        recording = find_recording(key) if engine == "file" else None
        if engine == "file" and recording is None:
            fallback.append(key)
        if recording is not None:
            fingerprint = hashlib.sha1(b"file|" + recording.read_bytes()).hexdigest()
            make = lambda: import_recording(recording, wav)  # noqa: E731
        elif engine == "edge":
            fingerprint = hashlib.sha1(f"edge|{voice}|{text}".encode()).hexdigest()
            make = lambda: synth_edge(voice, text, wav)  # noqa: E731
        else:
            text = tts_text(key, "phienam") if engine == "file" else text
            fingerprint = hashlib.sha1(f"say|Linh|{text}".encode()).hexdigest()
            make = lambda: synth_say("Linh", text, wav)  # noqa: E731
        if not (wav.exists() and manifest.get(key) == fingerprint):
            make()
            manifest[key] = fingerprint
            MANIFEST.write_text(json.dumps(manifest, indent=2))
            generated += 1
            if engine == "edge":
                time.sleep(EDGE_PAUSE)
        with wave.open(str(wav)) as w:
            durations[key] = round(w.getnframes() / w.getframerate(), 3)

    (AUDIO / "durations.json").write_text(json.dumps(durations, indent=2))

    lines = [
        f"# {LESSON} — kịch bản lời đọc (sinh tự động từ narration.py)",
        "",
        f"Giọng: bản ghi `recordings/` ({len(SEGMENTS) - len(fallback)}/{len(SEGMENTS)} đoạn, còn lại Linh)"
        if engine == "file" else f"Giọng: `{voice}` · kiểu lời đọc: `{mode}`",
        "",
        "| Đoạn | Giây | Phụ đề | Lời đọc TTS |",
        "|---|---|---|---|",
    ]
    for key, (caption, _) in SEGMENTS.items():
        lines.append(f"| `{key}` | {durations[key]} | {caption} | {tts_text(key, mode)} |")
    lines.append(f"\nTổng lời đọc: {sum(durations.values()):.1f}s")
    (HERE / "script.md").write_text("\n".join(lines) + "\n")
    if fallback:
        print(f"Chưa có bản ghi, tạm dùng Linh: {', '.join(fallback)}")
    print(f"{voice} / {mode}: {generated} đoạn mới, {len(durations)} đoạn, tổng {sum(durations.values()):.1f}s")


if __name__ == "__main__":
    main()
