#!/bin/bash

# VNC Test Harness Setup Script
# This script sets up a VNC server and noVNC web interface for testing the Ignition Python 3 IDE

set -e

echo "=== VNC Test Harness Setup ==="
echo "This script will:"
echo "  1. Start Xvnc on display :2 (port 5902)"
echo "  2. Start x11vnc to proxy the display"
echo "  3. Start websockify to enable web access"
echo "  4. Make noVNC accessible at http://localhost:6080/vnc.html"
echo ""

# Step 1: Check if packages are installed
echo "Step 1: Checking required packages..."
if ! command -v Xvnc &> /dev/null; then
    echo "  Installing tigervnc-standalone-server..."
    apt-get update -qq
    apt-get install -y tigervnc-standalone-server -qq
fi

if ! command -v x11vnc &> /dev/null; then
    echo "  Installing x11vnc..."
    apt-get install -y x11vnc -qq
fi

if ! command -v websockify &> /dev/null; then
    echo "  Installing websockify..."
    apt-get install -y websockify python3-numpy -qq
fi

if [ ! -f /usr/share/novnc/vnc.html ]; then
    echo "  Installing novnc..."
    apt-get install -y novnc -qq
fi

echo "  ✓ All packages installed"

# Step 2: Kill any existing VNC processes
echo ""
echo "Step 2: Cleaning up existing VNC processes..."
killall Xvnc x11vnc websockify 2>/dev/null || true
sleep 2
echo "  ✓ Cleanup complete"

# Step 3: Start Xvnc
echo ""
echo "Step 3: Starting Xvnc on display :2..."
Xvnc :2 -screen 0 1920x1080x24 -SecurityTypes None -AlwaysShared &
XVNC_PID=$!
sleep 3
echo "  ✓ Xvnc started (PID: $XVNC_PID)"

# Step 4: Start x11vnc
echo ""
echo "Step 4: Starting x11vnc proxy..."
x11vnc -display :2 -forever -shared -rfbport 5902 -nopw -bg -o /tmp/x11vnc.log
sleep 2
echo "  ✓ x11vnc started on port 5902"

# Step 5: Start websockify
echo ""
echo "Step 5: Starting websockify for web access..."
websockify -D --web=/usr/share/novnc/ 6080 localhost:5902 > /tmp/websockify.log 2>&1
sleep 2
echo "  ✓ websockify started on port 6080"

# Step 6: Verify all processes
echo ""
echo "Step 6: Verifying processes..."
ps aux | grep -E "(Xvnc|x11vnc|websockify)" | grep -v grep
echo ""

echo "=== VNC Test Harness Ready ==="
echo ""
echo "Access the VNC desktop in your browser:"
echo "  → http://localhost:6080/vnc.html"
echo ""
echo "Display :2 is now available for running GUI applications"
echo "Example: DISPLAY=:2 ./gradlew :designer:runIDE"
echo ""
echo "To stop VNC, run: ./stop-vnc.sh"
