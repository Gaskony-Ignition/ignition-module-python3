#!/bin/bash

# Documentation Consistency Checker
# Validates version references, file existence, and documentation completeness

set -e

echo "=================================="
echo "Documentation Consistency Checker"
echo "=================================="
echo ""

ERRORS=0
WARNINGS=0

# Colors for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Get current version from version.properties
VERSION_FILE="python3-integration/version.properties"
if [ ! -f "$VERSION_FILE" ]; then
    echo -e "${RED}ERROR: version.properties not found${NC}"
    exit 1
fi

MAJOR=$(grep "version.major=" "$VERSION_FILE" | cut -d'=' -f2)
MINOR=$(grep "version.minor=" "$VERSION_FILE" | cut -d'=' -f2)
PATCH=$(grep "version.patch=" "$VERSION_FILE" | cut -d'=' -f2)
CURRENT_VERSION="$MAJOR.$MINOR.$PATCH"

echo "Current version: $CURRENT_VERSION"
echo ""

# Function to check file exists
check_file() {
    if [ ! -f "$1" ]; then
        echo -e "${RED}ERROR: Required file not found: $1${NC}"
        ((ERRORS++))
        return 1
    fi
    return 0
}

# Function to check version in file
check_version_in_file() {
    local file=$1
    local pattern=$2
    local description=$3

    if ! check_file "$file"; then
        return
    fi

    if ! grep -q "$pattern" "$file"; then
        echo -e "${RED}ERROR: Version mismatch in $file${NC}"
        echo -e "  Expected: $description"
        ((ERRORS++))
    else
        echo -e "${GREEN}✓${NC} $file - Version OK"
    fi
}

# Function to check for broken links
check_broken_links() {
    local file=$1

    if ! check_file "$file"; then
        return
    fi

    # Extract markdown links [text](path)
    grep -oP '\[.*?\]\(\K[^)]+' "$file" 2>/dev/null | while read -r link; do
        # Skip external URLs
        if [[ $link =~ ^https?:// ]]; then
            continue
        fi

        # Skip anchors
        if [[ $link =~ ^# ]]; then
            continue
        fi

        # Resolve relative path
        local dir=$(dirname "$file")
        local resolved="$dir/$link"

        if [ ! -f "$resolved" ]; then
            echo -e "${YELLOW}WARNING: Broken link in $file: $link${NC}"
            ((WARNINGS++))
        fi
    done
}

echo "=== Checking Version Consistency ==="
echo ""

# Check version.properties
check_file "$VERSION_FILE"

# Check DesignerHook.java fallback version
check_version_in_file \
    "python3-integration/designer/src/main/java/com/inductiveautomation/ignition/examples/python3/designer/DesignerHook.java" \
    "return \"$CURRENT_VERSION\"" \
    "Fallback version should be $CURRENT_VERSION"

# Check README.md (root)
check_version_in_file \
    "README.md" \
    "version-$CURRENT_VERSION" \
    "Badge version should be $CURRENT_VERSION"

# Check python3-integration/README.md (uses different format)
check_version_in_file \
    "python3-integration/README.md" \
    "**Current Version: v$CURRENT_VERSION**" \
    "Current version should be v$CURRENT_VERSION"

# Check CLAUDE.md
check_version_in_file \
    "CLAUDE.md" \
    "**Current Version: v$CURRENT_VERSION**" \
    "Current version reference should be v$CURRENT_VERSION"

echo ""
echo "=== Checking Required Documentation Files ==="
echo ""

# List of required documentation files
REQUIRED_DOCS=(
    "README.md"
    "CLAUDE.md"
    "CHANGELOG.md"
    "python3-integration/README.md"
    "python3-integration/docs/architecture/OVERVIEW.md"
    "python3-integration/docs/development/TESTING.md"
    "python3-integration/docs/development/VERSION_WORKFLOW.md"
    "python3-integration/docs/getting-started/QUICK_START.md"
    "python3-integration/docs/api/REST_API.md"
    "python3-integration/docs/api/DESIGNER_IDE.md"
)

for doc in "${REQUIRED_DOCS[@]}"; do
    if check_file "$doc"; then
        echo -e "${GREEN}✓${NC} $doc exists"
    fi
done

echo ""
echo "=== Checking for Broken Links ==="
echo ""

# Check markdown files for broken links
find . -name "*.md" -not -path "*/\.*" -not -path "*/build/*" | while read -r mdfile; do
    check_broken_links "$mdfile"
done

echo ""
echo "=== Checking for Zone.Identifier Files ==="
echo ""

ZONE_FILES=$(find . -name "*Zone.Identifier*" -type f 2>/dev/null)
if [ -n "$ZONE_FILES" ]; then
    echo -e "${RED}ERROR: Found Zone.Identifier files (should be deleted):${NC}"
    echo "$ZONE_FILES"
    ((ERRORS++))
else
    echo -e "${GREEN}✓${NC} No Zone.Identifier files found"
fi

echo ""
echo "=== Checking for Temporary Files ==="
echo ""

TEMP_FILES=$(find . -name "*.tmp" -o -name "*.bak" -o -name "*.swp" -o -name "*~" | grep -v build | grep -v ".gradle" 2>/dev/null)
if [ -n "$TEMP_FILES" ]; then
    echo -e "${YELLOW}WARNING: Found temporary files:${NC}"
    echo "$TEMP_FILES"
    ((WARNINGS++))
else
    echo -e "${GREEN}✓${NC} No temporary files found"
fi

echo ""
echo "=== Checking Changelog ==="
echo ""

if check_file "CHANGELOG.md"; then
    # Check if current version is in changelog
    if grep -q "\[$CURRENT_VERSION\]" "CHANGELOG.md"; then
        echo -e "${GREEN}✓${NC} Current version ($CURRENT_VERSION) found in CHANGELOG.md"
    else
        echo -e "${YELLOW}WARNING: Current version ($CURRENT_VERSION) not found in CHANGELOG.md${NC}"
        echo "  Consider adding a changelog entry for this version"
        ((WARNINGS++))
    fi
fi

echo ""
echo "=== Checking Architecture Documentation ==="
echo ""

ARCH_DOC="python3-integration/docs/architecture/OVERVIEW.md"
if check_file "$ARCH_DOC"; then
    # Check for Mermaid diagrams
    DIAGRAM_COUNT=$(grep -c "^```mermaid" "$ARCH_DOC" 2>/dev/null || echo "0")
    echo -e "${GREEN}✓${NC} Architecture document contains $DIAGRAM_COUNT Mermaid diagrams"

    if [ "$DIAGRAM_COUNT" -lt 5 ]; then
        echo -e "${YELLOW}WARNING: Expected at least 5 architecture diagrams${NC}"
        ((WARNINGS++))
    fi
fi

echo ""
echo "=================================="
echo "Documentation Check Summary"
echo "=================================="
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo ""
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ $WARNINGS warning(s) found${NC}"
    echo ""
    exit 0
else
    echo -e "${RED}✗ $ERRORS error(s) found${NC}"
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠ $WARNINGS warning(s) found${NC}"
    fi
    echo ""
    exit 1
fi
