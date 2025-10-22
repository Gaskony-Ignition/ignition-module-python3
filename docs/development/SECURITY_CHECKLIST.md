# Security Assessment Checklist

Run appropriate level based on workflow checkpoint.

## ⚡ Quick Scan (Every Commit)

### Input Validation
- [ ] All user inputs validated/sanitized
- [ ] SQL queries use parameterization
- [ ] File uploads restricted by type/size
- [ ] API inputs have schema validation

### Authentication/Authorization  
- [ ] No hardcoded credentials in code
- [ ] API keys in environment variables
- [ ] Sensitive operations require auth
- [ ] Proper session management

### Data Protection
- [ ] No sensitive data in logs
- [ ] PII data encrypted/hashed
- [ ] Secure communication (HTTPS)
- [ ] Safe error messages (no stack traces)

### Quick Commands
```bash
# Check for common issues
grep -r "password\|secret\|api_key" --exclude-dir=.env
grep -r "eval(\|exec(" .
grep -r "TODO\|FIXME\|HACK" . | grep -i security
```

---

## 🔍 Full Review (Weekly)

### Dependency Security
- [ ] Run dependency vulnerability scan
- [ ] Check for outdated packages
- [ ] Review new dependencies added
- [ ] Verify package sources

```bash
# Language-specific commands
npm audit                # Node.js
pip-audit               # Python  
bundle audit            # Ruby
cargo audit             # Rust
./gradlew dependencyCheckAnalyze # Java/Gradle
```

### Code Security Patterns
- [ ] No use of dangerous functions (eval, exec)
- [ ] Proper random number generation
- [ ] Secure file operations
- [ ] Safe deserialization
- [ ] No race conditions
- [ ] Proper error handling

### Access Control
- [ ] Principle of least privilege applied
- [ ] Role-based access implemented correctly
- [ ] API rate limiting in place
- [ ] Proper CORS configuration

### Infrastructure Security
- [ ] Secrets management system used
- [ ] Logging doesn't expose sensitive data
- [ ] Backups encrypted
- [ ] Development/production separation

---

## 🚨 Security Incident Response

If vulnerability discovered:
1. **STOP** current development
2. **ASSESS** severity (Critical/High/Medium/Low)
3. **FIX** immediately if Critical/High
4. **DOCUMENT** in CHANGELOG.md
5. **TEST** fix thoroughly
6. **DEPLOY** patch release

---

## 🔐 Security Best Practices Reference

### Never Do
- Store passwords in plain text
- Use MD5/SHA1 for password hashing  
- Trust user input
- Use predictable tokens/IDs
- Log sensitive information
- Commit .env files

### Always Do
- Use bcrypt/argon2 for passwords
- Parameterize database queries
- Validate on server side
- Use cryptographically secure random
- Encrypt data in transit and at rest
- Regular security updates

---

## 📊 Security Debt Tracking

### Current Security Debt
| Issue | Severity | Date Found | Status |
|-------|----------|------------|---------|
| Example | Medium | 2024-01-01 | Pending |

### Security Improvements Made
| Improvement | Date | Version |
|-------------|------|---------|
| Added input validation | 2024-01-01 | v1.2.0 |

---

Last Security Audit: [DATE]
Next Required Audit: [DATE]
Security Contact: [EMAIL/SLACK]
