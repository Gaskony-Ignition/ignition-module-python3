# Gradle Deprecation Warnings

**Date:** 2025-11-22
**Gradle Version:** 8.10.2
**Ignition SDK Plugin:** 0.4.1

This document tracks Gradle deprecation warnings and their resolutions.

---

## ⚠️ Current Deprecation Warnings

### 1. moduleDependencies Property (SDK Plugin)

**Severity:** LOW - Build still works, but will break in future versions

**Warning:**
```
w: file:///.../build.gradle.kts:51:5: 'moduleDependencies: MapProperty<String, String>' is deprecated.
Use new moduleDependencySpecs
```

**Location:** `python3-integration/build.gradle.kts:51-53`

**Current Code:**
```kotlin
moduleDependencies.putAll(
    mapOf()
)
```

**Issue:**
The Ignition SDK Gradle plugin has deprecated `moduleDependencies` in favor of a new API called `moduleDependencySpecs`.

**Impact:**
- **Current:** No impact - builds successfully
- **Future:** Will fail when SDK plugin removes deprecated API (likely v1.0.0+)
- **Timeline:** Not urgent - SDK plugin is still v0.4.1

**Resolution Options:**

**Option A: Update to new API (Recommended for long-term)**
```kotlin
// Replace moduleDependencies with moduleDependencySpecs
moduleDependencySpecs.apply {
    // Add module dependencies here if needed
}
```

**Option B: Wait for SDK plugin documentation**
- The new `moduleDependencySpecs` API is not yet fully documented
- Wait for SDK plugin v0.5.0 or v1.0.0 release with migration guide
- Monitor: https://github.com/inductiveautomation/ignition-module-tools

**Recommended Action:** Option B (wait for documentation)
- Current code works fine
- No module dependencies declared (empty map)
- Upgrade when SDK plugin provides migration guide

---

## ✅ Resolved Deprecations

None - This is the only deprecation warning in the project.

---

## 📋 Gradle Compatibility

### Tested Versions
- ✅ Gradle 8.10.2 - Working
- ✅ Gradle 8.8+ - Confirmed compatible
- ⚠️ Gradle 9.0+ - May have issues (see warning above)

### Dependencies
- Ignition SDK Plugin: 0.4.1
- OWASP Dependency Check: 11.1.0
- Checkstyle: Latest
- JaCoCo: Latest

---

## 🔄 Monitoring Plan

### Quarterly Review
1. **Check for SDK plugin updates:**
   ```bash
   # Check latest version
   https://plugins.gradle.org/plugin/io.ia.sdk.modl
   ```

2. **Test with newer Gradle versions:**
   ```bash
   ./gradlew wrapper --gradle-version=8.11
   ./gradlew clean build
   ```

3. **Review SDK plugin changelog:**
   - https://github.com/inductiveautomation/ignition-module-tools/releases

### Before Upgrading Gradle to 9.0+

1. **Update SDK plugin first** (wait for v1.0.0+)
2. **Migrate to `moduleDependencySpecs` API**
3. **Test thoroughly:**
   ```bash
   ./gradlew clean build test
   ```
4. **Update this document** with results

---

## 📝 Notes

- **Current Status:** ✅ All builds passing with only 1 minor deprecation warning
- **Urgency:** LOW - No action required immediately
- **Next Action:** Monitor SDK plugin updates in Q1 2026
- **Owner:** Development team

---

## 🔗 Resources

- **Ignition SDK Tools:** https://github.com/inductiveautomation/ignition-module-tools
- **Gradle Releases:** https://gradle.org/releases/
- **Module Documentation:** https://docs.inductiveautomation.com/docs/8.3/platform/module-dev

---

**Last Updated:** 2025-11-22
**Next Review:** 2026-02-28 (Quarterly)
