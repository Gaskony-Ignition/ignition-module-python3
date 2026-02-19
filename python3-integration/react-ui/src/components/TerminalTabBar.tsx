import { X, Plus, Terminal } from 'lucide-react'

interface TabEntry {
  id: string
  title: string
  sessionId: string
}

interface TerminalTabBarProps {
  tabs: TabEntry[]
  activeTabId: string
  onSelectTab: (id: string) => void
  onCloseTab: (id: string) => void
  onNewTab: () => void
}

function TerminalTabBar({ tabs, activeTabId, onSelectTab, onCloseTab, onNewTab }: TerminalTabBarProps) {
  return (
    <div className="terminal-tab-bar">
      {tabs.map(tab => (
        <button
          key={tab.id}
          className={`terminal-tab ${tab.id === activeTabId ? 'active' : ''}`}
          onClick={() => onSelectTab(tab.id)}
          title={tab.title}
        >
          <Terminal size={13} />
          <span className="terminal-tab-title">{tab.title}</span>
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
      ))}
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
