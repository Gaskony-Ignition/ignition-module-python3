# Development Workflow & Checkpoints

This document defines our development workflow with regular assessment points for security, refactoring, and documentation.

## 🔄 Development Cycle Checkpoints

### Every Feature/Fix (Before Commit)
- [ ] Run security quick-check (see SECURITY_CHECKLIST.md - Quick Scan)
- [ ] Check refactor triggers (see REFACTOR_TRIGGERS.md)
- [ ] Update relevant documentation
- [ ] Increment version appropriately (see VERSION.md)

### Daily (End of Session)
- [ ] Review today's changes for security implications
- [ ] Identify technical debt introduced
- [ ] Update CHANGELOG.md with work completed
- [ ] Commit work-in-progress with clear status

### Weekly Sprint Review
- [ ] Full security assessment (SECURITY_CHECKLIST.md - Full Review)
- [ ] Refactoring assessment (complexity metrics)
- [ ] Documentation completeness check
- [ ] Version release decision
- [ ] Update ARCHITECTURE.md if design changed

### Monthly Deep Dive
- [ ] Dependency security audit
- [ ] Code complexity analysis
- [ ] Documentation audit
- [ ] Performance profiling
- [ ] Technical debt prioritization

---

## 📊 Workflow States

### 🟢 GREEN LIGHT - Continue Development
- Security checks pass
- Code complexity acceptable (<10 cyclomatic complexity)
- Documentation current
- Tests passing

### 🟡 YELLOW FLAG - Proceed with Caution
- Minor security warnings (low severity)
- Complexity increasing (10-15)
- Documentation slightly behind
- Some tests skipped

### 🔴 RED ALERT - Stop and Fix
- Security vulnerabilities found
- Complexity too high (>15)
- Documentation significantly outdated
- Tests failing

---

## 🎯 Checkpoint Commands

When you see these triggers, run the associated workflow:

### Trigger: "Let's implement [feature]"
```
1. Review SECURITY_CHECKLIST.md for relevant items
2. Check VERSION.md for versioning strategy
3. Create feature branch if using git-flow
4. Document approach in ARCHITECTURE.md if significant
```

### Trigger: "Fixed the bug"
```
1. Run security quick-check
2. Check if fix introduces complexity
3. Update CHANGELOG.md
4. Increment patch version
5. Add regression test
```

### Trigger: "Ready to commit"
```
1. Run: /workflow-precommit
2. Address any RED or YELLOW flags
3. Update documentation
4. Create comprehensive commit message
```

### Trigger: "End of session" or "That's all for today"
```
1. Run: /workflow-daily
2. Commit WIP with clear state
3. Note any pending security/refactor items
4. Update tomorrow's priorities
```

---

## 📋 Integration with Claude

Claude should:
1. **Proactively suggest checkpoints** when detecting triggers
2. **Reference this workflow** when making decisions
3. **Alert on RED flags** before proceeding
4. **Track accumulating technical debt** across sessions
5. **Suggest when to run specific assessments**

---

## 🔄 Workflow Metrics to Track

### Security Health Score
- Days since last security review: ___
- Open security issues: ___
- Dependencies needing updates: ___

### Code Quality Score  
- Average complexity: ___
- Test coverage: ___
- Documentation coverage: ___

### Technical Debt Score
- Refactoring backlog items: ___
- TODO/FIXME comments: ___
- Deprecated code still in use: ___

---

Last Review: [DATE]
Next Scheduled Review: [DATE]
