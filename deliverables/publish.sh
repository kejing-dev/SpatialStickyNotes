#!/bin/bash
# Publish release v1.2.0 to github.com/kejing-dev/SpatialStickyNotes (the repo already exists; run after `gh auth login`
# and after the v1.2 commits are pushed, so the release tag lands on the current default-branch head).
#
# Preconditions — the release notes link to the xlsx and to deliverables/screenshots/, so both must be in the pushed
# commit or the Release page shows dead links. Do, in order:
#   1. generate the xlsx (system python3 lacks openpyxl; use the scratchpad venv):
#        scratchpad/venv/bin/python3 build_report_v12.py   # -> deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx
#   2. git add README.md deliverables/ app/src/main/res/drawable/ic_*.xml + the Kotlin / gradle changes, commit, push
#   3. run this script
set -euo pipefail
cd "$(dirname "$0")/.."
gh auth status
test -f "deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx" && [ -z "$(git status --porcelain --untracked-files=all -- deliverables README.md)" ] || { echo "generate the xlsx and commit/push deliverables first"; exit 1; }
gh release create v1.2.0 "deliverables/SpatialStickyNotes-debug.apk#SpatialStickyNotes-debug.apk (PICO OS 6 / Emulator 6.1)" \
  --title "v1.2.0 · 图标化 UI（英文对话框）" --notes-file deliverables/release-notes.md
gh repo view --web >/dev/null 2>&1 || true
