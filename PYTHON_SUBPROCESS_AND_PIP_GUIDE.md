# Python Subprocess and Pip Usage Guide

**For Python 3 IDE in Ignition Designer**
**Version:** 2.4.2+
**Last Updated:** 2025-10-18

---

## Overview

The Python 3 IDE executes code in **ADMIN security mode** (as of v2.0.21), which allows full Python access including:
- `subprocess` module
- `os` module
- `sys` module
- File I/O operations
- Network operations
- And all other admin-level modules

This guide explains how to use these capabilities correctly from the IDE.

---

## Quick Answer: "AC: Ready" Means Jedi is Already Installed

If your IDE status bar shows **"AC: Ready"**, Jedi autocomplete is already installed and working. You don't need to install it manually.

Jedi auto-installs at Gateway startup via `GatewayHook.java`:
```java
// Auto-install Jedi for IDE autocomplete (v2.3.1)
if (!packageManager.isInstalled("jedi")) {
    LOGGER.info("Jedi not installed - installing automatically...");
    packageManager.installPackage("jedi");
}
```

Check Gateway logs to confirm:
```bash
tail -f <ignition-install>/logs/wrapper.log | grep -i jedi
```

---

## Running Shell Commands from the IDE

### ❌ WRONG: Typing shell commands directly

```python
# This will FAIL - not Python code
pip install jedi
```

**Error:** `NameError: name 'pip' is not defined`

### ✅ CORRECT: Using subprocess module

```python
import subprocess
import sys

# Method 1: Run pip as a module (recommended)
result = subprocess.run(
    [sys.executable, '-m', 'pip', 'install', 'requests'],
    capture_output=True,
    text=True
)

print("STDOUT:", result.stdout)
print("STDERR:", result.stderr)
print("Return code:", result.returncode)
print("Success:", result.returncode == 0)
```

```python
# Method 2: Run shell command
result = subprocess.run(
    ['pip', 'install', 'requests'],
    capture_output=True,
    text=True,
    shell=False  # Safer than shell=True
)

print("Output:", result.stdout)
if result.returncode != 0:
    print("Error:", result.stderr)
```

```python
# Method 3: Check installed packages
result = subprocess.run(
    [sys.executable, '-m', 'pip', 'list'],
    capture_output=True,
    text=True
)

print(result.stdout)
```

---

## Understanding the Execution Environment

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│  Ignition Designer (Client)                             │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Python 3 IDE                                      │ │
│  │  - Code Editor (Java/Swing)                        │ │
│  │  - Sends code via REST API →                       │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          ↓ HTTP POST
┌─────────────────────────────────────────────────────────┐
│  Ignition Gateway (Server)                              │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Python3ProcessPool                                │ │
│  │  ┌──────────────┐  ┌──────────────┐              │ │
│  │  │ Process #1   │  │ Process #2   │  ...          │ │
│  │  │ python_bridge│  │ python_bridge│              │ │
│  │  │ (ephemeral)  │  │ (ephemeral)  │              │ │
│  │  └──────────────┘  └──────────────┘              │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

### Key Points:

1. **IDE code executes in Gateway subprocesses** (not Designer JVM)
2. **Subprocesses are ephemeral** (recycled after use)
3. **Each subprocess is isolated** (own globals, imports)
4. **ADMIN mode enabled** (full Python access as of v2.0.21)

---

## Package Installation Best Practices

### ❌ DON'T: Install packages from IDE subprocess

```python
# This will "work" but installations won't persist
import subprocess
import sys

result = subprocess.run(
    [sys.executable, '-m', 'pip', 'install', 'pandas'],
    capture_output=True,
    text=True
)
# Package installed to subprocess, but will be gone after process recycle
```

**Problems:**
- Installations are ephemeral (lost when subprocess recycled)
- May not have write permissions
- Wastes time and bandwidth on repeated installs
- Not visible to other pool processes

### ✅ DO: Install packages at Gateway host level

