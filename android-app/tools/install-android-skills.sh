#!/usr/bin/env bash
set -euo pipefail

revision=c72942584fff970dac86ced78b367fce0c441e52
project="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
target="$project/.agents/skills"
upstream=(
  camera/camerax
  device-ai/litert/litert-lm
  jetpack-compose/adaptive
  jetpack-compose/jetpack-compose-performance
  navigation/navigation-3
  navigation/navigation-event
  performance/anr-audit-and-fix
  security/android-intent-security
  security/android-permissions-security
  system/edge-to-edge
  system/glance-appwidget-updates-and-memory
  system/glance-canonical-layouts
  testing/testing-setup
)

source_dir="${1:-}"
if [[ -z "$source_dir" ]]; then
  source_dir="$(mktemp -d)"
  trap 'rm -rf "$source_dir"' EXIT
  git -C "$source_dir" init --quiet
  git -C "$source_dir" remote add origin https://github.com/android/skills.git
  git -C "$source_dir" sparse-checkout set "${upstream[@]}"
  git -C "$source_dir" fetch --depth=1 origin "$revision"
  git -C "$source_dir" checkout --quiet --detach FETCH_HEAD
fi

for path in "${upstream[@]}"; do
  test -f "$source_dir/$path/SKILL.md"
done

mkdir -p "$target"
for path in "${upstream[@]}"; do
  dest="$target/android-${path##*/}"
  mkdir -p "$dest"
  cp -a "$source_dir/$path/." "$dest/"
  printf 'Installed %s\n' "${dest##*/}"
done
