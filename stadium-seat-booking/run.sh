#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_DIR="$ROOT_DIR/build/classes"
LIB_DIR="$ROOT_DIR/lib"
cd "$ROOT_DIR"

mkdir -p "$OUT_DIR"
find "$ROOT_DIR/src/main/java" -name '*.java' -print0 \
  | xargs -0 javac -cp "$LIB_DIR/*" -d "$OUT_DIR"

exec java -cp "$OUT_DIR:$LIB_DIR/*" com.stadium.booking.StadiumBookingApp