**Method 1: SSH to Gateway host**
```bash
ssh gateway-host

# Find Python executable path
which python3
# or check Gateway logs for path

# Install package globally
/path/to/python3 -m pip install pandas

# Verify installation
/path/to/python3 -m pip list | grep pandas
```

**Method 2: Use Gateway's Python environment**
```bash
# On Gateway host
cd <ignition-install>/bin

# Gateway uses system Python (configured in GatewayHook)
python3 -m pip install pandas
```

**Method 3: Bundle with module (for distribution)**

See `python3-integration/gateway/src/main/resources/bundled-wheels/` for pre-bundled packages:
- Jedi (autocomplete)
- Additional packages can be added here

---

## Common Use Cases

### 1. Check Python Environment

```python
import sys
import subprocess

# Python version
print("Python version:", sys.version)
print("Executable:", sys.executable)
print("Platform:", sys.platform)

# Installed packages
result = subprocess.run(
    [sys.executable, '-m', 'pip', 'list'],
    capture_output=True,
    text=True
)
print("Installed packages:")
print(result.stdout)
```

### 2. Verify Package Installation

```python
import subprocess
import sys

def check_package(package_name):
    """Check if a package is installed"""
    result = subprocess.run(
        [sys.executable, '-m', 'pip', 'show', package_name],
        capture_output=True,
        text=True
    )

    if result.returncode == 0:
        print(f"✓ {package_name} is installed")
        print(result.stdout)
    else:
        print(f"✗ {package_name} is NOT installed")

    return result.returncode == 0

# Check multiple packages
packages = ['jedi', 'requests', 'pandas', 'numpy']
for pkg in packages:
    check_package(pkg)
```

### 3. Run System Commands

```python
import subprocess

# List files in a directory
result = subprocess.run(
    ['ls', '-lah', '/tmp'],
    capture_output=True,
    text=True
)
print(result.stdout)

# Check disk usage
result = subprocess.run(
    ['df', '-h'],
    capture_output=True,
    text=True
)
print(result.stdout)

# Network diagnostics
result = subprocess.run(
    ['ping', '-c', '3', 'google.com'],
    capture_output=True,
    text=True
)
print(result.stdout)
```

### 4. File Operations

```python
import os
import subprocess

# Read environment variables
print("Gateway user:", os.environ.get('USER'))
print("Home directory:", os.environ.get('HOME'))
print("PATH:", os.environ.get('PATH'))

# Create temporary file
import tempfile
with tempfile.NamedTemporaryFile(mode='w', delete=False, suffix='.txt') as f:
    f.write("Hello from Python 3 IDE!")
    temp_path = f.name
    print(f"Created temp file: {temp_path}")

# Read it back
with open(temp_path, 'r') as f:
    print("File contents:", f.read())

# Clean up
os.remove(temp_path)
print("Temp file deleted")
```

---

## Security Considerations

### ADMIN Mode Capabilities (v2.0.21+)

The IDE runs in **ADMIN mode** by default, allowing:

**Allowed modules:**
- `subprocess` - Run shell commands
- `os` - File system operations
- `sys` - Python runtime access
- `socket` - Network operations
- `urllib`, `requests` - HTTP requests
- `pathlib`, `shutil` - Path manipulation
- `sqlite3`, `csv` - Data access
- `pandas`, `numpy` - Data science (if installed)

**Always blocked modules** (even in ADMIN mode):
- `ctypes` - Direct memory access
- `multiprocessing` - Process forking
- `threading` - Thread creation
- `telnetlib`, `paramiko` - Remote access
- `pty`, `tty` - Terminal control

### Network and Gateway Security

Remember that IDE executes on the **Gateway** (server), not the Designer (client):

1. **File operations** access Gateway host filesystem
2. **Network operations** originate from Gateway IP
3. **Shell commands** run as Gateway process user
4. **API calls** come from Gateway network

**Best practices:**
- Avoid hardcoded credentials in scripts
- Use Gateway's network access controls
- Be cautious with file operations
- Test destructive operations in dev environment first

---

## Troubleshooting

