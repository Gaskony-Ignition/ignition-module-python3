#!/bin/bash

# VNC Test Harness Shutdown Script
# This script stops all VNC-related processes

echo "=== Stopping VNC Test Harness ==="

echo "Stopping websockify..."
killall websockify 2>/dev/null || echo "  (not running)"

echo "Stopping x11vnc..."
killall x11vnc 2>/dev/null || echo "  (not running)"

echo "Stopping Xvnc..."
killall Xvnc 2>/dev/null || echo "  (not running)"

echo "Stopping any running IDE processes..."
killall java 2>/dev/null || echo "  (not running)"
ps aux | grep "gradlew.*runIDE" | grep -v grep | awk '{print $2}' | xargs -r kill -9 2>/dev/null || echo "  (not running)"

sleep 2

echo ""
echo "Verifying shutdown..."
REMAINING=$(ps aux | grep -E "(Xvnc|x11vnc|websockify)" | grep -v grep | wc -l)
if [ "$REMAINING" -eq 0 ]; then
    echo "  ✓ All VNC processes stopped"
else
    echo "  ⚠ Some processes still running:"
    ps aux | grep -E "(Xvnc|x11vnc|websockify)" | grep -v grep
fi

echo ""
echo "=== VNC Test Harness Stopped ==="
