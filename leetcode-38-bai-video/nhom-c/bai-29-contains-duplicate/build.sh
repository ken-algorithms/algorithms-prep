#!/usr/bin/env bash
# Build bài 29: giọng đọc → video 1080p có tiếng → GIF walkthrough.
#   ./build.sh          bản chính thức (1080p30, ~2-3 phút render)
#   ./build.sh preview  bản nháp 480p15 để duyệt nhanh bố cục
set -euo pipefail
cd "$(dirname "$0")"

VENV="$(cd ../.. && pwd)/.venv"
[ -x "$VENV/bin/manim" ] || ../../setup-env.sh
export PATH="$VENV/bin:$PATH"
MEDIA="$PWD/build"

python3 make_voice.py

if [ "${1:-}" = "preview" ]; then
  manim render -ql --media_dir "$MEDIA" scene.py ContainsDuplicateVideo
  cp "$MEDIA/videos/scene/480p15/ContainsDuplicateVideo.mp4" bai-29-preview.mp4
  echo "→ bai-29-preview.mp4"
  exit 0
fi

manim render -r 1920,1080 --fps 30 --media_dir "$MEDIA" scene.py ContainsDuplicateVideo
cp "$MEDIA/videos/scene/1080p30/ContainsDuplicateVideo.mp4" bai-29-full.mp4

manim render -r 960,540 --fps 24 --media_dir "$MEDIA" scene.py ContainsDuplicateGif
ffmpeg -loglevel error -y -i "$MEDIA/videos/scene/540p24/ContainsDuplicateGif.mp4" \
  -vf "fps=12,scale=720:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=64[p];[b][p]paletteuse=dither=bayer" \
  bai-29.gif

echo "→ bai-29-full.mp4, bai-29.gif"
