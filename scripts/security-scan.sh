#!/usr/bin/env bash
set -euo pipefail

# Portable security scan for hardcoded credentials
# Usage: ./scripts/security-scan.sh [project-dir]
# Scans for passwords, secrets, and credentials in tracked files only.

PROJECT_DIR="${1:-.}"
cd "$PROJECT_DIR"
PROJECT_NAME="$(basename "$(pwd)")"

RED="\033[0;31m"
YELLOW="\033[0;33m"
GREEN="\033[0;32m"
RESET="\033[0m"

ISSUES=0

echo ""
echo "=== Security scan: $PROJECT_NAME ==="
echo ""

# Only scan git-tracked files (not .gitignored files)
if ! git rev-parse --git-dir > /dev/null 2>&1; then
  echo -e "${RED}Not a git repository. Skipping.${RESET}"
  exit 1
fi

# 1. Check if .env is tracked in git
echo "Checking .env tracking..."
if git ls-files --error-unmatch .env > /dev/null 2>&1; then
  echo -e "  ${RED}FAIL${RESET}  .env is tracked in git — remove with: git rm --cached .env"
  ISSUES=$((ISSUES + 1))
else
  echo -e "  ${GREEN}PASS${RESET}  .env not tracked in git"
fi

# 2. Check .gitignore includes .env
if [ -f .gitignore ] && grep -q "^\.env$" .gitignore; then
  echo -e "  ${GREEN}PASS${RESET}  .env in .gitignore"
else
  echo -e "  ${YELLOW}WARN${RESET}  .env not in .gitignore"
fi

# 3. Scan tracked files for hardcoded passwords/secrets
echo ""
echo "Scanning tracked files for hardcoded credentials..."

# Patterns to detect (case-insensitive)
PATTERNS=(
  'password\s*[:=]\s*["\x27]?[A-Za-z0-9!@#$%^&*]{4,}'
  'POSTGRES_PASSWORD:\s*[A-Za-z0-9!@#$%^&*]+'
  'PGADMIN_DEFAULT_PASSWORD:\s*[A-Za-z0-9]+'
  'secret\s*[:=]\s*["\x27][^"\x27]{4,}'
  'api[_-]?key\s*[:=]\s*["\x27][^"\x27]{8,}'
  'token\s*[:=]\s*["\x27][^"\x27]{8,}'
)

# Files to exclude from scan (test files, examples, docs with "how to set" instructions)
EXCLUDE_PATTERNS="\.env\.example|test/|Test\.java|README\.md|CLAUDE\.md|dev-up\.sh"

FOUND_SECRETS=0
for pattern in "${PATTERNS[@]}"; do
  while IFS= read -r file; do
    [ -z "$file" ] && continue
    # Skip excluded files
    if echo "$file" | grep -qE "$EXCLUDE_PATTERNS"; then
      continue
    fi
    matches=$(grep -inE "$pattern" "$file" 2>/dev/null \
      | grep -v '^\s*#' \
      | grep -v '^\s*//' \
      | grep -v '\${.*}' \
      | grep -v 'getProperty\|getenv\|environment\.' \
      | head -5 || true)
    if [ -n "$matches" ]; then
      echo -e "  ${RED}FAIL${RESET}  $file"
      echo "$matches" | sed 's/^/         /'
      FOUND_SECRETS=1
    fi
  done < <(git ls-files -- '*.yml' '*.yaml' '*.properties' '*.json' '*.sh' '*.java' '*.xml' '*.tsx' '*.ts' 'docker-compose*' 2>/dev/null)
done

if [ $FOUND_SECRETS -eq 0 ]; then
  echo -e "  ${GREEN}PASS${RESET}  No hardcoded credentials found in tracked files"
else
  ISSUES=$((ISSUES + 1))
fi

# 4. Check docker-compose uses env var substitution
echo ""
echo "Checking docker-compose for env var usage..."
if [ -f docker-compose.yml ]; then
  if grep -qE 'PASSWORD:\s*\${' docker-compose.yml; then
    echo -e "  ${GREEN}PASS${RESET}  docker-compose.yml uses env var substitution"
  elif grep -qiE 'PASSWORD:' docker-compose.yml; then
    echo -e "  ${RED}FAIL${RESET}  docker-compose.yml has hardcoded passwords"
    ISSUES=$((ISSUES + 1))
  else
    echo -e "  ${GREEN}PASS${RESET}  No passwords in docker-compose.yml"
  fi
else
  echo -e "  ${GREEN}PASS${RESET}  No docker-compose.yml"
fi

echo ""
if [ $ISSUES -eq 0 ]; then
  echo -e "${GREEN}Security scan passed.${RESET}"
else
  echo -e "${RED}Found $ISSUES security issue(s). Fix before committing.${RESET}"
  exit 1
fi
