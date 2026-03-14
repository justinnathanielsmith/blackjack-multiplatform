#!/usr/bin/env bash
# High-efficiency project verification - runs fast checks and JVM unit tests
# Exit codes: 0 = success, non-zero = failure
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=== 1. Running jj fix (Fast Auto-formatting) ==="
if command -v jj &> /dev/null; then
    jj fix
else
    echo "jj not found, skipping jj fix"
fi

echo ""
echo "=== 2. Running Lint Checks (ktlint + detekt) ==="
if ! "$SCRIPT_DIR/lint.sh"; then
    echo "✗ Lint checks failed"
    exit 1
fi

echo ""
echo "=== 3. Running JVM Unit Tests ==="
# We run granular JVM test tasks to avoid time-consuming Android/iOS builds
if ! "$SCRIPT_DIR/amper" test -m core -m data -m sharedUI -m desktopApp --platform jvm; then
    echo "✗ JVM Unit Tests failed"
    exit 1
fi

echo ""
echo "========================================"
echo "✓ Project verified successfully (Quick Flow)"
echo "========================================"
