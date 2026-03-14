#!/bin/sh
set -eu

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd -P)
WRAPPER_JAR="$ROOT_DIR/gradle/wrapper/gradle-wrapper.jar"

if [ -f "$WRAPPER_JAR" ]; then
  echo "gradle-wrapper.jar already exists."
  exit 0
fi

if ! command -v gradle >/dev/null 2>&1; then
  echo "gradle not found. Install Gradle or run from Android Studio."
  exit 1
fi

echo "Generating Gradle wrapper..."
(cd "$ROOT_DIR" && gradle wrapper)

echo "Done: $WRAPPER_JAR"
