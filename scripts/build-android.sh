#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[ "${1:-debug}" = debug ] || { echo "Only debug is supported by foundation" >&2; exit 2; }
"$ROOT/scripts/build-rust.sh"
cd "$ROOT/android"
./gradlew --no-daemon assembleDebug
