# Refactoring Triggers & Guidelines

This document defines when and how to refactor code for maintainability.

## 🚦 Refactoring Triggers

### Immediate Refactoring (RED FLAGS)
Refactor before next commit if:

- [ ] Function exceeds 50 lines
- [ ] Cyclomatic complexity > 15
- [ ] Duplicate code appears 3+ times
- [ ] Nesting depth > 4 levels
- [ ] File exceeds 500 lines
- [ ] Class has > 10 methods
- [ ] Method has > 5 parameters

### Scheduled Refactoring (YELLOW FLAGS)
Add to backlog, refactor within week:

- [ ] Function 30-50 lines
- [ ] Cyclomatic complexity 10-15
- [ ] Duplicate code appears twice
- [ ] Nesting depth = 3 levels
- [ ] File 300-500 lines
- [ ] Class has 7-10 methods
- [ ] Method has 4-5 parameters

---

## 📏 Complexity Metrics

### How to Measure
```bash
# Python
radon cc . -s -a              # Cyclomatic complexity
radon mi . -s                 # Maintainability index

# JavaScript  
npx complexity-report src/    # Complexity analysis

# Java
# Use IDE metrics or SonarQube

# General
cloc . --by-file             # Lines of code by file
```

### Target Metrics
| Metric | Good | Acceptable | Refactor |
|--------|------|------------|-----------|
| Cyclomatic Complexity | <5 | 5-10 | >10 |
| Maintainability Index | >20 | 10-20 | <10 |
| Test Coverage | >80% | 60-80% | <60% |
| Documentation Coverage | >90% | 70-90% | <70% |

---

## 🔨 Refactoring Patterns

### Extract Method
**When:** Code block has clear purpose
```python
# Before
def process_user(user):
    # Validate user
    if not user.email or '@' not in user.email:
        raise ValueError('Invalid email')
    if len(user.password) < 8:
        raise ValueError('Password too short')
    # ... more code

# After  
def process_user(user):
    validate_user(user)
    # ... more code

def validate_user(user):
    validate_email(user.email)
    validate_password(user.password)
```

### Replace Conditional with Polymorphism
**When:** Multiple if/elif checking type
```python
# Before
if user_type == 'admin':
    permissions = ALL_PERMISSIONS
elif user_type == 'user':
    permissions = USER_PERMISSIONS
elif user_type == 'guest':
    permissions = GUEST_PERMISSIONS

# After
permissions = USER_TYPES[user_type].permissions
```

### Extract Class
**When:** Group of related methods/data
```python
# Before
def calculate_total(items, tax_rate, discount):
    subtotal = sum(item.price for item in items)
    tax = subtotal * tax_rate
    total = subtotal + tax - discount
    return total

# After
class OrderCalculator:
    def __init__(self, tax_rate):
        self.tax_rate = tax_rate
    
    def calculate_total(self, items, discount=0):
        # ...
```

---

## 📋 Refactoring Checklist

### Before Refactoring
- [ ] Tests exist and pass
- [ ] Commit current work
- [ ] Understand the code's purpose
- [ ] Identify improvement goal

### During Refactoring  
- [ ] Make one change at a time
- [ ] Run tests after each change
- [ ] Keep refactoring separate from features
- [ ] Update documentation as you go

### After Refactoring
- [ ] All tests still pass
- [ ] Code is clearer
- [ ] Performance unchanged/improved
- [ ] Documentation updated
- [ ] Commit with clear message

---

## 📊 Technical Debt Log

### Current Technical Debt
| Item | Type | Priority | Effort | Added |
|------|------|----------|--------|--------|
| UserManager class too large | Complexity | High | 4h | 2024-01-01 |
| Duplicate validation logic | Duplication | Medium | 2h | 2024-01-02 |

### Refactoring Completed
| Item | Type | Version | Date |
|------|------|---------|------|
| Split authentication module | Complexity | v2.1.0 | 2024-01-15 |

---

## 🎯 Refactoring Goals

### Short-term (This Week)
- [ ] Reduce average complexity below 10
- [ ] Eliminate critical code smells
- [ ] Improve test coverage to 80%

### Long-term (This Month)
- [ ] Achieve 90% documentation coverage
- [ ] Refactor legacy modules
- [ ] Implement design patterns where appropriate

---

Last Refactoring Session: [DATE]
Next Scheduled Session: [DATE]
Technical Debt Budget: [HOURS/SPRINT]