### "execution failed" when running pip

**Symptom:** Trying to run `pip install package` results in "execution failed"

**Diagnosis:**
1. Check if you typed it as shell command (wrong)
2. Check if you used `subprocess.run()` (correct)
3. Check stderr output for actual error
4. Verify pip is available in Gateway's Python

**Solution:**
```python
import subprocess
import sys

# Correct way to run pip
result = subprocess.run(
    [sys.executable, '-m', 'pip', 'install', 'package-name'],
    capture_output=True,
    text=True
)

# Check what actually failed
if result.returncode != 0:
    print("STDERR:", result.stderr)
    print("STDOUT:", result.stdout)
    print("Return code:", result.returncode)
```

### "Module not found" after pip install from IDE

**Symptom:** Installed package via IDE subprocess, but still get ImportError

**Cause:** Package installed to ephemeral subprocess, already recycled

**Solution:** Install at Gateway host level (see "Package Installation Best Practices")

### Permission denied errors

**Symptom:** `Permission denied` when writing files or installing packages

**Cause:** Gateway process doesn't have write permissions to target directory

**Solution:**
1. Check Gateway process user: `ps aux | grep ignition`
2. Grant permissions: `sudo chown -R ignition-user /target/directory`
3. Or use directories Gateway user owns (e.g., `/tmp`, `<ignition>/data/`)

---

## Examples for Common Tasks

### Example 1: Download file from URL

```python
import subprocess
import sys

# Using urllib (built-in)
import urllib.request

url = "https://example.com/data.json"
local_file = "/tmp/data.json"

urllib.request.urlretrieve(url, local_file)
print(f"Downloaded to: {local_file}")

# Read and parse
import json
with open(local_file, 'r') as f:
    data = json.load(f)
    print(data)
```

### Example 2: Query database

```python
import sqlite3

# Create in-memory database
conn = sqlite3.connect(':memory:')
cursor = conn.cursor()

# Create table
cursor.execute('''
    CREATE TABLE users (
        id INTEGER PRIMARY KEY,
        name TEXT,
        email TEXT
    )
''')

# Insert data
cursor.execute("INSERT INTO users (name, email) VALUES (?, ?)",
               ("Alice", "alice@example.com"))
cursor.execute("INSERT INTO users (name, email) VALUES (?, ?)",
               ("Bob", "bob@example.com"))
conn.commit()

# Query
cursor.execute("SELECT * FROM users")
rows = cursor.fetchall()

for row in rows:
    print(f"ID: {row[0]}, Name: {row[1]}, Email: {row[2]}")

conn.close()
```

### Example 3: Generate report

```python
import subprocess
import sys
from datetime import datetime
import json

# Gather system info
info = {
    'timestamp': datetime.now().isoformat(),
    'python_version': sys.version,
    'platform': sys.platform,
}

# Get disk usage
result = subprocess.run(['df', '-h'], capture_output=True, text=True)
info['disk_usage'] = result.stdout

# Get memory usage
result = subprocess.run(['free', '-h'], capture_output=True, text=True)
info['memory_usage'] = result.stdout

# Save report
report_path = '/tmp/system_report.json'
with open(report_path, 'w') as f:
    json.dump(info, f, indent=2)

print(f"Report saved to: {report_path}")
print(json.dumps(info, indent=2))
```

---

## Related Documentation

- **Gateway Package Management:** `python3-integration/gateway/.../Python3PackageManager.java`
- **Security Modes:** `python3-integration/gateway/resources/python_bridge.py` (lines 88-142)
- **REST API:** `python3-integration/gateway/.../Python3RestEndpoints.java` (line 174-179)
- **IDE Architecture:** `python3-integration/docs/V2_ARCHITECTURE_GUIDE.md`

---

## Version History

- **v1.0** (2025-10-18): Initial guide based on v2.4.2 architecture

---

**Questions?**
- Check Gateway logs: `<ignition>/logs/wrapper.log`
- Review security modes in `python_bridge.py`
- Test in dev environment first
