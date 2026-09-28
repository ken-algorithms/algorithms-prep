#!/usr/bin/env bash
# ./build.sh | ./build.sh preview | VOICE=file ./build.sh — chi tiết xem ../../common/build.sh
cd "$(dirname "$0")" && exec ../../common/build.sh "$@"
