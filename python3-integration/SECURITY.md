# Security Notice - Module Signing Certificates

## ⚠️ IMPORTANT: Development Certificates

This repository contains **development-only** self-signed certificates with **PUBLIC passwords**.

### Files Affected:
- `keystore.jks` - Private keystore (password: `***REDACTED***`)
- `certificate.der` - Public certificate
- `gaskony-cert.pem` - PEM format certificate
- `sign.props` - Signing configuration
- `gradle.properties` - Gradle signing configuration

### Why are these in version control?

**For Development & Testing Only:**
1. ✅ Reproducible builds across all environments
2. ✅ CI/CD pipelines work without extra configuration
3. ✅ Clone-and-build works immediately for contributors
4. ✅ Consistent module signatures for development testing

### Production Deployment - CRITICAL

**🚨 DO NOT USE THESE CERTIFICATES IN PRODUCTION! 🚨**

#### For Production Deployments:

1. **Generate New Certificates**:
   ```bash
   ./generate-signing-certs.sh
   ```

2. **Use Secure Passwords**:
   - Generate strong random passwords (32+ characters)
   - Example: `openssl rand -hex 32`

3. **Store Credentials Securely**:
   - Use CI/CD secrets (GitHub Secrets, GitLab CI/CD Variables)
   - Use environment variables
   - Use secrets managers (AWS Secrets Manager, HashiCorp Vault, etc.)

4. **NEVER Commit Production Credentials**:
   - Add `sign.props` to `.gitignore` for production builds
   - Add `gradle.properties` to `.gitignore` for production builds
   - Use template files instead (e.g., `sign.props.template`)

#### Example: Production CI/CD Setup

**GitHub Actions:**
```yaml
- name: Configure signing
  env:
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    CERT_PASSWORD: ${{ secrets.CERT_PASSWORD }}
  run: |
    echo "key.pass=$KEYSTORE_PASSWORD" > sign.props
    echo "cert.pass=$CERT_PASSWORD" >> sign.props
```

**GitLab CI:**
```yaml
before_script:
  - echo "key.pass=$KEYSTORE_PASSWORD" > sign.props
  - echo "cert.pass=$CERT_PASSWORD" >> sign.props
```

### Security Best Practices

1. **Rotate Certificates Regularly**
   - Generate new certificates every 12-24 months
   - Update all deployed modules with re-signed versions

2. **Restrict Access**
   - Private keys should only be accessible to build systems
   - Use role-based access control (RBAC) for secrets

3. **Audit Certificate Usage**
   - Log all certificate operations
   - Monitor for unauthorized certificate usage

4. **Separate Dev/Prod Certificates**
   - ALWAYS use different certificates for development vs. production
   - Never use the certificates from this repository in production

### Why This Approach?

This follows the **Ignition SDK best practices** for open-source development modules:
- Development certificates in version control ✅
- Production certificate generation scripts provided ✅
- Clear security warnings ✅
- Easy contributor onboarding ✅

### Questions?

See `CLAUDE.md` section "Certificate Management" for full rationale and technical details.

---

**Last Updated:** v2.9.0 (Security hardening release)
