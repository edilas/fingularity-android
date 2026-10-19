#!/usr/bin/env sh
# Lightweight bootstrap for this generated project. Android Studio can also use
# gradle/wrapper/gradle-wrapper.properties directly.
set -eu
VER=8.13
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/fingularity-gradle-$VER"
BIN="$BASE/gradle-$VER/bin/gradle"
if [ ! -x "$BIN" ]; then
  mkdir -p "$BASE"
  ZIP="$BASE/gradle-$VER-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$VER-bin.zip"
  if command -v curl >/dev/null 2>&1; then curl -L --fail "$URL" -o "$ZIP"; \
  elif command -v wget >/dev/null 2>&1; then wget -O "$ZIP" "$URL"; \
  else echo "Install Gradle $VER or open this project in Android Studio." >&2; exit 1; fi
  unzip -q -o "$ZIP" -d "$BASE"
fi
exec "$BIN" "$@"
