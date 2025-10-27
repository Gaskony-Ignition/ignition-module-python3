# Comprehensive CI/CD Guide
## Python 3 Integration Module for Ignition

**Last Updated:** 2025-10-27
**Status:** Production-Ready
**Platforms:** GitHub Actions + GitLab CI/CD

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [GitHub Actions Workflows](#github-actions-workflows)
4. [GitLab CI/CD Pipeline](#gitlab-cicd-pipeline)
5. [Python Quality Checks](#python-quality-checks)
6. [Matrix Testing](#matrix-testing)
7. [Security Scanning](#security-scanning)
8. [Email Notifications](#email-notifications)
9. [Automated Dependencies](#automated-dependencies)
10. [Best Practices](#best-practices)
11. [Troubleshooting](#troubleshooting)
12. [FAQ](#faq)

---

## 📖 Overview

This project implements a **universal, multi-language CI/CD pipeline** that provides comprehensive quality assurance for the Python 3 Integration module.

### Key Features

✅ **Multi-Platform Support**
- GitHub Actions (6 workflows)
- GitLab CI/CD (comprehensive pipeline)
- Identical functionality across platforms

✅ **Comprehensive Testing**
- Matrix testing across Python 3.9, 3.11, 3.12
- JUnit 5 test suite (184+ tests)
- JaCoCo code coverage (5% minimum, targeting 80%)

✅ **Code Quality**
- Java: Checkstyle (Google style guide)
- Python: Black, Flake8, MyPy, Bandit, pip-audit
- Warnings-only mode for gradual adoption

✅ **Security First**
- OWASP Dependency Check (Java)
- Gitleaks secret scanning
- Bandit security analysis (Python)
- pip-audit vulnerability scanning

✅ **Automated Workflows**
- Auto-deploy on version tags
- Email notifications on failures
- Dependabot dependency updates
- Comprehensive CI summaries

---

## 🏗️ Architecture

### Pipeline Stages

```
┌─────────────┐
│   TRIGGER   │  Push / PR / Tag / Manual
└──────┬──────┘
       │
       ├─────────────────┬──────────────┬──────────────┐
       │                 │              │              │
       v                 v              v              v
  ┌─────────┐     ┌──────────┐   ┌─────────┐   ┌──────────┐
  │  SETUP  │     │   LINT   │   │  TEST   │   │  BUILD   │
  └─────────┘     └──────────┘   └─────────┘   └──────────┘
   - Java 17       - Checkstyle    - JUnit      - Gradle
   - Python 3.x    - Black          - Matrix     - Sign
   - Gradle        - Flake8         - Coverage   - .modl
                   - MyPy
                   - Bandit
       │                 │              │              │
       └─────────────────┴──────────────┴──────────────┘
                          │
                          v
                   ┌──────────────┐
                   │   SECURITY   │
                   └──────────────┘
                    - OWASP Scan
                    - Gitleaks
                    - pip-audit
                          │
                          v
                   ┌──────────────┐
                   │   DEPLOY     │
                   └──────────────┘
                    - GitHub Release
                    - Artifacts
                    - Notifications
```

### Workflow Dependencies

```
test.yml (Matrix: 3.9, 3.11, 3.12)
   ↓
quality.yml (Java + Python)
   ↓
build.yml (if on main/tag)
   ↓
release.yml (if tag)
   ↓
notify.yml (if failure)
```

---

## 🐙 GitHub Actions Workflows

### 1. **Tests and Coverage** (`test.yml`)

**Triggers:**
- Push to `master`, `main`, `develop`
- Pull requests
- Manual dispatch

**Matrix Strategy:**
```yaml
strategy:
  matrix:
    python-version: ['3.9', '3.11', '3.12']
  fail-fast: false
```

**Key Steps:**
1. Setup Java 17 + Python (matrix version)
2. Run Gradle tests with JaCoCo
3. Generate coverage badge (Python 3.11 only)
4. Upload coverage reports (per Python version)
5. Comment coverage on PRs
6. Enforce minimum 5% coverage threshold

**Artifacts:**
- `coverage-report-python-{version}` (90 days)
- `test-results-python-{version}` (90 days)

**Success Criteria:**
- All tests pass across all Python versions
- Coverage ≥ 5% (will increase over time)
- No critical failures in any matrix job

---

### 2. **Build Module** (`build.yml`)

**Triggers:**
- Push to `master`, `main`
- Version tags (`v*`)
- Manual dispatch

**Key Steps:**
1. Setup Java 17 + Python 3.11
2. Execute Gradle clean build
3. Extract version from `version.properties`
4. Upload signed `.modl` file
5. Create GitHub release (on tags)

**Artifacts:**
- `python3-integration-module-{version}` (90 days)

**Success Criteria:**
- Module builds successfully
- `.modl` file is signed with committed certificate
- Version matches `version.properties`

**IMPORTANT:** Does **NOT** regenerate certificates (uses committed ones)

---

### 3. **Code Quality** (`quality.yml`)

**Triggers:**
- Push to `master`, `main`, `develop`
- Pull requests
- Manual dispatch

**Jobs:**

#### **Checkstyle (Java)**
- Google Java Style Guide enforcement
- Runs on main + test code
- Reports uploaded as artifacts

#### **Python Quality** (NEW!)
- **Black** (v23.12.1) - Code formatting check
- **Flake8** (v7.0.0) - PEP 8 linting
- **MyPy** (v1.8.0) - Static type checking
- **Bandit** (v1.7.6) - Security scanning
- **pip-audit** (v2.7.0) - Dependency vulnerabilities

**Configuration Files:**
- `config/python/.flake8`
- `config/python/mypy.ini`
- `config/python/.bandit`

**Current Mode:** Warnings-only (`continue-on-error: true`)

**Enforcement Plan:**
1. **Phase 1** (Current): Warnings-only for 2-4 weeks
2. **Phase 2**: Fix all violations in `python_bridge.py`
3. **Phase 3**: Set `continue-on-error: false` to enforce

---

### 4. **CI - Build and Security** (`ci.yml`)

**Triggers:**
- Push to `master`, `main`, `develop`
- Pull requests
- Manual dispatch

**Jobs:**

1. **build** - Full Gradle build with module signing
2. **security-scan** - Gitleaks + OWASP Dependency Check
3. **code-quality** - Checkstyle analysis
4. **verify-module** - Module metadata validation

**Success Criteria:**
- Build completes without errors
- No secrets detected by Gitleaks
- Dependency vulnerabilities below HIGH threshold
- Module metadata is valid

---

### 5. **Release** (`release.yml`)

**Triggers:**
- Version tags (`v*.*.*`)
- Manual dispatch with version input

**Key Steps:**
1. Extract version from tag or manual input
2. Update `version.properties` (if manual)
3. Build signed module
4. Create GitHub release with notes
5. Attach `.modl` file to release

**Example Tag Creation:**
```bash
git tag v2.8.1
git push origin v2.8.1
```

**Success Criteria:**
- Release created with correct version
- `.modl` file attached
- Release notes auto-generated

---

### 6. **Email Notifications** (`notify.yml`)

**Triggers:**
- Workflow completion (failure only)

**Watches:**
- `test.yml`
- `build.yml`
- `quality.yml`
- `ci.yml`

**Setup Instructions:**

1. **Create App Password** (Gmail example):
   - Go to https://myaccount.google.com/apppasswords
   - Enable 2FA first
   - Create "GitHub Actions" app password
   - Copy the 16-character password

2. **Add GitHub Secrets**:
   ```
   Settings → Secrets and variables → Actions → New repository secret
   ```
   - `SMTP_SERVER` = `smtp.gmail.com`
   - `SMTP_PORT` = `587`
   - `SMTP_USERNAME` = `your-email@gmail.com`
   - `SMTP_PASSWORD` = `your-16-char-app-password`
   - `NOTIFICATION_EMAIL` = `recipient@example.com`

3. **Test**:
   - Trigger a workflow manually
   - Verify email received on failure

**Email Content:**
- HTML formatted
- Workflow details (name, run ID, branch, commit)
- Direct link to failed workflow
- Attached `ci_summary.log`

---

## 🦊 GitLab CI/CD Pipeline

The GitLab pipeline (`.gitlab-ci.yml`) mirrors all GitHub Actions functionality.

### Stages

1. **setup** - Environment preparation
2. **lint** - Checkstyle + Python quality checks
3. **test** - JUnit tests with matrix (3.9, 3.11, 3.12)
4. **build** - Gradle module build
5. **security** - OWASP + Gitleaks scanning
6. **deploy** - Manual deployment stages

### Key Features

✅ **Cache Configuration**
```yaml
cache:
  key: ${CI_COMMIT_REF_SLUG}
  paths:
    - .cache/pip
    - .gradle/wrapper
    - .gradle/caches
```

✅ **Matrix Testing**
```yaml
test:python-3.9:
  extends: test
  before_script:
    - apt-get install -y python3.9
```

✅ **Coverage Reporting**
```yaml
coverage: '/Total.*?([0-9]{1,3})%/'
```

### Workflow Rules

```yaml
workflow:
  rules:
    - if: $CI_PIPELINE_SOURCE == "merge_request_event"
    - if: $CI_COMMIT_BRANCH == "main" || $CI_COMMIT_BRANCH == "master"
    - if: $CI_COMMIT_BRANCH == "develop"
    - if: $CI_COMMIT_TAG
    - if: $CI_PIPELINE_SOURCE == "web"
```

### Running on GitLab

1. Push repository to GitLab
2. GitLab auto-detects `.gitlab-ci.yml`
3. Pipeline runs automatically on commits
4. View results: CI/CD → Pipelines

---

## 🐍 Python Quality Checks

### Configuration Overview

| Tool | Purpose | Config File | Version |
|------|---------|-------------|---------|
| **Black** | Code formatting | `.flake8` (max-line-length) | 23.12.1 |
| **Flake8** | PEP 8 linting | `config/python/.flake8` | 7.0.0 |
| **MyPy** | Type checking | `config/python/mypy.ini` | 1.8.0 |
| **Bandit** | Security scanning | `config/python/.bandit` | 1.7.6 |
| **pip-audit** | Dependency vulns | (no config) | 2.7.0 |

### Flake8 Configuration

**File:** `config/python/.flake8`

```ini
[flake8]
max-line-length = 120
exclude = .git, __pycache__, build, dist
ignore = E203, W503  # Conflicts with Black
max-complexity = 15
select = C,E,F,W,B,B9
```

**Key Settings:**
- Max line length: 120 characters
- Complexity threshold: 15 (McCabe)
- Ignores Black conflicts (E203, W503)

### MyPy Configuration

**File:** `config/python/mypy.ini`

```ini
[mypy]
python_version = 3.9
disallow_untyped_defs = False  # Gradual adoption
check_untyped_defs = True
ignore_missing_imports = True
```

**Strategy:** Gradual type adoption
- Currently allows untyped functions
- Checks typed functions strictly
- Ignores missing stubs for external libs

### Bandit Configuration

**File:** `config/python/.bandit`

```ini
[bandit]
skips = ['B102', 'B307', 'B404', 'B603']
level = MEDIUM
confidence = MEDIUM
```

**Skipped Checks:**
- `B102` - `exec()` usage (intentional in sandbox)
- `B307` - `eval()` usage (intentional in sandbox)
- `B404` - `subprocess` import (needed for shell mode)
- `B603` - `shell=True` usage (intentional in shell mode)

### Running Locally

```bash
# Install tools
pip install black==23.12.1 flake8==7.0.0 mypy==1.8.0 bandit==1.7.6 pip-audit==2.7.0

# Format check
black --check --diff python3-integration/gateway/src/main/resources/python_bridge.py

# Linting
flake8 --config config/python/.flake8 python3-integration/gateway/src/main/resources/python_bridge.py

# Type checking
mypy --config-file config/python/mypy.ini

# Security scan
bandit -c config/python/.bandit -r python3-integration/gateway/src/main/resources/python_bridge.py

# Dependency vulnerabilities
pip-audit
```

### Enforcement Timeline

| Phase | Duration | Mode | Action |
|-------|----------|------|--------|
| 1 | Weeks 1-2 | Warnings-only | Identify violations |
| 2 | Weeks 3-4 | Warnings-only | Fix violations |
| 3 | Week 5+ | Enforced | Fail build on violations |

**To Enforce:**
Edit `.github/workflows/quality.yml`:
```yaml
python-quality:
  continue-on-error: false  # Change from true
```

---

## 🔬 Matrix Testing

### Strategy

Test across multiple Python versions to ensure compatibility.

**Versions Tested:**
- **Python 3.9** - Minimum supported version
- **Python 3.11** - Primary development version
- **Python 3.12** - Latest stable version

**Rationale:**
- Users may have different Python installations
- `python_bridge.py` must work across versions
- Syntax changes between versions (e.g., `match` in 3.10+)

### Configuration

**GitHub Actions:**
```yaml
strategy:
  matrix:
    python-version: ['3.9', '3.11', '3.12']
  fail-fast: false
```

**GitLab CI/CD:**
```yaml
test:python-3.9:
  extends: test
  before_script:
    - apt-get install -y python3.9
```

### Artifact Naming

Artifacts are versioned per Python version:
- `coverage-report-python-3.9`
- `coverage-report-python-3.11`
- `coverage-report-python-3.12`

### Viewing Matrix Results

**GitHub:**
1. Go to Actions → Test workflow run
2. See matrix jobs expanded (3 jobs)
3. Click individual jobs to see logs
4. Download per-version artifacts

**GitLab:**
1. Go to CI/CD → Pipelines
2. Click pipeline → See matrix jobs
3. View per-job logs and artifacts

---

## 🔒 Security Scanning

### OWASP Dependency Check

**Purpose:** Scan Java dependencies for known vulnerabilities

**Configuration:**
```kotlin
// build.gradle.kts
dependencyCheck {
    format = "HTML"
    failBuildOnCVSS = 7.0f  // HIGH or CRITICAL
    scanConfigurations = listOf("runtimeClasspath", "compileClasspath")
}
```

**Reports:**
- `build/reports/dependency-check-report.html`

**Thresholds:**
- CVSS ≥ 7.0 fails the build
- Lower scores generate warnings

### Gitleaks Secret Scanning

**Purpose:** Detect accidentally committed secrets

**Configuration:** Uses default Gitleaks rules

**Detects:**
- API keys
- AWS credentials
- Database passwords
- Private keys
- OAuth tokens

**Example:**
```bash
# Run locally
docker run -v $(pwd):/path zricethezav/gitleaks:latest detect --source /path --verbose
```

### Bandit Security Analysis

**Purpose:** Find common security issues in Python code

**Configuration:** `config/python/.bandit`

**Checks:**
- Hardcoded passwords
- SQL injection risks
- Unsafe YAML/XML parsing
- Insecure random number generation
- Dangerous function calls

### pip-audit

**Purpose:** Scan Python dependencies for vulnerabilities

**Usage:**
```bash
pip-audit --requirement requirements.txt
```

**Note:** Currently `python_bridge.py` uses only stdlib, so no dependencies to scan

---

## 📧 Email Notifications

### Setup Process

#### 1. Gmail Configuration

**Enable 2FA:**
1. https://myaccount.google.com/security
2. Turn on 2-Step Verification
3. Follow prompts

**Create App Password:**
1. https://myaccount.google.com/apppasswords
2. Select "Mail" and "Other (Custom name)"
3. Enter "GitHub Actions"
4. Click "Generate"
5. Copy 16-character password

#### 2. GitHub Secrets

Add these secrets to your repository:

| Secret Name | Value | Example |
|-------------|-------|---------|
| `SMTP_SERVER` | SMTP server address | `smtp.gmail.com` |
| `SMTP_PORT` | SMTP port (TLS) | `587` |
| `SMTP_USERNAME` | Your email | `you@gmail.com` |
| `SMTP_PASSWORD` | App password | `xxxx xxxx xxxx xxxx` |
| `NOTIFICATION_EMAIL` | Recipient email | `team@company.com` |

**Steps:**
1. Go to: `Settings → Secrets and variables → Actions`
2. Click "New repository secret"
3. Add each secret above
4. Click "Add secret"

#### 3. Alternative Email Providers

**SendGrid:**
```yaml
server_address: smtp.sendgrid.net
server_port: 587
username: apikey
password: ${{ secrets.SENDGRID_API_KEY }}
```

**AWS SES:**
```yaml
server_address: email-smtp.us-east-1.amazonaws.com
server_port: 587
username: ${{ secrets.AWS_SMTP_USERNAME }}
password: ${{ secrets.AWS_SMTP_PASSWORD }}
```

### Email Content

**Plain Text:**
- Repository name
- Workflow name
- Branch and commit
- Status
- Direct link to workflow

**HTML:**
- Styled header
- Formatted information blocks
- Action buttons
- Next steps checklist

**Attachments:**
- `ci_summary.log` (comprehensive report)

### Testing

**Manual Test:**
1. Create intentional test failure
2. Push to repository
3. Wait for workflow failure
4. Check email inbox

**Debug:**
- Check GitHub Actions logs for email step
- Verify secrets are set correctly
- Test SMTP credentials manually

---

## 🤖 Automated Dependencies

### Dependabot Configuration

**File:** `.github/dependabot.yml`

### Update Schedule

| Ecosystem | Day | Time | Limit |
|-----------|-----|------|-------|
| GitHub Actions | Monday | 09:00 UTC | 5 PRs |
| Gradle (Java) | Tuesday | 09:00 UTC | 10 PRs |
| Pip (Python) | Wednesday | 09:00 UTC | 5 PRs |

### Features

✅ **Automatic PR Creation**
- Dependabot checks for updates
- Creates PR for each update
- Includes changelog and release notes

✅ **Grouped Updates**
```yaml
groups:
  test-dependencies:
    patterns:
      - "org.junit.jupiter:*"
      - "org.mockito:*"
```

✅ **Ignore Rules**
```yaml
ignore:
  - dependency-name: "com.inductiveautomation.ignitionsdk:*"
    update-types: ["version-update:semver-major"]
```

✅ **Auto-Assign Reviewers**
```yaml
reviewers:
  - "gasko"
assignees:
  - "gasko"
```

### Review Process

1. **Receive PR** from Dependabot
2. **Review Changes**:
   - Check changelog
   - Review release notes
   - Note breaking changes
3. **Run CI/CD**:
   - Verify tests pass
   - Check for new warnings
4. **Merge or Close**:
   - Merge if safe
   - Close if incompatible

### Security Updates

**Automatic:**
- Dependabot security updates run immediately
- Separate from version updates
- High priority

**Enable:**
`Settings → Security → Dependabot security updates`

---

## ✅ Best Practices

### 1. **Certificate Management**

❌ **DO NOT:**
```yaml
# Never regenerate certificates in CI
- name: Generate certificates
  run: keytool -genkeypair ...
```

✅ **DO:**
```yaml
# Use committed certificates
# Certificates are in repository:
#   - keystore.jks
#   - certificate.der
#   - sign.props
```

**Rationale:**
- Consistent signatures across environments
- Reproducible builds
- CI/CD compatibility

### 2. **Version Management**

**Update ALL locations:**
1. `version.properties`
2. `DesignerHook.java` (line 183 fallback)
3. `README.md` (version references)
4. `python3-integration/README.md` (changelog)
5. `CLAUDE.md` (current version)

**Process:**
```bash
# 1. Update version.properties
version.major=2
version.minor=8
version.patch=1

# 2. Update DesignerHook.java fallback
return "2.8.1";  # Line 183

# 3. Commit and tag
git add -A
git commit -m "Release v2.8.1"
git tag v2.8.1
git push origin main
git push origin v2.8.1
```

### 3. **Python Quality Gradual Adoption**

**Phase 1: Observe** (2 weeks)
- Run checks in warnings-only mode
- Review violations
- Understand impact

**Phase 2: Fix** (2 weeks)
- Address violations systematically
- Update code style
- Add type hints

**Phase 3: Enforce** (ongoing)
- Enable build failures
- Maintain standards
- Reject non-compliant PRs

### 4. **Testing Best Practices**

**Local Testing:**
```bash
# Before pushing, run locally:
./gradlew clean test jacocoTestReport
./gradlew checkstyleMain checkstyleTest
./gradlew build
```

**Matrix Testing:**
- Test against multiple Python versions
- Don't assume stdlib consistency
- Check syntax changes (3.9 vs 3.12)

**Coverage Goals:**
| Phase | Target | Timeline |
|-------|--------|----------|
| Current | 5% | Baseline |
| Short-term | 20% | 3 months |
| Mid-term | 40% | 6 months |
| Long-term | 60-80% | 12 months |

### 5. **Security Best Practices**

✅ **DO:**
- Scan dependencies weekly
- Use Dependabot security updates
- Review OWASP reports
- Enable Gitleaks pre-commit hook

❌ **DON'T:**
- Commit secrets (use environment variables)
- Ignore HIGH/CRITICAL vulnerabilities
- Disable security scans
- Use outdated dependencies

### 6. **Workflow Optimization**

**Cache Gradle:**
```yaml
- name: Set up Java
  uses: actions/setup-java@v4
  with:
    cache: 'gradle'  # Faster builds
```

**Parallel Jobs:**
```yaml
# Run independent jobs in parallel
jobs:
  test: ...
  checkstyle: ...  # Runs concurrently with test
```

**Fail-Fast:**
```yaml
strategy:
  fail-fast: false  # Continue testing other versions
```

---

## 🔧 Troubleshooting

### Common Issues

#### 1. **Tests Failing**

**Symptom:** Test workflow fails

**Diagnose:**
```bash
# Run locally
cd python3-integration
./gradlew clean test --info

# Check logs
cat gateway/build/reports/tests/test/index.html
```

**Common Causes:**
- Python not found → Check Python setup step
- Timeout → Increase `timeout-minutes`
- Memory issues → Adjust Gradle heap size

**Fix:**
```yaml
# Increase timeout
timeout-minutes: 20

# Increase memory
env:
  GRADLE_OPTS: "-Xmx2g"
```

#### 2. **Coverage Not Generating**

**Symptom:** Coverage badge not updated

**Diagnose:**
```bash
# Check if CSV exists
ls -la gateway/build/reports/jacoco/test/jacocoTestReport.csv

# Verify permissions
chmod 644 gateway/build/reports/jacoco/test/jacocoTestReport.csv
```

**Fix:**
```yaml
# Ensure jacoco plugin enabled
plugins {
    jacoco
}

# Generate all formats
reports {
    xml.required.set(true)
    html.required.set(true)
    csv.required.set(true)
}
```

#### 3. **Build Failing - Certificate Issues**

**Symptom:** "Certificate not found" or "Invalid signature"

**Diagnose:**
```bash
# Verify certificates exist
ls -la python3-integration/keystore.jks
ls -la python3-integration/certificate.der
ls -la python3-integration/sign.props
```

**Fix:**
- Ensure certificates are committed to repository
- Verify `sign.props` has correct paths
- Do NOT regenerate certificates in CI

#### 4. **Email Notifications Not Working**

**Symptom:** No emails received on failures

**Diagnose:**
```bash
# Check GitHub secrets are set
gh secret list

# Test SMTP manually
openssl s_client -connect smtp.gmail.com:587 -starttls smtp
```

**Fix:**
1. Verify all 5 secrets are set
2. Check app password is correct (16 chars, no spaces)
3. Enable "Less secure app access" if using regular password (not recommended)
4. Check spam folder

#### 5. **Python Quality Checks Failing**

**Symptom:** Quality workflow fails (when not in warnings mode)

**Diagnose:**
```bash
# Run locally
black --check python3-integration/gateway/src/main/resources/python_bridge.py
flake8 --config config/python/.flake8 python3-integration/gateway/src/main/resources/python_bridge.py
mypy --config-file config/python/mypy.ini
bandit -c config/python/.bandit -r python3-integration/gateway/src/main/resources/python_bridge.py
```

**Fix:**
```bash
# Auto-fix Black formatting
black python3-integration/gateway/src/main/resources/python_bridge.py

# Address Flake8 issues manually
# Address MyPy type issues manually
# Review Bandit security issues
```

#### 6. **GitLab Pipeline Stuck**

**Symptom:** GitLab pipeline pending indefinitely

**Diagnose:**
- Check GitLab Runner availability
- Verify Docker images are accessible
- Check cache configuration

**Fix:**
```yaml
# Use shared runners
# Or configure project-specific runner
# Settings → CI/CD → Runners
```

---

## ❓ FAQ

### Q1: Why use matrix testing?

**A:** Users may have different Python versions installed. Testing across 3.9, 3.11, and 3.12 ensures compatibility and catches version-specific issues early.

### Q2: Why are Python checks warnings-only?

**A:** Gradual adoption prevents blocking legitimate work while violations are addressed. After 2-4 weeks of observation and fixes, enforcement can be enabled.

### Q3: Why not regenerate certificates in CI?

**A:** Regenerating creates different signatures each time, breaking reproducible builds and causing consistency issues. Committed certificates ensure identical signatures across all environments.

### Q4: How do I increase coverage threshold?

**A:** Edit `.github/workflows/test.yml` and change:
```yaml
MIN_COVERAGE=5  # Change to 10, 20, etc.
```

### Q5: Can I skip certain workflow runs?

**A:** Yes, use `[skip ci]` in commit message:
```bash
git commit -m "Update docs [skip ci]"
```

### Q6: How do I test workflows locally?

**A:** Use `act` (GitHub Actions locally):
```bash
# Install act
brew install act  # macOS
# or: https://github.com/nektos/act

# Run workflow locally
act -W .github/workflows/test.yml
```

### Q7: What if Dependabot creates too many PRs?

**A:** Adjust limits in `.github/dependabot.yml`:
```yaml
open-pull-requests-limit: 3  # Reduce from 5/10
```

### Q8: How do I disable email notifications temporarily?

**A:** Remove the `NOTIFICATION_EMAIL` secret or disable the `notify.yml` workflow:
```yaml
# Add to workflow
if: false  # Disables entire workflow
```

### Q9: Can I use this CI/CD setup for other projects?

**A:** Yes! The setup is universal and language-agnostic. Copy the workflows and adjust for your stack (replace Gradle with Maven, npm, etc.)

### Q10: How do I add a new workflow?

**A:**
1. Create `.github/workflows/my-workflow.yml`
2. Define triggers, jobs, steps
3. Test with manual dispatch
4. Update `.github/workflows/README.md`
5. Commit and push

---

## 📚 Additional Resources

### Official Documentation

- **GitHub Actions:** https://docs.github.com/en/actions
- **GitLab CI/CD:** https://docs.gitlab.com/ee/ci/
- **Dependabot:** https://docs.github.com/en/code-security/dependabot
- **JaCoCo:** https://www.jacoco.org/jacoco/trunk/doc/
- **Black:** https://black.readthedocs.io/
- **Flake8:** https://flake8.pycqa.org/
- **MyPy:** https://mypy.readthedocs.io/
- **Bandit:** https://bandit.readthedocs.io/

### Internal Documentation

- **Workflow README:** `.github/workflows/README.md`
- **Architecture Guide:** `python3-integration/docs/V2_ARCHITECTURE_GUIDE.md`
- **Testing Guide:** `python3-integration/docs/TESTING_GUIDE.md`
- **Project Guide:** `CLAUDE.md`

### Support

- **GitHub Issues:** https://github.com/gasko/ignition-module-python3/issues
- **Discussions:** https://github.com/gasko/ignition-module-python3/discussions

---

## 🎯 Summary Checklist

Before pushing to production:

- [ ] All workflows passing on GitHub
- [ ] GitLab pipeline configured and tested
- [ ] Email notifications configured and tested
- [ ] Dependabot enabled and reviewed
- [ ] Python quality checks running (warnings-only)
- [ ] Matrix testing across Python 3.9, 3.11, 3.12
- [ ] Code coverage ≥ 5% (target 80%)
- [ ] No HIGH/CRITICAL security vulnerabilities
- [ ] Certificates committed (not regenerated)
- [ ] Version updated in all locations
- [ ] Release notes prepared
- [ ] Documentation updated

---

**This guide is a living document. Please update as the CI/CD pipeline evolves.**

*Last reviewed: 2025-10-27*
