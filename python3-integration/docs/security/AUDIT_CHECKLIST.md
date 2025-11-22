# Python 3 Integration - Security Audit Checklist

**Version:** v2.15.9
**Last Updated:** October 2025
**Purpose:** Monthly security audit and compliance verification

---

## Overview

This checklist should be completed **monthly** to ensure the Python 3 Integration module remains secure and compliant. Each section includes verification steps and expected results.

**Audit Duration:** 1-2 hours
**Frequency:** Monthly (minimum)
**Responsible:** Security Team / System Administrator

---

## Audit Information

**Audit Date:** ______________

**Auditor Name:** ______________

**Audit Period:** ______________ to ______________

**Environment:** ☐ Production  ☐ Staging  ☐ Development

**Gateway URL:** ______________

---

## 1. Authentication & Access Control

### 1.1 Admin API Key Security

- [ ] **Key Strength:** Admin key is 32+ characters
  ```bash
  # Check key length in ignition.conf
  grep "python3.admin.apikey" data/ignition.conf | \
    sed 's/.*apikey=//' | wc -c
  # Should be 65+ (64 chars + newline)
  ```

- [ ] **Key Storage:** Key not exposed in logs or public repositories
  ```bash
  # Search logs for API key
  grep -i "apikey" logs/wrapper.log
  # Should NOT show actual key value
  ```

- [ ] **Key Rotation:** Last rotation date < 90 days ago
  - **Last Rotation Date:** ______________
  - **Next Rotation Date:** ______________

- [ ] **Key Distribution:** Document which systems have API key
  - **System 1:** ______________
  - **System 2:** ______________
  - **System 3:** ______________

**Findings:**
_____________________________________________________________________________________

---

### 1.2 Designer Access Control

- [ ] **User Count:** Only authorized users have Designer access
  ```bash
  # Check in Gateway: Config → Security → Users
  # Count users with "Designer" role
  ```
  - **Total Designer Users:** ______________
  - **Expected Count:** ______________

- [ ] **Recent Access:** Review Designer logins in past 30 days
  - **Active Users:** ______________
  - **Inactive Users (>30 days):** ______________

- [ ] **Access Review:** All Designer users still require access
  - **Users Removed:** ______________

**Findings:**
_____________________________________________________________________________________

---

### 1.3 HTTPS Enforcement

- [ ] **HTTPS Enabled:** Gateway has valid SSL certificate
  ```bash
  # Test HTTPS
  curl -v https://localhost:8043/StatusPing 2>&1 | grep "SSL certificate"
  ```

- [ ] **Certificate Validity:** Certificate not expired
  ```bash
  # Check expiry
  echo | openssl s_client -connect localhost:8043 2>/dev/null | \
    openssl x509 -noout -dates
  ```
  - **Expires On:** ______________
  - **Days Remaining:** ______________

- [ ] **HTTPS Required:** `requirehttps=true` for ADMIN mode
  ```bash
  # Check configuration
  grep "requirehttps" data/ignition.conf
  # Should show "requirehttps=true" or not present (defaults to true)
  ```

- [ ] **HTTP Blocked:** Port 8088 not accessible externally
  ```bash
  # Test from external IP (should fail)
  curl http://<external-ip>:8088/StatusPing
  ```

**Findings:**
_____________________________________________________________________________________

---

## 2. Audit Log Review

### 2.1 Log Integrity

- [ ] **Logs Exist:** Audit logs present for past 30 days
  ```bash
  ls -lh data/python3-integration/audit/audit-*.log | wc -l
  # Should have ~30 files
  ```

- [ ] **Log Rotation:** Daily log files created
  ```bash
  # Check today's log exists
  ls data/python3-integration/audit/audit-$(date +%Y-%m-%d).log
  ```

- [ ] **Log Permissions:** Logs not world-readable
  ```bash
  ls -l data/python3-integration/audit/ | head -5
  # Should show -rw-r----- or similar (not -rw-rw-rw-)
  ```

- [ ] **Log Size:** Log files reasonable size (not filling disk)
  ```bash
  du -sh data/python3-integration/audit/
  ```
  - **Total Size:** ______________
  - **Average Daily Size:** ______________

**Findings:**
_____________________________________________________________________________________

---

