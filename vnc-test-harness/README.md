# VNC Test Harness for Python 3 IDE

This directory contains scripts to set up a VNC server for visually testing the Ignition Python 3 IDE in a headless environment.

## Quick Start

```bash
# Make scripts executable
chmod +x setup-vnc.sh stop-vnc.sh

# Start VNC server
./setup-vnc.sh

# Access the desktop in your browser
# → http://localhost:6080/vnc.html

# Run the IDE on the VNC display
cd ../python3-integration
DISPLAY=:2 ./gradlew :designer:runIDE

# When finished, stop VNC
cd ../vnc-test-harness
./stop-vnc.sh
```

## What This Does

The VNC test harness creates a virtual desktop that you can access through a web browser:

1. **Xvnc** - Runs a virtual X server on display `:2` (port 5902)
2. **x11vnc** - Proxies the Xvnc display for VNC clients
3. **websockify** - Converts VNC protocol to WebSocket
4. **noVNC** - Provides HTML5 VNC client at http://localhost:6080/vnc.html

## Requirements

The setup script will automatically install:
- `tigervnc-standalone-server` - Virtual X server
- `x11vnc` - VNC server
- `websockify` - WebSocket proxy
- `novnc` - Web-based VNC client
- `python3-numpy` - Required by websockify

## Usage

### Starting VNC

```bash
./setup-vnc.sh
```

This will:
- Install required packages (if needed)
- Clean up any existing VNC processes
- Start Xvnc on display :2 (1920x1080 resolution)
- Start x11vnc on port 5902
- Start websockify on port 6080
- Display status of all processes

### Accessing the Desktop

Open your web browser and navigate to:
```
http://localhost:6080/vnc.html
```

You'll see a virtual desktop where GUI applications will appear.

### Running the IDE

In a separate terminal:
```bash
cd /modules/ignition-module-python3-java/python3-integration
DISPLAY=:2 ./gradlew :designer:runIDE
```

The IDE window will appear in your browser at the noVNC URL.

### Stopping VNC

```bash
./stop-vnc.sh
```

This will:
- Stop websockify, x11vnc, and Xvnc
- Kill any running IDE processes
- Verify all processes are stopped

## Troubleshooting

### "Connection failed" in browser

Check if websockify is running:
```bash
ps aux | grep websockify
netstat -tlnp | grep 6080
```

Restart if needed:
```bash
./stop-vnc.sh
./setup-vnc.sh
```

### IDE window doesn't appear

Check if Xvnc is running:
```bash
ps aux | grep Xvnc
DISPLAY=:2 xdpyinfo | head -10
```

Check IDE logs:
```bash
tail -f /tmp/ide_harness.log
```

### Port already in use

If ports 5902 or 6080 are already in use:
```bash
# Find processes using the ports
lsof -i :5902
lsof -i :6080

# Kill them or use stop-vnc.sh
./stop-vnc.sh
```

## Display Configuration

The VNC display is configured as:
- **Display**: `:2`
- **Port**: `5902` (VNC) / `6080` (HTTP)
- **Resolution**: `1920x1080x24`
- **Security**: None (no password - local use only)

⚠️ **Security Note**: This setup has no authentication and should only be used in local development environments, never exposed to public networks.

## Log Files

- `/tmp/x11vnc.log` - x11vnc server logs
- `/tmp/websockify.log` - websockify proxy logs
- `/tmp/ide_harness.log` - IDE application logs (if logging to file)

## Alternative: Manual Setup

If you prefer to run commands manually:

```bash
# Start Xvnc
Xvnc :2 -screen 0 1920x1080x24 -SecurityTypes None -AlwaysShared &

# Start x11vnc
x11vnc -display :2 -forever -shared -rfbport 5902 -nopw -bg

# Start websockify
websockify -D --web=/usr/share/novnc/ 6080 localhost:5902

# Run IDE
DISPLAY=:2 ./gradlew :designer:runIDE
```

## Integration with Claude Code

This VNC setup was created to enable rapid visual iteration on UI changes with Claude Code. The workflow is:

1. Start VNC test harness
2. Run IDE on display :2
3. View in browser at http://localhost:6080/vnc.html
4. Make code changes with Claude Code
5. Recompile and restart IDE
6. Instantly see visual changes in browser

This eliminates the need for:
- Physical display or X forwarding
- Screenshot tools (everything is in the browser)
- Manual window management

Perfect for headless development environments and remote coding sessions!
