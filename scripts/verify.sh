#!/usr/bin/env bash
set -euo pipefail

# Golden copy verification script for Spring Boot + frontend projects.
# Portable across machines — uses only relative paths and detects environment.
# Idempotent — safe to run multiple times, skips unavailable checks gracefully.
# Usage: ./scripts/verify.sh [--quick|--full]

MODE="${1:---quick}"
FAILED=0
SKIPPED=0
PASS="\033[0;32mPASS\033[0m"
FAIL="\033[0;31mFAIL\033[0m"
SKIP="\033[0;33mSKIP\033[0m"

# Always run from project root, regardless of where the script is called from
cd "$(dirname "$0")/.."
PROJECT_ROOT="$(pwd)"

VERIFY_LOG="${TMPDIR:-/tmp}/diagram-maker-verify-output.log"

check() {
  local label="$1"
  shift
  if "$@" > "$VERIFY_LOG" 2>&1; then
    echo -e "  $PASS  $label"
  else
    echo -e "  $FAIL  $label"
    tail -10 "$VERIFY_LOG" | sed 's/^/         /'
    FAILED=1
  fi
}

skip() {
  local label="$1"
  local reason="$2"
  echo -e "  $SKIP  $label ($reason)"
  SKIPPED=1
}

echo ""
echo "=== diagram-maker verification ($MODE) ==="
echo "    project: $PROJECT_ROOT"
echo ""

# --- Prerequisites ---
if [ ! -f "package.json" ]; then
  echo "ERROR: Not in a project root (package.json not found)"
  exit 1
fi

# --- Security checks ---
echo "Security:"
check "No hardcoded credentials" bash "$PROJECT_ROOT/scripts/security-scan.sh" "$PROJECT_ROOT"

# --- Frontend checks ---
echo ""
echo "Frontend:"

if command -v node > /dev/null 2>&1; then
  if [ ! -d "node_modules" ]; then
    check "npm install" npm install --silent
  else
    echo -e "  $PASS  npm install (node_modules present)"
  fi
  check "Webpack build" npx webpack --mode development
else
  skip "Frontend checks" "Node.js not found"
fi

if [ "$MODE" = "--full" ]; then
  echo ""
  echo "Backend:"

  if command -v docker > /dev/null 2>&1 && docker info > /dev/null 2>&1; then
    if [ -f "docker-compose.yml" ]; then
      check "Docker Compose up" docker compose up -d
    else
      skip "Docker Compose" "docker-compose.yml not found"
    fi
  else
    skip "Docker Compose" "Docker not running"
  fi

  if [ -f "./mvnw" ]; then
    check "Maven build"  ./mvnw clean package -DskipTests -q
    check "Backend tests" ./mvnw test -q
  else
    skip "Maven build" "mvnw not found"
  fi
fi

echo ""
if [ $FAILED -eq 0 ] && [ $SKIPPED -eq 0 ]; then
  echo -e "\033[0;32mAll checks passed.\033[0m"
elif [ $FAILED -eq 0 ]; then
  echo -e "\033[0;33mPassed with skips. Review skipped checks.\033[0m"
else
  echo -e "\033[0;31mSome checks failed. Fix issues before committing.\033[0m"
  exit 1
fi