### 2.2 Execution Analysis

- [ ] **Total Executions:** Count executions this month
  ```bash
  cat data/python3-integration/audit/audit-2025-10-*.log | wc -l
  ```
  - **Total Executions:** ______________
  - **Expected Range:** ______________

- [ ] **Success Rate:** Calculate success rate
  ```bash
  TOTAL=$(cat audit-2025-10-*.log | wc -l)
  SUCCESS=$(grep '"success":true' audit-2025-10-*.log | wc -l)
  echo "Success rate: $(($SUCCESS * 100 / $TOTAL))%"
  ```
  - **Success Rate:** ______________% (expect > 95%)

- [ ] **Error Spike:** No unexpected spike in errors
  ```bash
  # Count errors per day
  for file in audit-2025-10-*.log; do
    echo "$file: $(grep '"success":false' $file | wc -l)"
  done
  ```
  - **Max Errors/Day:** ______________
  - **Average Errors/Day:** ______________

**Findings:**
_____________________________________________________________________________________

---

### 2.3 Security Mode Usage

- [ ] **Mode Distribution:** Verify expected distribution
  ```bash
  echo "DESIGNER_ADMIN:"
  grep '"securityMode":"DESIGNER_ADMIN"' audit-*.log | wc -l
  echo "ADMIN:"
  grep '"securityMode":"ADMIN"' audit-*.log | wc -l
  echo "RESTRICTED:"
  grep '"securityMode":"RESTRICTED"' audit-*.log | wc -l
  ```
  - **DESIGNER_ADMIN:** ______________ (most internal users)
  - **ADMIN:** ______________ (API with key)
  - **RESTRICTED:** ______________ (public API)

- [ ] **Unexpected ADMIN Usage:** No unauthorized ADMIN mode usage
  ```bash
  # Review ADMIN mode executions
  grep '"securityMode":"ADMIN"' audit-*.log | \
    jq -r '.timestamp + " " + .sourceIP + " " + .endpoint'
  ```
  - **Known IPs:** ______________
  - **Unknown IPs:** ______________

**Findings:**
_____________________________________________________________________________________

---

### 2.4 User Activity Review

- [ ] **Top Users:** Identify most active users
  ```bash
  grep -o '"user":"[^"]*"' audit-*.log | \
    sort | uniq -c | sort -rn | head -10
  ```
  - **User 1:** ______________ executions
  - **User 2:** ______________ executions
  - **User 3:** ______________ executions

- [ ] **Unusual Activity:** No suspicious patterns
  - **Late Night Access (12am-6am):** ______________
  - **Weekend Access:** ______________
  - **Failed Logins:** ______________

- [ ] **Anonymous Access:** Review unauthenticated executions
  ```bash
  grep '"user":null' audit-*.log | wc -l
  ```
  - **Anonymous Executions:** ______________
  - **Expected:** ______________ (RESTRICTED mode only)

**Findings:**
_____________________________________________________________________________________

---

## 3. Code Validation & Security

### 3.1 Blocked Module Attempts

- [ ] **Always-Blocked Modules:** Check attempts to import dangerous modules
  ```bash
  # Search for blocked modules
  grep -E "(ctypes|multiprocessing|threading)" audit-*.log
  ```
  - **ctypes Attempts:** ______________
  - **multiprocessing Attempts:** ______________
  - **threading Attempts:** ______________

- [ ] **Bypass Attempts:** Check for security bypass attempts
  ```bash
  # Search for evasion techniques
  grep -E "(__import__|eval\(|exec\()" audit-*.log
  ```
  - **Dynamic Import Attempts:** ______________
  - **Eval/Exec Attempts:** ______________

- [ ] **RESTRICTED Mode Violations:** Blocked module access in RESTRICTED mode
  ```bash
  grep '"securityMode":"RESTRICTED"' audit-*.log | \
    grep '"success":false'
  ```
  - **Blocked Attempts:** ______________
  - **Common Modules:** ______________

**Findings:**
_____________________________________________________________________________________

---

### 3.2 AST Validation

- [ ] **Validation Errors:** Review AST validation failures
  ```bash
  grep "SECURITY ERROR" logs/wrapper.log | tail -20
  ```
  - **Validation Failures:** ______________
  - **Most Common Error:** ______________

