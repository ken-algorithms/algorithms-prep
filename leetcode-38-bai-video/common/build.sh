#!/usr/bin/env bash
# Build MỘT bài: giọng đọc → video 1080p có tiếng → GIF walkthrough. Gọi từ build.sh của từng bài.
#   ./build.sh          bản chính thức → bai-<N>-full.mp4 + bai-<N>.gif
#   ./build.sh preview  bản nháp 480p15 → bai-<N>-preview.mp4
#   VOICE=linh|file|hoaimy|namminh  TEXT=phienam|tienganh  ./build.sh   (xem common/make_voice.py)
set -euo pipefail
LESSON_DIR="$(pwd)"
COMMON="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
N="$(basename "$LESSON_DIR" | sed -E 's/^bai-([0-9]+)-.*/\1/')"
[[ "$N" =~ ^[0-9]+$ ]] || { echo "Thư mục phải tên bai-<số>-<tên>, đang ở: $LESSON_DIR" >&2; exit 1; }

VENV="$(dirname "$COMMON")/.venv"
[ -x "$VENV/bin/manim" ] || "$(dirname "$COMMON")/setup-env.sh"
export PATH="$VENV/bin:$PATH"
MEDIA="$LESSON_DIR/build"
# Cache của manim làm rơi tiếng: animation lấy từ cache bật skip_animations, add_sound ngay sau đó bị bỏ qua
MANIM="manim render --disable_caching --media_dir $MEDIA"

python "$COMMON/make_voice.py"

if [ "${1:-}" = "preview" ]; then
  $MANIM -ql scene.py LessonVideo
  cp "$MEDIA/videos/scene/480p15/LessonVideo.mp4" "bai-$N-preview.mp4"
  echo "→ bai-$N-preview.mp4"
  exit 0
fi

$MANIM -r 1920,1080 --fps 30 scene.py LessonVideo
cp "$MEDIA/videos/scene/1080p30/LessonVideo.mp4" "bai-$N-full.mp4"

$MANIM -r 960,540 --fps 24 scene.py LessonGif
ffmpeg -loglevel error -y -i "$MEDIA/videos/scene/540p24/LessonGif.mp4" \
  -vf "fps=12,scale=720:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=64[p];[b][p]paletteuse=dither=bayer" \
  "bai-$N.gif"

echo "→ bai-$N-full.mp4, bai-$N.gif"
