#!/usr/bin/env bash
# Build bài 29: giọng đọc → video 1080p có tiếng → GIF walkthrough.
#   ./build.sh          bản chính thức (1080p30, ~2-3 phút render)
#   ./build.sh preview  bản nháp 480p15 để duyệt nhanh bố cục
#   VOICE=file ./build.sh   dùng bản ghi của bạn trong recordings/ (xem recordings/README.md)
#   VOICE=namminh TEXT=tienganh ./build.sh   đổi giọng / kiểu lời đọc (xem make_voice.py)
set -euo pipefail
cd "$(dirname "$0")"

VENV="$(cd ../.. && pwd)/.venv"
[ -x "$VENV/bin/manim" ] || ../../setup-env.sh
export PATH="$VENV/bin:$PATH"
MEDIA="$PWD/build"
# Cache của manim làm rơi tiếng: animation lấy từ cache bật skip_animations, add_sound ngay sau đó bị bỏ qua
MANIM="manim render --disable_caching --media_dir $MEDIA"

python3 make_voice.py

if [ "${1:-}" = "preview" ]; then
  $MANIM -ql scene.py ContainsDuplicateVideo
  cp "$MEDIA/videos/scene/480p15/ContainsDuplicateVideo.mp4" bai-29-preview.mp4
  echo "→ bai-29-preview.mp4"
  exit 0
fi

$MANIM -r 1920,1080 --fps 30 scene.py ContainsDuplicateVideo
cp "$MEDIA/videos/scene/1080p30/ContainsDuplicateVideo.mp4" bai-29-full.mp4

$MANIM -r 960,540 --fps 24 scene.py ContainsDuplicateGif
ffmpeg -loglevel error -y -i "$MEDIA/videos/scene/540p24/ContainsDuplicateGif.mp4" \
  -vf "fps=12,scale=720:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=64[p];[b][p]paletteuse=dither=bayer" \
  bai-29.gif

echo "→ bai-29-full.mp4, bai-29.gif"
