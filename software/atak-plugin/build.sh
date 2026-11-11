#!/usr/bin/env bash
set -euo pipefail
PLUGIN_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$PLUGIN_ROOT/../.." && pwd)"
[[ -n "${JAVA_HOME:-}" ]] || { echo "JAVA_HOME に JDK 17 のディレクトリを指定してください（例: JAVA_HOME=/path/to/jdk-17 bash software/atak-plugin/build.sh ...）" >&2; exit 1; }
export JAVA_HOME
export GRADLE_USER_HOME="$REPO_ROOT/out/atak-plugin/.gradle-home"
export ANDROID_USER_HOME="$REPO_ROOT/out/atak-plugin/.android-home"
unset ANDROID_PREFS_ROOT ANDROID_SDK_HOME
mkdir -p "$ANDROID_USER_HOME"
GRADLE_BIN="${JTAK_GRADLE:-$GRADLE_USER_HOME/wrapper/dists/gradle-8.14.3-all/10utluxaxniiv4wxiphsi49nj/gradle-8.14.3/bin/gradle}"
[[ -x "$GRADLE_BIN" ]] || { echo "Gradle 8.14.3 のローカルキャッシュを用意してください: $GRADLE_BIN" >&2; exit 1; }
cd "$PLUGIN_ROOT"
exec "$GRADLE_BIN" --offline --no-daemon -Pandroid.builder.sdkDownload=false "-Duser.home=$REPO_ROOT/out/atak-plugin" "$@"
