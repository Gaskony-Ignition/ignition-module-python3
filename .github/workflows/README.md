# GitHub Actions CI/CD Workflows

This directory contains automated workflows for the Python 3 Integration Module.

---

## 🔄 Workflows

### 1. Tests and Coverage (`test.yml`)
**Triggers:** Push to master/main/develop, Pull Requests, Manual dispatch

**Purpose:** Run all tests and generate coverage reports

**Steps:**
1. ✅ Checkout code
2. ✅ Set up Java 17 (Temurin)
3. ✅ Set up Python 3.11
4. ✅ Verify Python installation
5. ✅ Run tests with JaCoCo coverage
6. ✅ Generate coverage badge
7. ✅ Upload coverage report (HTML)
8. ✅ Upload test results
9. ✅ Comment coverage on PR
10. ✅ Check minimum coverage threshold (5%)
11. ✅ Generate summary

**Artifacts:**
- `coverage-report` - HTML coverage report (90 days retention)
- `test-results` - JUnit test results (90 days retention)

**Coverage Requirements:**
- Minimum overall: 5% (will increase over time)
- Minimum changed files (PR): 60%

---

### 2. Build Module (`build.yml`)
**Triggers:** Push to master/main, Version tags (v*), Manual dispatch

**Purpose:** Build the Ignition module (.modl file)

**Steps:**
1. ✅ Checkout code
2. ✅ Set up Java 17
3. ✅ Set up Python 3.11
4. ✅ Clean build
5. ✅ Extract version from version.properties
6. ✅ Upload module artifact
7. ✅ Create GitHub release (on tag push)
8. ✅ Generate summary

**Artifacts:**
- `python3-integration-module-{version}` - Signed .modl file (90 days retention)

**Releases:**
- Automatically creates GitHub release when pushing version tags
- Attaches .modl file to release
- Generates release notes automatically

---

### 3. Code Quality (`quality.yml`)
**Triggers:** Push to master/main/develop, Pull Requests, Manual dispatch

**Purpose:** Run code quality checks

**Jobs:**

#### Checkstyle
- Runs Checkstyle on main and test code
- Enforces code style standards
- Uploads reports as artifacts

#### Dependency Security Scan
- Runs OWASP Dependency Check
- Scans for known vulnerabilities in dependencies
- Uploads security reports

**Artifacts:**
- `checkstyle-reports` - Code style analysis
- `dependency-check-reports` - Security vulnerability reports

---

## 🎯 Usage

### Viewing Workflow Results

**GitHub UI:**
1. Go to repository → Actions tab
2. Select workflow from left sidebar
3. Click on specific run to see details
4. Download artifacts from run summary

**Command Line:**
```bash
# Using GitHub CLI
gh run list
gh run view <run-id>
gh run download <run-id>
```

### Manual Workflow Dispatch

All workflows support manual triggering:

```bash
# Using GitHub CLI
gh workflow run test.yml
gh workflow run build.yml
gh workflow run quality.yml
```

**GitHub UI:**
1. Go to Actions tab
2. Select workflow
3. Click "Run workflow" button
4. Select branch and run

---

## 📊 Coverage Badges

Coverage badges are automatically generated and can be embedded in README.md:

```markdown
![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)
```

**Badge Updates:**
- Updated on every test run
- Shows current coverage percentage
- Green: ≥80%, Yellow: 60-79%, Red: <60%

---

## 🔧 Configuration

### Coverage Thresholds

Current minimum thresholds (configured in `test.yml`):
- **Overall coverage:** 5% (starter threshold)
- **PR changed files:** 60%

**Increasing thresholds over time:**
1. Edit `.github/workflows/test.yml`
2. Update `MIN_COVERAGE` variable
3. Commit and push

Example progression:
- Phase 1: 5% (current)
- Phase 2: 20% (after basic tests)
- Phase 3: 40% (after API tests)
- Phase 4: 60% (after integration tests)
- Phase 5: 80% (production target)

### Java and Python Versions

Currently configured:
- **Java:** 17 (Temurin distribution)
- **Python:** 3.11

To change versions, edit all three workflow files:
```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    distribution: 'temurin'
    java-version: '17'  # Change here

- name: Set up Python 3
  uses: actions/setup-python@v5
  with:
    python-version: '3.11'  # Change here
```

### Artifact Retention

Default: 90 days for all artifacts

