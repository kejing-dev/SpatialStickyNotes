#!/bin/bash
# Publish release v1.3.0 to github.com/kejing-dev/SpatialStickyNotes (the repo already exists; run after `gh auth login`
# and after the v1.3 commits are pushed, so the release tag lands on the current default-branch head).
#
# Preconditions — the release notes link to the xlsx and to deliverables/screenshots/, so both must be in the pushed
# commit or the Release page shows dead links. Do, in order:
#   1. generate the xlsx (system python3 lacks openpyxl; the script and its venv live in the Claude session scratchpad,
#      NOT in this repo — the path below is the real one on this Mac):
#        cd "/private/tmp/claude-501/-Users-bytedance-Library-Application-Support-Claude-scratch-workspaces-ee303763-c249-47cc-9a31-7602ad58c224-b4f8f9bd-7412-4738-bf09-20e89511fd35-scratch-2026-09-11-01f9fd/b149e5e1-825e-4fe4-85c8-4abc067f8bdb/scratchpad" \
#          && ./venv/bin/python3 build_report_v13.py   # -> deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx (ROOT is absolute, cwd does not matter)
#      (the script aborts unless deliverables/SpatialStickyNotes-debug.apk reports versionName 1.3; the guard below rejects a
#      workbook that does not contain "1.3（versionCode 4）")
#   2. git add README.md deliverables/ (v1.3 screenshots 01_home_list … 10_draft_new replace the v1.2 set) + the Kotlin /
#      gradle changes (lod/ViewerDistance.kt deleted), commit, push
#   3. run this script
set -euo pipefail
cd "$(dirname "$0")/.."
gh auth status
XLSX="deliverables/SpatialStickyNotes_模拟器截图与验证.xlsx"
# The xlsx must be the v1.3 report, not a stale v1.2 one: openpyxl writes inline strings (no xl/sharedStrings.xml), so grep
# the cover sheet for the version cell that build_report_v13.py writes. (grep without -q so unzip never sees SIGPIPE under pipefail.)
test -f "$XLSX" && unzip -p "$XLSX" xl/worksheets/sheet1.xml | grep -F '1.3（versionCode 4）' >/dev/null \
  || { echo "regenerate the xlsx (step 1 above): $XLSX is missing or is not the v1.3 report"; exit 1; }
# Everything (app/ incl. build.gradle.kts, deliverables/, README.md) must be committed AND pushed: the release tag is put on
# origin's default-branch head, so a dirty tree or an unpushed HEAD would tag v1.2 code and leave the notes' links dead.
git fetch -q origin
[ -z "$(git status --porcelain --untracked-files=all)" ] && [ "$(git rev-parse HEAD)" = "$(git rev-parse origin/main)" ] \
  || { echo "commit and push everything (app/, deliverables/, README.md) first: working tree is dirty or HEAD != origin/main"; exit 1; }
gh release create v1.3.0 --target "$(git rev-parse HEAD)" \
  "deliverables/SpatialStickyNotes-debug.apk#SpatialStickyNotes-debug.apk (PICO OS 6 / Emulator 6.1)" \
  --title "v1.3.0 · 手动收起 + 自适应高度" --notes-file deliverables/release-notes.md
gh repo view --web >/dev/null 2>&1 || true
