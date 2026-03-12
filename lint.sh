#!/usr/bin/env bash
# CI lint script - runs both ktlint and detekt
# Exit codes: 0 = success, non-zero = violations found
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXIT_CODE=0

echo "=== Running ktlint ==="
if ! "$SCRIPT_DIR/ktlint"; then
    echo "ktlint found violations"
    EXIT_CODE=1
fi

echo ""
echo "=== Running detekt ==="
if ! "$SCRIPT_DIR/detekt"; then
    echo "detekt found violations"
    EXIT_CODE=1
fi

if [[ $EXIT_CODE -eq 0 ]]; then
    echo ""
    echo "✓ All lint checks passed"
else
    echo ""
    echo "✗ Lint checks failed"
fi

exit $EXIT_CODE
