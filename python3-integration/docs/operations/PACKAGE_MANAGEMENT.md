# Package Management Guide

**Module:** Python 3 Integration for Ignition 8.3+
**Version:** v2.15.9
**Last Updated:** 2025-11-21

Complete guide for installing and managing Python packages with the module.

---

## Overview

The module supports multiple methods for installing Python packages:
1. **Designer IDE** - Shell Command Mode (recommended)
2. **REST API** - For automation
3. **Bundled packages** - For air-gapped deployments

---

## Method 1: Designer IDE (Shell Command Mode)

**Recommended for interactive use**

### Open Shell Command Mode

1. Open Designer
2. Navigate to **Tools → Python 3 IDE**
3. Connect to Gateway
4. Select **"Shell Command"** tab at top

### Install Packages

```bash
# Install single package
pip install requests

# Install specific version
pip install pandas==2.0.0

# Install multiple packages
pip install numpy pandas matplotlib

# Install from requirements file
pip install -r requirements.txt

# Upgrade package
pip install --upgrade requests
```

### Uninstall Packages

```bash
# Uninstall single package
pip uninstall requests

# Uninstall multiple packages
pip uninstall numpy pandas
```

### List Installed Packages

```bash
# List all packages
pip list

# Show package details
pip show requests

# Check outdated packages
pip list --outdated
```

---

## Method 2: Packages Dialog (GUI)

**Visual package management** (Added in v2.15.0)

### Search PyPI

1. Open **Tools → Python 3 IDE → Packages** (button in toolbar)
2. Enter package name in "Search PyPI" field
3. Click **Search**
4. View package details (version, description, homepage)

### Install from PyPI

1. Enter package name in "Install from PyPI" field
2. (Optional) Specify version: `package==1.2.3`
3. Click **Install**
4. Monitor output panel for progress

### View Installed Packages

- Table shows all installed packages
- Columns: Name, Version, Location
- Auto-refreshes after install/uninstall

### Uninstall Packages

1. Select package in installed packages table
2. Right-click → **Uninstall**
3. Confirm uninstallation

---

## Method 3: REST API

**For automation and scripting**

### Install Package

```bash
curl -X POST \
  http://localhost:8088/data/python3integration/api/v1/packages/install \
  -H "Content-Type: application/json" \
  -d '{"package": "requests", "version": "2.31.0"}'
```

### List Packages

```bash
curl http://localhost:8088/data/python3integration/api/v1/packages/list
```

### Search PyPI

```bash
curl -X POST \
  http://localhost:8088/data/python3integration/api/v1/packages/search \
  -H "Content-Type: application/json" \
  -d '{"query": "requests"}'
```

---

## Common Packages

### Web & API

```bash
# HTTP requests
pip install requests

# Web scraping
pip install beautifulsoup4 lxml

# JSON/XML processing
pip install xmltodict
```

### Data Science

```bash
# Core data science stack
pip install numpy pandas matplotlib

# Scientific computing
pip install scipy scikit-learn

# Jupyter notebooks
pip install jupyter notebook
```

### Database

```bash
# PostgreSQL
pip install psycopg2-binary

# MySQL
pip install mysql-connector-python

# SQLite (built-in, no install needed)
import sqlite3

# SQLAlchemy ORM
pip install sqlalchemy
```

### Utilities

```bash
# Date/time handling
pip install python-dateutil pytz

# File operations
pip install pathlib2

# Configuration
pip install python-dotenv pyyaml
```

---

## Package Installation Locations

### With Virtual Environment

Packages install to venv:
```
/path/to/venv/lib/python3.X/site-packages/
```

### Without Virtual Environment

Packages install to bundled Python:
```
<gateway-data>/python3-integration/python/lib/python3.X/site-packages/
```

Check location:
```bash
python -c "import site; print(site.getsitepackages())"
```

---

## Requirements Files

### Create requirements.txt

**Export current packages**:
```bash
pip freeze > requirements.txt
```

