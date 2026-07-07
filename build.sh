#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

VERSION="$(sed -n 's/^version[[:space:]]*=[[:space:]]*//p' gradle.properties | head -n 1 | tr -d '\r')"
if [[ -z "$VERSION" ]]; then
  echo "Could not read version from gradle.properties" >&2
  exit 1
fi

OUTPUT_JAR="plaza-server-${VERSION}.jar"
PAPERCLIP_JAR="plaza-server/build/libs/plaza-paperclip-${VERSION}-mojmap.jar"

echo "==> Applying Plaza/Paper patches"
./gradlew applyAllPatches

echo "==> Building Paperclip distribution jar"
./gradlew createMojmapPaperclipJar

if [[ ! -f "$PAPERCLIP_JAR" ]]; then
  echo "Expected Paperclip jar not found: $PAPERCLIP_JAR" >&2
  exit 1
fi

cp -f "$PAPERCLIP_JAR" "$OUTPUT_JAR"

if jar tf "$OUTPUT_JAR" | grep -q '^net/minecraft/'; then
  echo "Refusing to publish $OUTPUT_JAR: it contains net.minecraft classes." >&2
  exit 1
fi

echo
echo "Built: $OUTPUT_JAR"
echo "Run with: java -jar $OUTPUT_JAR"