- [ ] **False Positives:** No legitimate code blocked by mistake
  - **User Complaints:** ______________
  - **False Positives:** ______________

**Findings:**
_____________________________________________________________________________________

---

## 4. Performance & Resources

### 4.1 Process Pool Health

- [ ] **Pool Size:** Current pool size appropriate for load
  ```bash
  curl -s http://localhost:8088/data/python3integration/api/v1/pool-stats | jq
  ```
  - **Total Size:** ______________
  - **Healthy:** ______________
  - **Available:** ______________
  - **In Use:** ______________

- [ ] **Pool Saturation:** Check if pool frequently exhausted
  ```bash
  # Check for "Timeout waiting for executor" errors
  grep "Timeout waiting" logs/wrapper.log | wc -l
  ```
  - **Timeout Count:** ______________ (expect 0)

- [ ] **Unhealthy Processes:** Review process failures
  ```bash
  # Check for process restart events
  grep "Process .* is not alive" logs/wrapper.log | wc -l
  ```
  - **Process Restarts:** ______________ (expect < 5/month)

**Findings:**
_____________________________________________________________________________________

---

### 4.2 Resource Usage

- [ ] **Memory Usage:** Check Python process memory
  ```bash
  # On Gateway server
  ps aux | grep python3 | awk '{print $6}' | \
    awk '{sum+=$1} END {print "Total: " sum/1024 " MB"}'
  ```
  - **Total Memory:** ______________ MB
  - **Per Process:** ______________ MB (expect < 512MB)

- [ ] **CPU Usage:** Check CPU consumption
  ```bash
  curl -s http://localhost:8088/data/python3integration/api/v1/diagnostics | \
    jq '.cpuUsagePercent'
  ```
  - **CPU Usage:** ______________% (expect < 20%)

- [ ] **Disk Space:** Check audit log disk usage
  ```bash
  du -sh data/python3-integration/
  ```
  - **Total Size:** ______________
  - **Available Space:** ______________

**Findings:**
_____________________________________________________________________________________

---

### 4.3 Performance Metrics

- [ ] **Average Execution Time:** Calculate average from audit logs
  ```bash
  grep '"durationMs"' audit-*.log | \
    sed 's/.*"durationMs":\([0-9]*\).*/\1/' | \
    awk '{sum+=$1; count++} END {print "Average:", sum/count, "ms"}'
  ```
  - **Average Time:** ______________ ms (expect < 200ms)

- [ ] **Slow Executions:** Identify slowest executions
  ```bash
  grep '"durationMs"' audit-*.log | \
    sed 's/.*"durationMs":\([0-9]*\).*/\1/' | sort -rn | head -10
  ```
  - **Slowest Execution:** ______________ ms
  - **P95 Execution Time:** ______________ ms

- [ ] **Rate Limiting:** Check if rate limits triggered
  ```bash
  grep "Rate limit exceeded" logs/wrapper.log | wc -l
  ```
  - **Rate Limit Hits:** ______________ (expect 0)

**Findings:**
_____________________________________________________________________________________

---

## 5. Compliance & Governance

### 5.1 Data Retention

- [ ] **Log Retention:** Audit logs retained for required period
  - **Retention Policy:** ______________ days
  - **Oldest Log:** ______________
  - **Compliance:** ☐ SOC 2  ☐ NIST  ☐ ISO 27001  ☐ Other: ______________

- [ ] **Backup Status:** Recent backups exist
  ```bash
  ls -lh backups/python3-scripts-*.tar.gz | tail -5
  ```
  - **Latest Backup:** ______________
  - **Backup Age:** ______________ days (expect < 7)

**Findings:**
_____________________________________________________________________________________

---

### 5.2 Change Management

- [ ] **Configuration Changes:** Document any config changes this month
  - **Change 1:** ______________
  - **Change 2:** ______________
  - **Change 3:** ______________

- [ ] **Module Version:** Running latest stable version
  ```bash
  curl -s http://localhost:8088/data/python3integration/api/v1/health | \
    jq '.version'
  ```
  - **Current Version:** ______________
  - **Latest Version:** v2.15.9

- [ ] **Change Approval:** All changes properly approved
  - **Approval Process:** ☐ Followed  ☐ Bypassed  ☐ N/A

