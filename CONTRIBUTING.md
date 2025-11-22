# Contributing to Python 3 Integration Module

Thank you for your interest in contributing! This document provides guidelines for contributing to the Python 3 Integration module for Ignition.

---

## 🎯 Ways to Contribute

### 1. Report Bugs
- **Search existing issues** first to avoid duplicates
- **Use issue templates** when creating new issues
- **Include details:** Version, steps to reproduce, expected vs actual behavior
- **Attach logs:** `<ignition-install>/logs/wrapper.log`

### 2. Suggest Features
- **Check roadmap** first: [CONSOLIDATED_ROADMAP.md](python3-integration/docs/roadmap/CONSOLIDATED_ROADMAP.md)
- **Describe use case:** Why is this feature needed?
- **Provide examples:** How would it work?

### 3. Submit Pull Requests
- **Fork repository**
- **Create feature branch:** `git checkout -b feature/my-feature`
- **Follow code style** (see below)
- **Add tests** for new functionality
- **Update documentation**
- **Submit PR** with clear description

### 4. Improve Documentation
- **Fix typos** and clarify confusing sections
- **Add examples** for common use cases
- **Update guides** when features change
- **Translate docs** (if applicable)

---

## 🏗️ Development Setup

### Prerequisites

- **Java 17+** (Adoptium/Temurin recommended)
- **Gradle 8.10+** (wrapper included)
- **Git**
- **Python 3.9+** (for testing)
- **Ignition 8.3+** (for local testing)

### Clone and Build

```bash
# Clone repository
git clone https://github.com/nigelgwork/ignition-module-python3-java.git
cd ignition-module-python3-java/python3-integration

# Build module
./gradlew clean build --no-daemon

# Output: build/libs/Python3-2.15.9-signed.modl
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests Python3ExecutorTest

# Run tests with coverage
./gradlew clean test jacocoTestReport
open gateway/build/reports/jacoco/test/html/index.html
```

### Local Testing

1. **Install module** in local Ignition Gateway
2. **Test in Script Console:**
   ```python
   system.python3.exec("print('Hello from Python 3!')")
   ```
3. **Test in Designer IDE:** Tools → Python 3 IDE

---

## 📝 Code Style Guidelines

### Java Style

- **Format:** Google Java Style Guide (with modifications)
- **Indentation:** 4 spaces (not tabs)
- **Line length:** 120 characters max
- **Naming:**
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`

**Example:**
```java
public class Python3Executor {
    private static final int DEFAULT_TIMEOUT = 30;

    public Object execute(String code) {
        // Implementation
    }
}
```

### Python Style (python_bridge.py)

- **Format:** PEP 8
- **Indentation:** 4 spaces
- **Line length:** 100 characters max

**Example:**
```python
def execute_code(code, variables):
    """Execute Python code with variables."""
    # Implementation
    pass
```

### Documentation

- **Javadoc** for all public methods and classes
- **Inline comments** for complex logic
- **README updates** for new features
- **Changelog entries** for all changes

---

## 🧪 Testing Guidelines

### Test Coverage Requirements

- **New features:** Minimum 80% coverage
- **Bug fixes:** Add regression test
- **Refactoring:** Maintain existing coverage

### Test Structure

```java
@Test
public void testExecute_ValidCode_ReturnsResult() {
    // Arrange
    String code = "result = 2 + 2";

    // Act
    Object result = executor.execute(code);

    // Assert
    assertEquals(4, result);
}
```

### Test Categories

- **Unit tests:** Individual components (Python3Executor, etc.)
- **Integration tests:** Full workflows (execute → process → return)
- **Smoke tests:** Basic functionality checks

---

## 📚 Documentation Guidelines

### Document Updates Required

When making changes, update these files as needed:
- **README.md** - For user-facing features
- **CHANGELOG.md** - All changes (use Keep a Changelog format)
- **Relevant guides** - Installation, security, operations, etc.
- **API docs** - REST API, Designer IDE features
- **Code comments** - Complex logic explanation

### Documentation Style

- **Clear and concise:** Avoid jargon
- **Examples:** Show, don't just tell
- **Screenshots:** For UI features
- **Code blocks:** Use syntax highlighting

---

## 🔄 Git Workflow

### Branch Naming

- `feature/feature-name` - New features
- `fix/bug-description` - Bug fixes
- `docs/topic` - Documentation updates
- `refactor/component-name` - Code refactoring

### Commit Messages

Use conventional commits format:

```
type(scope): short description

Longer description if needed.

- Detail 1
- Detail 2

Fixes #123
```

**Types:**
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation
- `refactor:` Code refactoring
- `test:` Test updates
- `chore:` Maintenance tasks

**Example:**
```
feat(designer): add autocomplete support

Implemented Jedi-based autocomplete in Designer IDE.

- Wire up Jedi library
- Add completion popup UI
- Support Ctrl+Space trigger

Closes #45
```

### Pull Request Process

1. **Create feature branch** from `main`
2. **Make changes** and commit
3. **Run tests:** `./gradlew clean build test`
4. **Update docs** and changelog
5. **Push to fork**
6. **Create PR** with description
7. **Address review feedback**
8. **Squash and merge** when approved

---

## 🎨 UI/UX Guidelines

### Designer IDE Changes

- **Consistency:** Match Ignition Designer look and feel
- **Accessibility:** Support keyboard shortcuts
- **Dark theme:** Test in both light and dark themes
- **Performance:** No UI freezing during execution

### Testing UI Changes

1. **Light theme test**
2. **Dark theme test**
3. **Keyboard navigation test**
4. **Different screen sizes**

---

## 🐛 Debugging Tips

### Gateway Debugging

```bash
# Tail logs
tail -f <ignition-install>/logs/wrapper.log

# Filter Python3 logs
tail -f wrapper.log | grep Python3

# Enable debug logging (ignition.conf)
wrapper.java.additional.100=-Dlogging.level.com.inductiveautomation.ignition.examples.python3=DEBUG
```

### Designer IDE Debugging

- **Logs:** View in Gateway → Console → Logs
- **Breakpoints:** Attach debugger to Designer JVM
- **Profiling:** Use VisualVM or JProfiler

---

## 📋 Pre-Release Checklist

Before submitting major changes:

- [ ] All tests passing (`./gradlew clean build test`)
- [ ] Code coverage maintained or improved
- [ ] Documentation updated (README, guides, changelog)
- [ ] Version bumped in `version.properties`
- [ ] CHANGELOG.md entry added
- [ ] Manual testing in local Ignition instance
- [ ] No commented-out code
- [ ] No debug statements
- [ ] Git history clean (squash if needed)

---

## 🏆 Recognition

Contributors are recognized in:
- **CHANGELOG.md** for significant contributions
- **README.md** acknowledgments section
- GitHub contributors page

---

## 📞 Questions?

- **Documentation:** [python3-integration/docs/](python3-integration/docs/)
- **Issues:** https://github.com/nigelgwork/ignition-module-python3/issues
- **Discussions:** https://github.com/nigelgwork/ignition-module-python3/discussions

---

## 📜 License

By contributing, you agree that your contributions will be licensed under the same license as the project (MIT License).

---

**Thank you for contributing!** 🎉
