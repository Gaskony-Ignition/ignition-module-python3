import { useState, useEffect, useCallback } from 'react'
import TerminalTabBar from './TerminalTabBar'
import TerminalTab from './TerminalTab'
import './TerminalView.css'

interface TabEntry {
  id: string
  title: string
  sessionId: string
}

interface Props {
  gatewayUrl: string
}

function TerminalView({ gatewayUrl }: Props) {
  const [tabs, setTabs] = useState<TabEntry[]>([])
  const [activeTabId, setActiveTabId] = useState<string>('')

  const createTab = useCallback(async () => {
    try {
      const res = await fetch(`${gatewayUrl}/api/v1/shell-interactive/create`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      })
      const raw = await res.json()
      const data = raw.data || raw

      if (data.sessionId) {
        const id = `tab-${Date.now()}`
        setTabs(prev => {
          const newTab: TabEntry = {
            id,
            title: `Python ${prev.length + 1}`,
            sessionId: data.sessionId,
          }
          return [...prev, newTab]
        })
        setActiveTabId(id)
      } else {
        console.error('No sessionId in response:', data)
      }
    } catch (err) {
      console.error('Failed to create terminal session:', err)
    }
  }, [gatewayUrl])

  // Auto-create first tab on mount
  useEffect(() => {
    createTab()
  }, [createTab])

  const handleCloseTab = useCallback(async (tabId: string) => {
    const tab = tabs.find(t => t.id === tabId)
    if (!tab) return

    // Close session on gateway
    try {
      await fetch(`${gatewayUrl}/api/v1/shell-interactive/close`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId: tab.sessionId }),
      })
    } catch {
      // ignore close errors
    }

    setTabs(prev => {
      const remaining = prev.filter(t => t.id !== tabId)
      if (tabId === activeTabId && remaining.length > 0) {
        setActiveTabId(remaining[remaining.length - 1].id)
      } else if (remaining.length === 0) {
        setActiveTabId('')
      }
      return remaining
    })
  }, [tabs, activeTabId, gatewayUrl])

  return (
    <div className="terminal-view">
      <TerminalTabBar
        tabs={tabs}
        activeTabId={activeTabId}
        onSelectTab={setActiveTabId}
        onCloseTab={handleCloseTab}
        onNewTab={createTab}
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
            gatewayUrl={gatewayUrl}
            sessionId={tab.sessionId}
            isActive={tab.id === activeTabId}
            onClose={() => handleCloseTab(tab.id)}
          />
        ))}
      </div>
    </div>
  )
}

export default TerminalView
