#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

PROJECT_VERSION="$(sed -n 's/^version[[:space:]]*=[[:space:]]*//p' gradle.properties | head -n 1 | tr -d '\r')"
if [[ -z "$PROJECT_VERSION" ]]; then
  echo "Could not read version from gradle.properties" >&2
  exit 1
fi

MC_VERSION="$(sed -n 's/^mcVersion[[:space:]]*=[[:space:]]*//p' gradle.properties | head -n 1 | tr -d '\r')"
if [[ -z "$MC_VERSION" ]]; then
  echo "Could not read mcVersion from gradle.properties" >&2
  exit 1
fi

GIT_COMMIT="$(git rev-parse --short=7 HEAD)"

OUTPUT_DIR="$ROOT_DIR/build/libs"
OUTPUT_JAR="$OUTPUT_DIR/plaza-server-${MC_VERSION}-${GIT_COMMIT}.jar"
PAPERCLIP_JAR="$ROOT_DIR/plaza-server/build/libs/plaza-paperclip-${PROJECT_VERSION}-mojmap.jar"

rm -f "$OUTPUT_DIR"/plaza-server-*.jar "$ROOT_DIR"/plaza-server-*.jar

# Avoid stale/confusing Paperweight jar outputs from previous manual builds.
rm -f "$ROOT_DIR"/plaza-server/build/libs/*.jar 2>/dev/null || true

echo "==> Applying Plaza/Paper patches"
./gradlew applyAllPatches

echo "==> Building Paperclip distribution jar"
./gradlew createMojmapPaperclipJar

if [[ ! -f "$PAPERCLIP_JAR" ]]; then
  echo "Expected Paperclip jar not found: $PAPERCLIP_JAR" >&2
  exit 1
fi

if ! unzip -p "$PAPERCLIP_JAR" META-INF/MANIFEST.MF | grep -q '^Main-Class: io.papermc.paperclip.Main'; then
  echo "Refusing to publish $PAPERCLIP_JAR: it is not a Paperclip jar." >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"
cp -f "$PAPERCLIP_JAR" "$OUTPUT_JAR"

if ! unzip -p "$OUTPUT_JAR" META-INF/MANIFEST.MF | grep -q '^Main-Class: io.papermc.paperclip.Main'; then
  echo "Refusing to publish $OUTPUT_JAR: final jar is not Paperclip." >&2
  exit 1
fi

if jar tf "$OUTPUT_JAR" | grep -q '^net/minecraft/'; then
  echo "Refusing to publish $OUTPUT_JAR: it contains net.minecraft classes." >&2
  exit 1
fi

# Keep ./build.sh output unambiguous: only the clean distribution jar remains.
rm -f "$ROOT_DIR"/plaza-server/build/libs/*.jar 2>/dev/null || true

echo
echo "Built: $OUTPUT_JAR"
