# Automation Scripts

This directory contains automation scripts for maintaining code quality and documentation consistency.

## Available Scripts

### check-docs.sh

**Purpose:** Validates documentation consistency across the repository

**Usage:**
```bash
./scripts/check-docs.sh
```

**What it checks:**
1. **Version Consistency** - Ensures version matches across:
   - `python3-integration/version.properties`
   - `DesignerHook.java` fallback version
   - `README.md` badge
   - `python3-integration/README.md` version header
   - `CLAUDE.md` version reference

2. **Required Documentation Files** - Verifies existence of:
   - README.md (root)
   - CLAUDE.md
   - CHANGELOG.md
   - python3-integration/README.md
   - python3-integration/docs/architecture/OVERVIEW.md
   - python3-integration/docs/development/TESTING.md
   - python3-integration/docs/development/VERSION_WORKFLOW.md
   - python3-integration/docs/getting-started/QUICK_START.md
   - python3-integration/docs/api/REST_API.md
   - python3-integration/docs/api/DESIGNER_IDE.md

3. **Broken Links** - Scans all markdown files for broken internal links

4. **Zone.Identifier Files** - Detects Windows WSL metadata files (should be deleted)

5. **Temporary Files** - Finds leftover temporary files (*.tmp, *.bak, *.swp, *~)

6. **Changelog Validation** - Checks current version is documented in CHANGELOG.md

7. **Architecture Diagrams** - Verifies presence of Mermaid diagrams in architecture docs

**Exit Codes:**
- `0` - All checks passed (or only warnings)
- `1` - One or more errors found

**Example Output:**
```
==================================
Documentation Consistency Checker
==================================

Current version: 2.11.3

=== Checking Version Consistency ===

✓ DesignerHook.java - Version OK
✓ README.md - Version OK
✓ python3-integration/README.md - Version OK
✓ CLAUDE.md - Version OK

=== Checking Required Documentation Files ===

✓ README.md exists
✓ CLAUDE.md exists
...

==================================
Documentation Check Summary
==================================

✓ All checks passed!
```

## Integration with Development Workflow

### Before Committing
Run the documentation check to ensure everything is consistent:
```bash
./scripts/check-docs.sh
```

### After Version Bump
The script will catch version mismatches across files.

### Before Releases
Validate all documentation is up-to-date and properly linked.

## Adding New Scripts

When adding new automation scripts to this directory:

1. **Make executable**: `chmod +x scripts/new-script.sh`
2. **Add to .gitignore** if temporary
3. **Document in this README**
4. **Follow naming convention**: lowercase with hyphens (e.g., `check-something.sh`)
5. **Include help text**: Add `--help` option to show usage

## Git Hooks

### setup-git-hooks.sh

**Purpose:** Installs pre-commit hooks for automated quality checks

**Usage:**
```bash
./scripts/setup-git-hooks.sh
```

**What it does:**
1. Configures git to use `.githooks/` directory
2. Makes hooks executable
3. Tests hook installation

### Pre-commit Hook

**Location:** `.githooks/pre-commit`

**Automated Checks:**
1. **Zone.Identifier Detection** - Prevents WSL metadata files
2. **Temporary Files** - Blocks *.tmp, *.bak, *.swp files
3. **Documentation Consistency** - Runs check-docs.sh
4. **Java Compilation** - Quick compilation check
5. **Debug Statements** - Warns about System.out.println
6. **TODO/FIXME Comments** - Reports comment count

**Skip Checks:**
```bash
git commit --no-verify
```

**Disable Hooks:**
```bash
git config --unset core.hooksPath
```

**Example Output:**
```
🔍 Running pre-commit checks...

=== File Cleanup Checks ===
  ✓ Zone.Identifier files passed
  ✓ No temporary files in commit

=== Documentation Checks ===
  ✓ Documentation consistency passed

=== Compilation Checks ===
  ✓ Java compilation passed

=== Code Quality Checks ===
  ✓ No debug print statements found
  ⚠ 3 TODO/FIXME comment(s) found in staged files

==================================
✓ All pre-commit checks passed!
==================================
```

## Future Automation Ideas

- **CI/CD integration** - Run on pull requests (when GitHub Actions re-enabled)
- **Code style checks** - Integrate Checkstyle or SpotBugs
- **Test coverage** - Monitor test coverage trends with JaCoCo
- **Dependency updates** - Automated Dependabot PRs
- **Performance benchmarks** - Track execution time metrics
