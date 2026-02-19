import { useEffect, useRef } from 'react'
import { Terminal } from 'xterm'
import { FitAddon } from '@xterm/addon-fit'
import { WebLinksAddon } from '@xterm/addon-web-links'
import { SearchAddon } from '@xterm/addon-search'
import { CanvasAddon } from '@xterm/addon-canvas'
import 'xterm/css/xterm.css'

interface TerminalTabProps {
  gatewayUrl: string
  sessionId: string
  isActive: boolean
  onClose: () => void
}

function TerminalTab({ gatewayUrl, sessionId, isActive }: TerminalTabProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const terminalRef = useRef<Terminal | null>(null)
  const fitAddonRef = useRef<FitAddon | null>(null)
  const lineBufferRef = useRef<string>('')
  const mountedRef = useRef<boolean>(false)

  useEffect(() => {
    if (!isActive || !containerRef.current || mountedRef.current) return
    mountedRef.current = true

    const term = new Terminal({
      theme: {
        background: '#0a0e14',
        foreground: '#e6e8eb',
        cursor: '#61afef',
        selectionBackground: '#2a3039',
        black: '#1a1f28',
        red: '#e06c75',
        green: '#98c379',
        yellow: '#e5c07b',
        blue: '#61afef',
        magenta: '#c678dd',
        cyan: '#56b6c2',
        white: '#e6e8eb',
        brightBlack: '#2a3039',
        brightRed: '#e06c75',
        brightGreen: '#98c379',
        brightYellow: '#e5c07b',
        brightBlue: '#61afef',
        brightMagenta: '#c678dd',
        brightCyan: '#56b6c2',
        brightWhite: '#ffffff',
      },
      fontFamily: "'Cascadia Code', 'Fira Code', 'JetBrains Mono', 'Consolas', monospace",
      fontSize: 14,
      lineHeight: 1.4,
      cursorBlink: true,
      cursorStyle: 'bar',
      scrollback: 10000,
      convertEol: true,
    })

    const fitAddon = new FitAddon()
    const webLinksAddon = new WebLinksAddon()
    const searchAddon = new SearchAddon()

    term.loadAddon(fitAddon)
    term.loadAddon(webLinksAddon)
    term.loadAddon(searchAddon)

    try {
      const canvasAddon = new CanvasAddon()
      term.loadAddon(canvasAddon)
    } catch {
      // CanvasAddon may fail in some environments; fall back to DOM renderer
    }

    term.open(containerRef.current)
    fitAddon.fit()

    terminalRef.current = term
    fitAddonRef.current = fitAddon

    // Write welcome message and initial prompt
    term.writeln('\x1b[36mPython 3 Interactive Shell\x1b[0m')
    term.writeln('\x1b[90mType Python expressions and press Enter to execute.\x1b[0m')
    term.writeln('')
    term.write('\x1b[34m>>>\x1b[0m ')

    // Handle user input
    term.onData((data) => {
      const code = data.charCodeAt(0)

      // Handle Enter
      if (data === '\r' || data === '\n') {
        const command = lineBufferRef.current.trim()
        lineBufferRef.current = ''
        term.writeln('')

        if (command === '') {
          term.write('\x1b[34m>>>\x1b[0m ')
          return
        }

        if (command === 'clear' || command === 'cls') {
          term.clear()
          term.write('\x1b[34m>>>\x1b[0m ')
          return
        }

        if (command === 'exit' || command === 'quit()') {
          term.writeln('\x1b[90mUse the tab close button to close this terminal.\x1b[0m')
          term.write('\x1b[34m>>>\x1b[0m ')
          return
        }

        // Send command to gateway
        sendCommand(command, term)
        return
      }

      // Handle Backspace
      if (data === '\x7f' || code === 8) {
        if (lineBufferRef.current.length > 0) {
          lineBufferRef.current = lineBufferRef.current.slice(0, -1)
          term.write('\b \b')
        }
        return
      }

      // Handle Ctrl+C
      if (data === '\x03') {
        lineBufferRef.current = ''
        term.writeln('^C')
        term.write('\x1b[34m>>>\x1b[0m ')
        return
      }

      // Handle Ctrl+L (clear screen)
      if (data === '\x0c') {
        term.clear()
        term.write('\x1b[34m>>>\x1b[0m ')
        return
      }

      // Ignore control characters and escape sequences (arrow keys etc.)
      if (code < 32 || data.startsWith('\x1b')) {
        return
      }

      // Normal printable character
      lineBufferRef.current += data
      term.write(data)
    })

    const handleResize = () => {
      try {
        fitAddon.fit()
      } catch {
        // ignore resize errors
      }
    }

    window.addEventListener('resize', handleResize)

    return () => {
      window.removeEventListener('resize', handleResize)
    }
  }, [isActive, gatewayUrl, sessionId])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (terminalRef.current) {
        // Try to close session
        fetch(`${gatewayUrl}/api/v1/shell-interactive/close`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ sessionId }),
        }).catch(() => {})

        terminalRef.current.dispose()
        terminalRef.current = null
      }
    }
  }, [gatewayUrl, sessionId])

  // Re-fit when becoming active
  useEffect(() => {
    if (isActive && fitAddonRef.current) {
      setTimeout(() => {
        try {
          fitAddonRef.current?.fit()
        } catch {
          // ignore
        }
      }, 50)
    }
  }, [isActive])

  const sendCommand = async (command: string, term: Terminal) => {
    try {
      const res = await fetch(`${gatewayUrl}/api/v1/shell-interactive/exec`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId, command }),
      })

      const raw = await res.json()
      const data = raw.data || raw

      if (data.output && data.output.trim()) {
        const lines = data.output.split('\n')
        for (const line of lines) {
          if (line) {
            term.writeln(line)
          }
        }
      }

      if (data.error && data.error.trim()) {
        const errorLines = data.error.split('\n')
        for (const line of errorLines) {
          if (line) {
            term.writeln(`\x1b[31m${line}\x1b[0m`)
          }
        }
      }

      if (!data.output && !data.error && data.result !== undefined && data.result !== null) {
        term.writeln(String(data.result))
      }
    } catch (err) {
      term.writeln(`\x1b[31mError: Failed to execute command\x1b[0m`)
      console.error('Shell exec error:', err)
    } finally {
      term.write('\x1b[34m>>>\x1b[0m ')
    }
  }

  return (
    <div
      className="xterm-container"
      ref={containerRef}
      style={{ display: isActive ? 'block' : 'none' }}
    />
  )
}

export default TerminalTab
