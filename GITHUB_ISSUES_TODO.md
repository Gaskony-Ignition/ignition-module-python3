# GitHub Issues - TODO Items from Code

**Created:** 2025-11-22
**Source:** Code audit of TODO/FIXME comments

These items should be created as GitHub issues to track future development.

---

## 🔐 Security Features (Priority: MEDIUM)

### Issue 1: Implement Ignition Context API Integration for Security

**File:** `gateway/src/main/java/.../Python3SecurityUtils.java:66`

**Description:**
Implement user authentication checking via Ignition context API.

**Current Code:**
```java
public static boolean isUserAuthenticated(String username) {
    // TODO: Implement when Ignition context API is integrated
    return true; // Temporary: assume all users authenticated
}
```

**Required:**
- Integrate with Ignition's GatewayContext API
- Check actual user authentication status
- Remove placeholder `return true`

**Labels:** `enhancement`, `security`, `gateway`
**Priority:** Medium
**Effort:** 2-4 hours

---

### Issue 2: Implement REST API Integration for Role Checking

**File:** `gateway/src/main/java/.../Python3SecurityUtils.java:78`

**Description:**
Implement user role checking via REST API integration.

**Current Code:**
```java
public static boolean hasRole(String username, String role) {
    // TODO: Implement when REST API integration is complete
    return true; // Temporary: assume all users have all roles
}
```

**Required:**
- Integrate with Ignition's security API
- Check actual user roles
- Remove placeholder `return true`

**Labels:** `enhancement`, `security`, `gateway`
**Priority:** Medium
**Effort:** 2-4 hours

---

## 📡 REST API Endpoints (Priority: LOW-MEDIUM)

### Issue 3: Implement Package Upgrade Endpoint

**File:** `designer/src/main/java/.../PackagesDialog.java:705`

**Description:**
Add REST API endpoint for upgrading Python packages.

**Current Code:**
```java
public JsonObject upgradePackage(String packageName) {
    // TODO: Implement when REST endpoint is available
    throw new UnsupportedOperationException("Package upgrade not yet implemented");
}
```

**Required:**
- Create Gateway REST endpoint: `/api/v1/package-upgrade`
- Implement upgrade logic in Gateway
- Wire up Designer UI to use endpoint

**Labels:** `enhancement`, `rest-api`, `packages`
**Priority:** Low
**Effort:** 4-6 hours

---

### Issue 4: Implement Package Search by Category

**File:** `designer/src/main/java/.../Python3RestClient.java:984`

**Description:**
Add ability to search PyPI packages by category.

**Current Code:**
```java
public List<PackageInfo> searchByCategory(String category) {
    // TODO: Implement when Gateway endpoint is available
    return new ArrayList<>();
}
```

**Required:**
- Research PyPI category API
- Create Gateway endpoint
- Implement search logic
- Wire up UI

**Labels:** `enhancement`, `rest-api`, `packages`
**Priority:** Low
**Effort:** 6-8 hours

---

### Issue 5: Implement Popular Packages Listing

**File:** `designer/src/main/java/.../Python3RestClient.java:1003`

**Description:**
Add endpoint to list most popular Python packages.

**Current Code:**
```java
public List<PackageInfo> getPopularPackages(int limit) {
    // TODO: Implement when Gateway endpoint is available
    return new ArrayList<>();
}
```

**Required:**
- Use PyPI stats API or hardcode popular list
- Create Gateway endpoint
- Implement caching (popular packages change slowly)
- Wire up UI

**Labels:** `enhancement`, `rest-api`, `packages`, `ux`
**Priority:** Low
**Effort:** 4-6 hours

---

### Issue 6: Implement Package Update Checking

**File:** `designer/src/main/java/.../Python3RestClient.java:1024`

**Description:**
Check for available package updates.

**Current Code:**
```java
public Map<String, String> checkForUpdates() {
    // TODO: Implement when Gateway endpoint is available
    return new HashMap<>();
}
```

**Required:**
- Compare installed versions with PyPI latest
- Create Gateway endpoint
- Show update notifications in UI

**Labels:** `enhancement`, `rest-api`, `packages`, `ux`
**Priority:** Medium
**Effort:** 4-6 hours

---

### Issue 7: Implement Dependency Resolution

**File:** `designer/src/main/java/.../Python3RestClient.java:1044`

**Description:**
Show package dependencies before installation.

**Current Code:**
```java
public List<String> resolveDependencies(String packageName) {
    // TODO: Implement when Gateway endpoint is available
    return new ArrayList<>();
}
```

**Required:**
- Query PyPI for package metadata
- Parse dependency tree
- Display in UI before install

**Labels:** `enhancement`, `rest-api`, `packages`, `ux`
**Priority:** Medium
**Effort:** 6-8 hours

---

### Issue 8: Implement Virtual Environment Listing

**File:** `designer/src/main/java/.../Python3RestClient.java:1064`

**Description:**
List all available virtual environments on Gateway.

**Current Code:**
```java
public List<String> listVirtualEnvironments() {
    // TODO: Implement when Gateway endpoint is available
    return new ArrayList<>();
}
```

**Required:**
- Scan Gateway for venv directories
- Create REST endpoint
- Allow switching venvs in UI

**Labels:** `enhancement`, `rest-api`, `virtual-environments`
**Priority:** Medium
**Effort:** 4-6 hours

---

## 📋 Summary

**Total TODOs:** 10
- Security features: 2 items (Medium priority)
- REST API endpoints: 6 items (Low-Medium priority)
- Test placeholders: 2 items (No action needed - test stubs)

**Recommended Priority:**
1. Issue 6: Package update checking (High user value)
2. Issues 1-2: Security integration (Production hardening)
3. Issue 7: Dependency resolution (UX improvement)
4. Issue 8: Virtual environment listing (v2.12.0 follow-up)
5. Issues 3-5: Package management enhancements (Nice-to-have)

**Total Estimated Effort:** 32-48 hours for all items

---

## ✅ Action Plan

1. **Review and prioritize** these issues with stakeholders
2. **Create GitHub issues** for approved items
3. **Label appropriately** (enhancement, security, etc.)
4. **Assign milestones** (e.g., v2.16.0, v2.17.0)
5. **Update TODO comments** to reference GitHub issue numbers

**Example:**
```java
// TODO #45: Implement when Ignition context API is integrated
// See: https://github.com/user/repo/issues/45
```

---

**Note:** These TODOs are placeholders for future functionality and do NOT indicate bugs or incomplete features. The module is fully functional without these enhancements.