**Example requirements.txt**:
```
requests==2.31.0
pandas==2.0.3
numpy==1.24.3
matplotlib==3.7.2
```

### Install from requirements.txt

```bash
pip install -r requirements.txt
```

### Version Pinning

```bash
# Exact version
requests==2.31.0

# Minimum version
requests>=2.30.0

# Compatible version
requests~=2.31.0  # >=2.31.0, <2.32.0

# Any version
requests
```

---

## Air-Gapped Deployments

See [AIR_GAPPED_DEPLOYMENT.md](AIR_GAPPED_DEPLOYMENT.md) for bundling packages into the module.

---

## Troubleshooting

### Installation Fails

**externally-managed-environment error**:
```bash
# Use --break-system-packages flag
pip install requests --break-system-packages
```

**Permission denied**:
- Check Gateway user has write access to site-packages
- On Linux, may need to run Gateway as different user
- Consider using virtual environment

**Package not found on PyPI**:
- Check spelling and capitalization
- Search PyPI website: https://pypi.org/
- Verify package name (sometimes different from import name)

### Import Errors After Installation

**ModuleNotFoundError: No module named 'package'**:
- Verify package installed: `pip list`
- Check Python path: `sys.path`
- Restart Gateway to reload sys.path
- Check virtual environment is active

**Version conflicts**:
- Check installed versions: `pip list`
- Upgrade conflicting packages
- Use version ranges in requirements.txt

### Slow Installation

**Large packages (numpy, pandas, scipy)**:
- Progress shown in output panel
- May take 1-5 minutes depending on package size
- Check Gateway CPU usage during install
- Consider pre-compiled wheels for your platform

---

## Best Practices

1. **Use requirements.txt** - Track dependencies in version control
2. **Pin versions** - Ensure reproducible deployments
3. **Test before production** - Verify packages work in test environment
4. **Use virtual environments** - Avoid dependency conflicts
5. **Regular updates** - Keep packages updated for security fixes
6. **Minimize dependencies** - Only install what you need
7. **Review security** - Check packages for known vulnerabilities

---

## Package Security

### Check Package Safety

```bash
# Install safety checker
pip install safety

# Scan for known vulnerabilities
safety check
```

### Verify Package Integrity

```bash
# Check package hash
pip hash requests

# Verify signature (if available)
pip verify requests
```

### Best Practices

- Only install from trusted sources (PyPI)
- Review package maintainers and download stats
- Check package homepage and documentation
- Scan for vulnerabilities before production use
- Keep packages updated for security patches

---

## Common Workflows

### Workflow 1: Add New Package

```bash
# 1. Install package
pip install requests

# 2. Test in script
import requests
response = requests.get('https://api.example.com')

# 3. Update requirements.txt
pip freeze > requirements.txt

# 4. Commit to version control
git add requirements.txt
git commit -m "Add requests package"
```

### Workflow 2: Update All Packages

```bash
# 1. List outdated packages
pip list --outdated

# 2. Update specific package
pip install --upgrade requests

# 3. Or update all (careful!)
pip list --outdated | cut -d ' ' -f1 | xargs pip install -U

# 4. Update requirements.txt
pip freeze > requirements.txt
```

### Workflow 3: Clean Install

```bash
# 1. Export current packages
pip freeze > requirements-backup.txt

# 2. Uninstall all packages
pip freeze | xargs pip uninstall -y

# 3. Install from requirements
pip install -r requirements.txt

# 4. Verify
pip list
```

---

## Integration with Ignition

### Using Packages in Scripts

```python
# In Ignition Script Console or Designer IDE
import requests

# Make API call
response = requests.get('https://api.example.com/data')
data = response.json()

# Process data
for item in data:
    print(item['name'])
```

### Using Packages in Gateway Events

```python
# Gateway Event Script
import pandas as pd

# Read CSV from Gateway
df = pd.read_csv('/path/to/data.csv')

# Process data
summary = df.describe()
print(summary)
```

---

**Need help?** See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) or open an issue on GitHub.
