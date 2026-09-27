"""Sinh giọng đọc từng đoạn bằng `say -v Linh` (macOS) → audio/<key>.wav + durations.json + script.md."""

import json
import subprocess
import wave
from pathlib import Path

from narration import SEGMENTS

HERE = Path(__file__).resolve().parent
AUDIO = HERE / "audio"
VOICE = "Linh"
RATE = 165


def main() -> None:
    AUDIO.mkdir(exist_ok=True)
    durations = {}
    for key, (_, tts) in SEGMENTS.items():
        wav = AUDIO / f"{key}.wav"
        subprocess.run(
            ["say", "-v", VOICE, "-r", str(RATE), "--data-format=LEI16@24000", "-o", str(wav), tts],
            check=True,
        )
        with wave.open(str(wav)) as w:
            durations[key] = round(w.getnframes() / w.getframerate(), 3)
    (AUDIO / "durations.json").write_text(json.dumps(durations, indent=2))

    lines = ["# Bài 29 — kịch bản lời đọc (sinh tự động từ narration.py)", ""]
    lines += ["| Đoạn | Giây | Phụ đề | Lời đọc TTS |", "|---|---|---|---|"]
    for key, (caption, tts) in SEGMENTS.items():
        lines.append(f"| `{key}` | {durations[key]} | {caption} | {tts} |")
    lines.append(f"\nTổng lời đọc: {sum(durations.values()):.1f}s")
    (HERE / "script.md").write_text("\n".join(lines) + "\n")
    print(f"{len(durations)} đoạn, tổng {sum(durations.values()):.1f}s")


if __name__ == "__main__":
    main()
