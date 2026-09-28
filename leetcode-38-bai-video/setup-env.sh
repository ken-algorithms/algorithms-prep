#!/usr/bin/env bash
# Tạo .venv chứa manim + ffmpeg cho mọi video trong leetcode-38-bai-video/. Chạy 1 lần.
# Cần: macOS, Homebrew đã có cairo + pango (`brew install cairo pango`), uv.
set -euo pipefail
cd "$(dirname "$0")"

VENV="$PWD/.venv"
uv venv -q --python 3.12 "$VENV"
export VIRTUAL_ENV="$VENV" PATH="$VENV/bin:$PATH"
uv pip install -q pkgconf

# pycairo build cần zlib/bzip2/expat.pc; macOS có sẵn thư viện nhưng không có file .pc
PC_DIR="$VENV/pkgconfig-stubs"
SDK="$(xcrun --show-sdk-path)"
mkdir -p "$PC_DIR"
for spec in "zlib:z:1.2.12" "bzip2:bz2:1.0.8" "expat:expat:2.5.0"; do
  IFS=: read -r name lib version <<<"$spec"
  printf 'prefix=%s/usr\nincludedir=${prefix}/include\nName: %s\nDescription: macOS SDK\nVersion: %s\nLibs: -l%s\nCflags: -I${includedir}\n' \
    "$SDK" "$name" "$version" "$lib" >"$PC_DIR/$name.pc"
done
export PKG_CONFIG_PATH="/opt/homebrew/lib/pkgconfig:/opt/homebrew/share/pkgconfig:$PC_DIR"

uv pip install -q manim imageio-ffmpeg edge-tts
ln -sf "$("$VENV/bin/python" -c 'import imageio_ffmpeg; print(imageio_ffmpeg.get_ffmpeg_exe())')" "$VENV/bin/ffmpeg"
"$VENV/bin/manim" --version
