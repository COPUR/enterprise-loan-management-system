#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

cd "${REPO_ROOT}"
git config core.hooksPath .githooks

echo "Installed Git hooks path: .githooks"
echo "Pre-commit governance checks are now active."
