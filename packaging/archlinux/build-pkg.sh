#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "==> Building distributable with Gradle..."
cd "$ROOT_DIR"
./gradlew :desktopApp:packagePkgTarGz

echo "==> Package ready at:"
ls -lh "$ROOT_DIR/artifacts/desktop-linux"/*.pkg.tar.gz