**Findings:**
_____________________________________________________________________________________

---

### 5.3 Incident Response

- [ ] **Security Incidents:** Review any security incidents
  - **Incidents This Month:** ______________
  - **Resolved:** ______________
  - **Pending:** ______________

- [ ] **Incident Documentation:** All incidents documented
  - **Documentation Complete:** ☐ Yes  ☐ No  ☐ N/A

- [ ] **Lessons Learned:** Corrective actions implemented
  - **Actions Taken:** ______________

**Findings:**
_____________________________________________________________________________________

---

## 6. Recommendations & Actions

### 6.1 Security Improvements

- [ ] **Recommended Actions:**
  1. ______________________________________________________________
  2. ______________________________________________________________
  3. ______________________________________________________________

### 6.2 Priority Issues

- [ ] **Critical Issues (fix immediately):**
  - ______________________________________________________________

- [ ] **High Priority (fix within 7 days):**
  - ______________________________________________________________

- [ ] **Medium Priority (fix within 30 days):**
  - ______________________________________________________________

- [ ] **Low Priority (monitor):**
  - ______________________________________________________________

---

## 7. Audit Summary

### 7.1 Overall Assessment

**Security Posture:** ☐ Excellent  ☐ Good  ☐ Needs Improvement  ☐ Critical Issues

**Compliance Status:** ☐ Compliant  ☐ Minor Issues  ☐ Non-Compliant

**Risk Level:** ☐ Low  ☐ Medium  ☐ High  ☐ Critical

### 7.2 Score Card

| Category | Score (1-5) | Notes |
|----------|-------------|-------|
| Authentication & Access Control | _____ | _________________ |
| Audit Logging | _____ | _________________ |
| Code Validation | _____ | _________________ |
| Performance | _____ | _________________ |
| Compliance | _____ | _________________ |
| **Overall Score** | **_____** | **(Average)** |

**Scoring Guide:**
- 5 = Excellent (no issues)
- 4 = Good (minor issues)
- 3 = Acceptable (some concerns)
- 2 = Needs Improvement (significant issues)
- 1 = Critical (immediate action required)

### 7.3 Executive Summary

**Key Findings:**
_____________________________________________________________________________________
_____________________________________________________________________________________
_____________________________________________________________________________________

**Recommendations:**
_____________________________________________________________________________________
_____________________________________________________________________________________
_____________________________________________________________________________________

**Next Audit Date:** ______________

---

## 8. Sign-Off

**Auditor Signature:** ______________

**Date:** ______________

**Reviewed By:** ______________

**Date:** ______________

**Actions Assigned To:** ______________

**Follow-up Date:** ______________

---

## Appendix: Useful Audit Commands

```bash
# Quick security check
./security-audit.sh

# Monthly execution summary
cat audit-2025-10-*.log | jq -s '
  {
    total: length,
    successful: [.[] | select(.success==true)] | length,
    failed: [.[] | select(.success==false)] | length,
    byMode: group_by(.securityMode) | map({mode: .[0].securityMode, count: length})
  }
'

# Top 10 most active users
grep -o '"user":"[^"]*"' audit-*.log | \
  sort | uniq -c | sort -rn | head -10

# Failed execution details
grep '"success":false' audit-*.log | \
  jq -r '.timestamp + " " + .user + " " + .error' | \
  tail -20

# Average execution time by security mode
for mode in DESIGNER_ADMIN ADMIN RESTRICTED; do
  echo "$mode:"
  grep "\"securityMode\":\"$mode\"" audit-*.log | \
    sed 's/.*"durationMs":\([0-9]*\).*/\1/' | \
    awk '{sum+=$1; count++} END {print "  Average:", sum/count, "ms"}'
done

# Check for unusual access times
grep -o '"timestamp":"[^"]*"' audit-*.log | \
  cut -d'T' -f2 | cut -d':' -f1 | sort | uniq -c | sort -rn

# Security error summary
grep "SECURITY ERROR" logs/wrapper.log | \
  sed 's/.*SECURITY ERROR: //' | sort | uniq -c | sort -rn
```

---

*This checklist was created for Python 3 Integration v2.15.9 - Last updated November 2025*
