#!/usr/bin/env bash
# 再配布しない SDK の必要物を、無視対象の out/sdk/ に展開する。
# Extract the required SDK files, which must not be redistributed, into the ignored out/sdk/ directory.
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SDK_ZIP="${ATAK_SDK_ZIP:-$HOME/sdk/atak-civ-5.8.0.9/ATAK-CIV-5.8.0.9-SDK.zip}"
SDK_ROOT="ATAK-CIV-5.8.0.9-SDK"
DEST="$REPO_ROOT/out/sdk"
[[ -f "$SDK_ZIP" ]] || { echo "SDK zip がありません: $SDK_ZIP" >&2; exit 1; }
mkdir -p "$DEST"
unzip -oq "$SDK_ZIP" \
    "$SDK_ROOT/main.jar" \
    "$SDK_ROOT/atak-gradle-takdev.jar" \
    "$SDK_ROOT/atak-javadoc.jar" \
    "$SDK_ROOT/android_keystore" \
    "$SDK_ROOT/samples/PluginTemplateLegacy/*" -d "$DEST"
for item in main.jar atak-gradle-takdev.jar atak-javadoc.jar android_keystore; do
    [[ -f "$DEST/$SDK_ROOT/$item" ]] || { echo "展開物がありません: $item" >&2; exit 1; }
done
mkdir -p "$DEST/javadoc"
unzip -oq "$DEST/$SDK_ROOT/atak-javadoc.jar" -d "$DEST/javadoc"
echo "SDK 展開完了: $DEST/$SDK_ROOT"
