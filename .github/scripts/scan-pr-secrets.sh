#!/usr/bin/env bash
set -euo pipefail

: "${BASE_SHA:?Missing pull-request base SHA}"
: "${HEAD_SHA:?Missing pull-request head SHA}"
: "${GITLEAKS_BIN:?Missing verified Gitleaks executable}"

[[ "$BASE_SHA" =~ ^[0-9a-f]{40}$ && "$HEAD_SHA" =~ ^[0-9a-f]{40}$ ]] || exit 1
git cat-file -e "$BASE_SHA^{commit}"
git cat-file -e "$HEAD_SHA^{commit}"
"$GITLEAKS_BIN" git --log-opts="--diff-merges=first-parent $BASE_SHA..$HEAD_SHA" \
  --redact --no-banner --no-color --ignore-gitleaks-allow
