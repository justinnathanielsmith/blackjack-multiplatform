#!/usr/bin/env bash
# Unified linting script for ktlint and detekt
set -e

FORMAT=false
if [[ "${1:-}" == "--format" ]]; then
    FORMAT=true
    shift
fi

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting linting process...${NC}"

# Source patterns for ktlint
SOURCES=("shared/**/*.kt" "sharedUI/**/*.kt" "androidApp/**/*.kt" "desktopApp/**/*.kt" "iosApp/**/*.kt")

if [ "$FORMAT" = true ]; then
    echo -e "${GREEN}Running ktlint --format...${NC}"
    ./ktlint --format --relative "${SOURCES[@]}" || echo -e "${RED}ktlint found issues that couldn't be auto-fixed.${NC}"
    
    echo -e "${GREEN}Running detekt --auto-correct...${NC}"
    ./detekt --auto-correct "$@" || echo -e "${RED}detekt found issues that couldn't be auto-fixed.${NC}"
else
    echo -e "${GREEN}Running ktlint...${NC}"
    ./ktlint --relative "${SOURCES[@]}"
    
    echo -e "${GREEN}Running detekt...${NC}"
    ./detekt "$@"
fi

echo -e "${GREEN}Linting process completed successfully!${NC}"
