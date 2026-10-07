"""Bộ đọc giọng nói, miễn phí và chạy offline.

- `kokoro` — Kokoro-82M (Apache-2.0) qua thư viện kokoro-onnx. Cần file model `.onnx` (bản nén q8 ~92 MB) và file
  giọng `.bin` (~0,5 MB mỗi giọng). Giọng tiếng Anh: af_*, am_* (Mỹ), bf_*, bm_* (Anh). Bộ video Java System Design
  dùng Tom = `am_michael`.
- `silent` — không đọc, sinh im lặng dài theo số chữ: soát bố cục slide nhanh và để test.

Lấy từ công cụ `agents/lesson_video` của repo superken-ielts/ielts-target-5-5 (bỏ Flite, giữ nguyên cách nạp giọng).
"""
from __future__ import annotations

import atexit
import os
import tempfile
from pathlib import Path
from typing import Optional

import numpy as np

HOME = Path(os.environ.get("LESSON_VIDEO_HOME") or Path.home() / ".cache" / "lesson_video")
KOKORO_HOME = HOME / "kokoro"
KOKORO_HELP = (
    "Cần model Kokoro q8 (~92 MB) và file giọng, đặt ở " + str(KOKORO_HOME) + " (giọng trong thư mục voices/), "
    "hoặc chỉ đường bằng --model/--voices hay biến KOKORO_MODEL/KOKORO_VOICES. "
    "Hugging Face bị chặn thì lấy từ npm: `npm pack kokoro-q8-shards@1.0.0 kokoro-js@1.2.1`, ghép "
    "kokoro-q8.part0..5.bin thành model_quantized.onnx (sha256 fbae9257…a1478) và chép voices/am_michael.bin. "
    "Xem tools/lesson_video/README.md.")


class TTSError(RuntimeError):
    pass


def load_voices(path: Path) -> dict[str, np.ndarray]:
    """Thư mục *.bin (float32, 510 × 256 — dạng của kho Hugging Face và gói kokoro-js) hoặc file .npz gộp."""
    path = Path(path)
    if path.is_dir():
        out = {}
        for p in sorted(path.glob("*.bin")):
            arr = np.fromfile(p, dtype=np.float32)
            if arr.size == 0 or arr.size % 256:
                raise TTSError(f"File giọng hỏng: {p}")
            out[p.stem] = arr.reshape(-1, 1, 256)
        return out
    with np.load(path) as z:
        return {k: z[k] for k in z.files}


def kokoro_files(model: Optional[str] = None, voices: Optional[str] = None) -> tuple[Optional[Path], Optional[Path]]:
    m = model or os.environ.get("KOKORO_MODEL")
    v = voices or os.environ.get("KOKORO_VOICES")
    if not m and KOKORO_HOME.is_dir():
        found = sorted(KOKORO_HOME.glob("*.onnx"))
        m = found[0] if found else None
    if not v and (KOKORO_HOME / "voices").is_dir():
        v = KOKORO_HOME / "voices"
    m, v = (Path(m) if m else None), (Path(v) if v else None)
    return (m if m and m.exists() else None), (v if v and v.exists() else None)


class Kokoro:
    name = "kokoro"

    def __init__(self, model: Path, voices: Path, speed: float = 0.9):
        if not 0.5 <= speed <= 2.0:
            raise TTSError(f"tốc độ Kokoro phải trong 0,5–2,0: {speed}")
        try:
            from kokoro_onnx import Kokoro as Engine
        except ImportError:
            raise TTSError("Chưa cài kokoro-onnx (xem tools/lesson_video/requirements.txt)") from None
        self.voices = load_voices(voices)
        if not self.voices:
            raise TTSError(f"Không thấy file giọng nào trong {voices}")
        # kokoro-onnx đòi một file giọng .npz; giọng được truyền thẳng dạng mảng khi đọc nên chỉ cần file tạm
        fd, tmp = tempfile.mkstemp(suffix=".npz")
        os.close(fd)
        np.savez(tmp, **{k: v for k, v in list(self.voices.items())[:1]})
        atexit.register(lambda: Path(tmp).unlink(missing_ok=True))
        self.k = Engine(str(model), tmp)
        self.speed = speed
        self.tag = f"kokoro:{Path(model).name}:{Path(model).stat().st_size}:{speed}"

    def check(self, voice: str) -> None:
        if voice not in self.voices:
            raise TTSError(f"Kokoro không có giọng '{voice}' (có: {', '.join(sorted(self.voices))})")
        if voice[:1] not in ("a", "b"):
            raise TTSError(f"Giọng '{voice}' không phải tiếng Anh (cần af_*, am_*, bf_*, bm_*)")

    def describe(self, voice: str) -> str:
        return f"Kokoro {voice}"

    def synth(self, text: str, voice: str) -> tuple[np.ndarray, int]:
        self.check(voice)
        lang = "en-gb" if voice.startswith("b") else "en-us"
        audio, rate = self.k.create(text, self.voices[voice], speed=self.speed, lang=lang)
        return (np.clip(audio, -1, 1) * 32767).astype(np.int16), rate


class Silent:
    name = "silent"
    tag = "silent"
    RATE = 16000

    def check(self, voice: str) -> None:
        pass

    def describe(self, voice: str) -> str:
        return "silent"

    def synth(self, text: str, voice: str) -> tuple[np.ndarray, int]:
        # đo trên Ep00 (Kokoro am_michael, tốc độ 0,9): ~0,076 giây mỗi ký tự → ước tính gần thời lượng thật
        sec = 0.4 + 0.076 * len(text)
        return np.zeros(int(sec * self.RATE), dtype=np.int16), self.RATE


def get(name: str, speed: float = 0.9, model: Optional[str] = None, voices: Optional[str] = None):
    if name == "kokoro":
        m, v = kokoro_files(model, voices)
        if not (m and v):
            raise TTSError(KOKORO_HELP)
        return Kokoro(m, v, speed=speed)
    if name == "silent":
        return Silent()
    raise TTSError(f"bộ đọc không hỗ trợ: {name} (kokoro, silent)")
