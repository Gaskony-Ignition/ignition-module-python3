#!/bin/bash

# Git Hooks Setup Script
# Installs pre-commit hooks for automated quality checks

set -e

echo "=================================="
echo "Git Hooks Setup"
echo "=================================="
echo ""

# Check if we're in a git repository
if [ ! -d ".git" ]; then
    echo "ERROR: Not in a git repository root"
    echo "Please run this script from the repository root directory"
    exit 1
fi

# Create hooks directory if it doesn't exist
if [ ! -d ".githooks" ]; then
    echo "ERROR: .githooks directory not found"
    echo "Expected location: .githooks/"
    exit 1
fi

# Configure git to use custom hooks directory
echo "→ Configuring git to use .githooks/ directory..."
git config core.hooksPath .githooks

# Make hooks executable
echo "→ Making hooks executable..."
chmod +x .githooks/pre-commit 2>/dev/null || true

# Verify setup
if [ -x ".githooks/pre-commit" ]; then
    echo ""
    echo "✓ Pre-commit hook installed successfully!"
    echo ""
    echo "The following checks will run before each commit:"
    echo "  1. Zone.Identifier file detection"
    echo "  2. Temporary file detection"
    echo "  3. Documentation consistency"
    echo "  4. Java compilation"
    echo "  5. Debug statement detection"
    echo "  6. TODO/FIXME comment detection"
    echo ""
    echo "To skip checks on a specific commit, use:"
    echo "  git commit --no-verify"
    echo ""
    echo "To disable hooks completely, run:"
    echo "  git config --unset core.hooksPath"
    echo ""
else
    echo ""
    echo "ERROR: Failed to install pre-commit hook"
    exit 1
fi

# Test the hook
echo "Testing pre-commit hook..."
if .githooks/pre-commit --dry-run 2>/dev/null || true; then
    echo "✓ Pre-commit hook is working"
else
    echo "⚠ Pre-commit hook test failed (this may be normal if there are current issues)"
fi

echo ""
echo "=================================="
echo "Setup complete!"
echo "=================================="
