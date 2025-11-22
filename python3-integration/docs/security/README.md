# Security Documentation

**Module:** Python 3 Integration for Ignition 8.3+
**Version:** v2.15.10
**Last Updated:** 2025-11-21

Complete security documentation for the Python 3 Integration module.

---

## 📖 Security Documents

### 1. [SECURITY_OVERVIEW.md](./SECURITY_OVERVIEW.md) ⭐ START HERE
**Audience:** All users (developers, administrators, security teams)
**Purpose:** Security architecture, security modes, and best practices
**Topics:**
- Three-tier security model (DESIGNER_ADMIN, ADMIN, RESTRICTED)
- Four-layer defense architecture
- Security modes and allowed modules
- Threat model and risk mitigation

**Read this first** to understand the module's security design.

---

### 2. [SECURITY_CONFIG.md](./SECURITY_CONFIG.md)
**Audience:** System administrators, DevOps engineers
**Purpose:** Security configuration and deployment guide
**Topics:**
- Production deployment configuration
- Security best practices
- Firewall and network configuration
- Monitoring and logging setup
- Environment-specific security settings

**Read this** when deploying to production environments.

---

### 3. [AUDIT_CHECKLIST.md](./AUDIT_CHECKLIST.md)
**Audience:** Security auditors, compliance teams
**Purpose:** Security audit and compliance checklist
**Topics:**
- Pre-deployment security checklist
- Ongoing security monitoring
- Incident response procedures
- Compliance requirements
- Security testing procedures

**Use this** for security audits and compliance reviews.

---

## 🔒 Other Security Resources

### Repository-Level Security
- **[/SECURITY.md](../../../../SECURITY.md)** - GitHub security policy (version support, vulnerability reporting)
- **[/python3-integration/SECURITY.md](../../SECURITY.md)** - Development certificate security notice

### External Resources
- **Ignition Security**: https://docs.inductiveautomation.com/docs/8.1/platform/security/security
- **OWASP Top 10**: https://owasp.org/www-project-top-ten/

---

## 🚨 Quick Reference

### Reporting Security Issues
See [/SECURITY.md](../../../../SECURITY.md) for vulnerability reporting procedures.

### Security Modes Summary

| Mode | Users | Access Level | Use Case |
|------|-------|--------------|----------|
| **DESIGNER_ADMIN** | Designer IDE users | Full Python capabilities | Interactive development |
| **ADMIN** | API users with key | Extended capabilities | Automation scripts |
| **RESTRICTED** | Unauthenticated API | Safe modules only | Public endpoints |

### Default Security Settings
- Memory limit: 512MB per process
- CPU timeout: 60 seconds
- Process pool size: 3-20 processes
- Code size limit: 1MB
- All actions logged for audit

---

## 📊 Document Status

| Document | Lines | Status | Last Updated |
|----------|-------|--------|--------------|
| SECURITY_OVERVIEW.md | 536 | ✅ Current | v2.15.10 |
| SECURITY_CONFIG.md | 798 | ✅ Current | v2.15.10 |
| AUDIT_CHECKLIST.md | 580 | ✅ Current | v2.15.10 |

---

**Need help?** See [TROUBLESHOOTING.md](../operations/TROUBLESHOOTING.md) or open an issue on GitHub.