To change:
```yaml
- name: Upload coverage report
  uses: actions/upload-artifact@v4
  with:
    name: coverage-report
    path: ...
    retention-days: 90  # Change here
```

---

## 🚀 Release Process

### Creating a Release

1. **Update version** in `version.properties`:
   ```properties
   version.major=2
   version.minor=5
   version.patch=27
   ```

2. **Commit and tag**:
   ```bash
   git add python3-integration/version.properties
   git commit -m "Release v2.5.27"
   git tag v2.5.27
   git push origin master
   git push origin v2.5.27
   ```

3. **Automatic actions**:
   - Tests run automatically
   - Build workflow creates .modl file
   - GitHub release created with artifact
   - Release notes auto-generated

4. **Verify release**:
   - Check Actions tab for workflow status
   - Check Releases page for new release
   - Download and test .modl file

---

## 📈 Monitoring

### Workflow Status

**Status Badge:**
```markdown
![Tests](https://github.com/{owner}/{repo}/actions/workflows/test.yml/badge.svg)
![Build](https://github.com/{owner}/{repo}/actions/workflows/build.yml/badge.svg)
![Quality](https://github.com/{owner}/{repo}/actions/workflows/quality.yml/badge.svg)
```

**Email Notifications:**
- GitHub sends notifications for failed workflows
- Configure in: Settings → Notifications → Actions

### Metrics to Track

1. **Test Success Rate** - Should stay at 100%
2. **Coverage Trend** - Should increase over time
3. **Build Time** - Should stay under 20 minutes
4. **Dependency Vulnerabilities** - Should be minimal

---

## 🐛 Troubleshooting

### Tests Failing

**Check:**
1. Test results artifact for detailed failure info
2. Workflow logs for error messages
3. Local reproduction: `./gradlew test`

**Common Issues:**
- Python not found → Check Python setup step
- Timeout → Increase `timeout-minutes`
- Memory issues → Adjust Gradle heap size

### Coverage Not Generating

**Check:**
1. JaCoCo CSV file exists in expected path
2. Test execution completed successfully
3. File permissions on reports directory

**Fix:**
```bash
# Verify locally
./gradlew test jacocoTestReport
ls -la gateway/build/reports/jacoco/test/
```

### Build Failing

**Check:**
1. Gradle version compatibility
2. Java version (must be 17)
3. Python version (must be 3.x)
4. Module signing certificates present

**Fix:**
```bash
# Clean build locally
./gradlew clean build --info
```

---

## 📚 References

### GitHub Actions Documentation
- [Workflow syntax](https://docs.github.com/en/actions/using-workflows/workflow-syntax-for-github-actions)
- [GitHub Actions marketplace](https://github.com/marketplace?type=actions)
- [Artifacts](https://docs.github.com/en/actions/using-workflows/storing-workflow-data-as-artifacts)

### Actions Used
- `actions/checkout@v4` - Checkout repository
- `actions/setup-java@v4` - Set up Java
- `actions/setup-python@v5` - Set up Python
- `actions/upload-artifact@v4` - Upload artifacts
- `cicirello/jacoco-badge-generator@v2` - Coverage badges
- `madrapps/jacoco-report@v1.6.1` - PR coverage comments
- `softprops/action-gh-release@v1` - Create releases

---

## 🎯 Best Practices

### 1. Keep Workflows Fast
- Use caching for Gradle dependencies
- Run jobs in parallel when possible
- Set appropriate timeouts

### 2. Meaningful Commit Messages
- Triggers appear in workflow runs
- Helps identify what caused failures

### 3. Test Locally First
```bash
# Before pushing, run locally:
./gradlew clean test jacocoTestReport
./gradlew checkstyleMain checkstyleTest
./gradlew build
```

### 4. Monitor Coverage Trends
- Coverage should increase, not decrease
- New code should have >60% coverage
- Review coverage reports regularly

### 5. Keep Dependencies Updated
- Review dependency-check reports
- Update dependencies regularly
- Test after updates

---

## 📝 Workflow Status

| Workflow | Status | Purpose | Frequency |
|----------|--------|---------|-----------|
| Tests and Coverage | ✅ Active | Quality assurance | Every push/PR |
| Build Module | ✅ Active | Release artifacts | Master push, tags |
| Code Quality | ✅ Active | Code standards | Every push/PR |

**Last Updated:** 2025-10-19
**Next Review:** When coverage reaches 50%
