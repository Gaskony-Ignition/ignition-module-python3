import { useState, useEffect, useCallback, useRef } from 'react'
import TerminalTabBar from './TerminalTabBar'
import TerminalTab from './TerminalTab'
import VersionSelector from './VersionSelector'
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
  const [selectedVersion, setSelectedVersion] = useState<string>('')

  const createTab = useCallback(async () => {
    setSessionError('')
    try {
      const body: Record<string, string> = {}
      if (selectedVersion) {
        body.pythonVersion = selectedVersion
      }
      const data = await apiPost<{ sessionId: string }>('/api/v1/shell-interactive/create', body)

      if (data.sessionId) {
        const id = `tab-${Date.now()}`
        const versionLabel = selectedVersion ? `Python ${selectedVersion}` : 'Python'
        setTabs(prev => {
          const newTab: TabEntry = {
            id,
            title: `${versionLabel} ${prev.length + 1}`,
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
  }, [selectedVersion])

  // Use ref for createTab to avoid re-triggering useEffect
  const createTabRef = useRef(createTab)
  createTabRef.current = createTab

  // Auto-create first tab on mount (once only)
  const hasAutoCreated = useRef(false)
  useEffect(() => {
    if (hasAutoCreated.current) return
    hasAutoCreated.current = true
    // Small delay to allow CSRF token initialization
    const timer = setTimeout(() => createTabRef.current(), 500)
    return () => clearTimeout(timer)
  }, [])

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
      <div className="terminal-toolbar">
        <VersionSelector
          gatewayUrl={gatewayUrl}
          selectedVersion={selectedVersion}
          onVersionChange={setSelectedVersion}
        />
      </div>
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
