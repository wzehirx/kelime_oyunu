#!/usr/bin/env bash
# Android SDK kurulumu (cmdline-tools, platform 35, build-tools 35.0.0, platform-tools).
# Kullanim: tools/setup-android-sdk.sh   (tekrar calistirilabilir)
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
CMDLINE_TOOLS_REV="${CMDLINE_TOOLS_REV:-16111833}"
URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_REV}_latest.zip"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

if [ ! -x "$SDKMANAGER" ]; then
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  curl -fsSL -o "$tmp/cmdline-tools.zip" "$URL"
  unzip -q "$tmp/cmdline-tools.zip" -d "$tmp/x"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$tmp/x/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi

# yes | ... SIGPIPE nedeniyle pipefail'i tetikleyebilir; gecici olarak kapat.
set +o pipefail
yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses >/dev/null
set -o pipefail
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# local.properties (gitignored)
echo "sdk.dir=$ANDROID_HOME" > "$REPO_ROOT/local.properties"

echo "Tamam. Kabugunuzda: export ANDROID_HOME=$ANDROID_HOME"
