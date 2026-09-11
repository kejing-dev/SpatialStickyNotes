#!/bin/bash
# Publish this project to github.com/kejing-dev/SpatialStickyNotes (run after `gh auth login`).
set -euo pipefail
cd "$(dirname "$0")/.."
gh repo create kejing-dev/SpatialStickyNotes --private --source . --remote origin --push \
  --description "Spatial Sticky Notes · PICO Spatial SDK sticky notes for Shared Space (PICO Spatial UI, one Planar window per note, far/near LOD, local JSON persistence)"
gh release create v1.0.0 "deliverables/SpatialStickyNotes-debug.apk#SpatialStickyNotes-debug.apk (PICO OS 6 / Emulator 6.1)" \
  --title "v1.0.0 · 空间便利贴首版" --notes-file deliverables/release-notes.md
gh repo view --web >/dev/null 2>&1 || true
