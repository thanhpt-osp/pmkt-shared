#!/usr/bin/env bash
# PMKT git hooks installer (T1.9).
#
# Setup core.hooksPath để git dùng .githooks/ thay vì .git/hooks/.
# Chạy 1 lần / repo sau khi clone.

set -euo pipefail

cd "$(dirname "$0")/.."

git config core.hooksPath .githooks
chmod +x .githooks/*

echo "[pmkt] git hooks đã cài. core.hooksPath = $(git config core.hooksPath)"
echo "[pmkt] Hook hiện có:"
ls -1 .githooks/
