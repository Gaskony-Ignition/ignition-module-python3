import { useState, useRef, useCallback } from 'react'
import { X, Plus, Terminal } from 'lucide-react'

interface TabEntry {
  id: string
  title: string
  sessionId: string
  lastActivityTime?: number
}

interface TerminalTabBarProps {
  tabs: TabEntry[]
  activeTabId: string
  onSelectTab: (id: string) => void
  onCloseTab: (id: string) => void
  onNewTab: () => void
  onRenameTab?: (id: string, newTitle: string) => void
}

// Show active dot if there was activity within the last 5 seconds
const ACTIVE_THRESHOLD_MS = 5000

function TerminalTabBar({
  tabs,
  activeTabId,
  onSelectTab,
  onCloseTab,
  onNewTab,
  onRenameTab,
}: TerminalTabBarProps) {
  const [renamingTabId, setRenamingTabId] = useState<string | null>(null)
  const [renameValue, setRenameValue] = useState('')
  const renameInputRef = useRef<HTMLInputElement>(null)

  const startRename = useCallback((tab: TabEntry, e: React.MouseEvent) => {
    e.stopPropagation()
    setRenamingTabId(tab.id)
    setRenameValue(tab.title)
    // Focus input on next tick
    setTimeout(() => renameInputRef.current?.select(), 0)
  }, [])

  const commitRename = useCallback(() => {
    if (renamingTabId && renameValue.trim()) {
      onRenameTab?.(renamingTabId, renameValue.trim())
    }
    setRenamingTabId(null)
  }, [renamingTabId, renameValue, onRenameTab])

  const handleRenameKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter') commitRename()
    if (e.key === 'Escape') setRenamingTabId(null)
  }, [commitRename])

  const now = Date.now()

  return (
    <div className="terminal-tab-bar">
      {tabs.map(tab => {
        const isActive = tab.id === activeTabId
        const isRecent = tab.lastActivityTime !== undefined && (now - tab.lastActivityTime) < ACTIVE_THRESHOLD_MS

        return (
          <button
            key={tab.id}
            className={`terminal-tab ${isActive ? 'active' : ''}`}
            onClick={() => onSelectTab(tab.id)}
            title={tab.title}
          >
            <Terminal size={13} />
            {/* Activity indicator dot */}
            <span
              className="terminal-tab-activity"
              style={{
                display: 'inline-block',
                width: 6,
                height: 6,
                borderRadius: '50%',
                background: isRecent ? '#98c379' : 'var(--border-light)',
                flexShrink: 0,
              }}
              title={isRecent ? 'Active' : 'Idle'}
            />
            {renamingTabId === tab.id ? (
              <input
                ref={renameInputRef}
                className="terminal-tab-rename-input"
                value={renameValue}
                onChange={e => setRenameValue(e.target.value)}
                onBlur={commitRename}
                onKeyDown={handleRenameKeyDown}
                onClick={e => e.stopPropagation()}
                style={{
                  background: 'var(--bg-active)',
                  border: '1px solid var(--accent-primary)',
                  color: 'var(--text-primary)',
                  borderRadius: 3,
                  padding: '0 4px',
                  fontSize: 12,
                  width: 90,
                  outline: 'none',
                }}
                autoFocus
              />
            ) : (
              <span
                className="terminal-tab-title"
                onDoubleClick={e => startRename(tab, e)}
                title="Double-click to rename"
              >
                {tab.title}
              </span>
            )}
            <span
              className="terminal-tab-close"
              role="button"
              aria-label={`Close ${tab.title}`}
              onClick={(e) => {
                e.stopPropagation()
                onCloseTab(tab.id)
              }}
            >
              <X size={12} />
            </span>
          </button>
        )
      })}
      <button
        className="terminal-new-tab"
        onClick={onNewTab}
        title="New terminal tab"
        aria-label="New terminal tab"
      >
        <Plus size={14} />
      </button>
    </div>
  )
}

export default TerminalTabBar
