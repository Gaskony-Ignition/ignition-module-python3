import { useState, useEffect, useCallback } from 'react'
import TerminalTabBar from './TerminalTabBar'
import TerminalTab from './TerminalTab'
import { apiPost } from '../utils/api'
import './TerminalView.css'

interface TabEntry {
  id: string
  title: string
  sessionId: string
  lastActivityTime?: number
}

interface Props {
  gatewayUrl: string
}

function TerminalView({ gatewayUrl }: Props) {
  const [tabs, setTabs] = useState<TabEntry[]>([])
  const [activeTabId, setActiveTabId] = useState<string>('')
  const [sessionError, setSessionError] = useState<string>('')

  const createTab = useCallback(async () => {
    setSessionError('')
    try {
      const data = await apiPost<{ sessionId: string }>('/api/v1/shell-interactive/create', {})

      if (data.sessionId) {
        const id = `tab-${Date.now()}`
        setTabs(prev => {
          const newTab: TabEntry = {
            id,
            title: `Python ${prev.length + 1}`,
            sessionId: data.sessionId,
            lastActivityTime: Date.now(),
          }
          return [...prev, newTab]
        })
        setActiveTabId(id)
      } else {
        setSessionError('Failed to create terminal session: no sessionId returned')
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Unknown error'
      setSessionError(`Failed to create terminal session: ${msg}`)
    }
  }, [])

  // Auto-create first tab on mount
  useEffect(() => {
    createTab()
  }, [createTab])

  const handleCloseTab = useCallback(async (tabId: string) => {
    const tab = tabs.find(t => t.id === tabId)
    if (!tab) return

    // Close session on gateway (best-effort, ignore errors)
    apiPost('/api/v1/shell-interactive/close', { sessionId: tab.sessionId }).catch(() => {})

    setTabs(prev => {
      const remaining = prev.filter(t => t.id !== tabId)
      if (tabId === activeTabId && remaining.length > 0) {
        setActiveTabId(remaining[remaining.length - 1].id)
      } else if (remaining.length === 0) {
        setActiveTabId('')
      }
      return remaining
    })
  }, [tabs, activeTabId])

  const handleRenameTab = useCallback((tabId: string, newTitle: string) => {
    setTabs(prev => prev.map(t => t.id === tabId ? { ...t, title: newTitle } : t))
  }, [])

  const handleActivityUpdate = useCallback((tabId: string) => {
    setTabs(prev => prev.map(t => t.id === tabId ? { ...t, lastActivityTime: Date.now() } : t))
  }, [])

  return (
    <div className="terminal-view">
      {sessionError && (
        <div className="terminal-session-error">
          {sessionError}
          <button onClick={() => setSessionError('')} className="terminal-error-dismiss">
            Dismiss
          </button>
        </div>
      )}
      <TerminalTabBar
        tabs={tabs}
        activeTabId={activeTabId}
        onSelectTab={setActiveTabId}
        onCloseTab={handleCloseTab}
        onNewTab={createTab}
        onRenameTab={handleRenameTab}
      />
      <div className="terminal-content">
        {tabs.length === 0 && (
          <div className="terminal-empty">
            <p>No terminal sessions. Click + to create one.</p>
          </div>
        )}
        {tabs.map(tab => (
          <TerminalTab
            key={tab.id}
            tabId={tab.id}
            sessionId={tab.sessionId}
            isActive={tab.id === activeTabId}
            onClose={() => handleCloseTab(tab.id)}
            onActivity={handleActivityUpdate}
          />
        ))}
      </div>
    </div>
  )
}

export default TerminalView
