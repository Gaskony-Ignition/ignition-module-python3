import { useState } from 'react'
import { FileCode, RefreshCw, Search, Trash2 } from 'lucide-react'

interface ScriptEntry {
  name: string
  description?: string
  updatedAt?: string
}

interface ScriptTreePanelProps {
  scripts: ScriptEntry[]
  selectedScript: string | null
  onSelectScript: (name: string) => void
  onDeleteScript: (name: string) => void
  onRefresh: () => void
}

function ScriptTreePanel({
  scripts,
  selectedScript,
  onSelectScript,
  onDeleteScript,
  onRefresh,
}: ScriptTreePanelProps) {
  const [filter, setFilter] = useState<string>('')

  const filtered = scripts.filter(s =>
    s.name.toLowerCase().includes(filter.toLowerCase()) ||
    (s.description || '').toLowerCase().includes(filter.toLowerCase())
  )

  const handleDelete = (e: React.MouseEvent, name: string) => {
    e.stopPropagation()
    if (window.confirm(`Delete script "${name}"?`)) {
      onDeleteScript(name)
    }
  }

  return (
    <div className="script-tree-panel">
      <div className="script-tree-toolbar">
        <div className="script-tree-search">
          <Search size={13} />
          <input
            type="text"
            placeholder="Filter scripts..."
            value={filter}
            onChange={e => setFilter(e.target.value)}
            className="script-tree-search-input"
          />
        </div>
        <button
          className="script-tree-refresh"
          onClick={onRefresh}
          title="Refresh scripts"
          aria-label="Refresh scripts"
        >
          <RefreshCw size={14} />
        </button>
      </div>

      <div className="script-tree-list">
        {filtered.length === 0 && (
          <div className="script-tree-empty">
            {filter ? 'No scripts match filter.' : 'No scripts saved yet.'}
          </div>
        )}
        {filtered.map(script => (
          <button
            key={script.name}
            className={`script-tree-item ${selectedScript === script.name ? 'active' : ''}`}
            onClick={() => onSelectScript(script.name)}
            title={script.description || script.name}
          >
            <span className="script-tree-item-icon">
              <FileCode size={14} />
            </span>
            <span className="script-tree-item-name">{script.name}</span>
            <span
              className="script-tree-item-delete"
              role="button"
              aria-label={`Delete ${script.name}`}
              onClick={e => handleDelete(e, script.name)}
              title={`Delete ${script.name}`}
            >
              <Trash2 size={12} />
            </span>
          </button>
        ))}
      </div>
    </div>
  )
}

export default ScriptTreePanel
