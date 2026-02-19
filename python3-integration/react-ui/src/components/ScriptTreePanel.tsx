import { useState } from 'react'
import {
  FileCode,
  RefreshCw,
  Search,
  Trash2,
  Folder,
  FolderOpen,
  ChevronRight,
  ChevronDown,
  Edit2,
  FolderInput,
} from 'lucide-react'
import { apiPost, apiGet, apiDelete } from '../utils/api'

interface ScriptEntry {
  name: string
  description?: string
  updatedAt?: string
  folderPath?: string
}

interface ScriptLoadData {
  name: string
  code: string
  description: string
  folderPath?: string
  updatedAt?: string
}

interface ScriptTreePanelProps {
  scripts: ScriptEntry[]
  folders: string[]
  selectedScript: string | null
  onSelectScript: (name: string) => void
  onDeleteScript: (name: string) => void
  onRenameScript: (oldName: string, newName: string) => void
  onRefresh: () => void
  gatewayUrl: string
}

function ScriptTreePanel({
  scripts,
  folders,
  selectedScript,
  onSelectScript,
  onDeleteScript,
  onRenameScript,
  onRefresh,
}: ScriptTreePanelProps) {
  const [filter, setFilter] = useState<string>('')
  // Track which folders are collapsed: key = folderPath, value = true if collapsed
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({})
  // Track inline rename state: key = script name, value = current input value or null if not renaming
  const [renaming, setRenaming] = useState<Record<string, string>>({})
  const [renameLoading, setRenameLoading] = useState<Record<string, boolean>>({})

  const filtered = scripts.filter(s =>
    s.name.toLowerCase().includes(filter.toLowerCase()) ||
    (s.description || '').toLowerCase().includes(filter.toLowerCase())
  )

  const toggleFolder = (folder: string) => {
    setCollapsed(prev => ({ ...prev, [folder]: !prev[folder] }))
  }

  const handleDelete = (e: React.MouseEvent, name: string) => {
    e.stopPropagation()
    if (window.confirm(`Delete script "${name}"?`)) {
      onDeleteScript(name)
    }
  }

  const startRename = (e: React.MouseEvent, name: string) => {
    e.stopPropagation()
    setRenaming(prev => ({ ...prev, [name]: name }))
  }

  const cancelRename = (name: string) => {
    setRenaming(prev => {
      const next = { ...prev }
      delete next[name]
      return next
    })
  }

  const commitRename = async (oldName: string) => {
    const newName = (renaming[oldName] || '').trim()
    if (!newName || newName === oldName) {
      cancelRename(oldName)
      return
    }

    setRenameLoading(prev => ({ ...prev, [oldName]: true }))
    try {
      // Load existing script data
      const data = await apiGet<ScriptLoadData>(`/api/v1/scripts/load/${encodeURIComponent(oldName)}`)
      // Save under new name
      await apiPost('/api/v1/scripts/save', {
        name: newName,
        code: data.code || '',
        description: data.description || '',
        folderPath: data.folderPath || undefined,
      })
      // Delete old name
      await apiDelete(`/api/v1/scripts/delete/${encodeURIComponent(oldName)}`)
      cancelRename(oldName)
      onRenameScript(oldName, newName)
    } catch (err) {
      alert(`Failed to rename script: ${err}`)
    } finally {
      setRenameLoading(prev => {
        const next = { ...prev }
        delete next[oldName]
        return next
      })
    }
  }

  const handleRenameKeyDown = (e: React.KeyboardEvent, oldName: string) => {
    if (e.key === 'Enter') {
      e.preventDefault()
      commitRename(oldName)
    } else if (e.key === 'Escape') {
      cancelRename(oldName)
    }
  }

  const handleMove = async (e: React.MouseEvent, name: string, currentFolder?: string) => {
    e.stopPropagation()
    const newFolder = window.prompt(
      `Move "${name}" to folder (leave blank for root):`,
      currentFolder || ''
    )
    if (newFolder === null) return // cancelled
    const trimmed = newFolder.trim()

    try {
      const data = await apiGet<ScriptLoadData>(`/api/v1/scripts/load/${encodeURIComponent(name)}`)
      await apiPost('/api/v1/scripts/save', {
        name: data.name || name,
        code: data.code || '',
        description: data.description || '',
        folderPath: trimmed || undefined,
      })
      onRefresh()
    } catch (err) {
      alert(`Failed to move script: ${err}`)
    }
  }

  // Group scripts by folder
  // Folders come from props (union of script-derived + locally created)
  // Root scripts: folderPath is null/empty
  const rootScripts = filtered.filter(s => !s.folderPath)
  const scriptsByFolder: Record<string, ScriptEntry[]> = {}
  for (const folder of folders) {
    scriptsByFolder[folder] = filtered.filter(s => s.folderPath === folder)
  }

  const renderScriptItem = (script: ScriptEntry, indented: boolean) => {
    const isActive = selectedScript === script.name
    const isRenamingThis = script.name in renaming
    const isRenameLoading = !!renameLoading[script.name]

    return (
      <div
        key={script.name}
        className={`script-tree-item${isActive ? ' active' : ''}${indented ? ' indented' : ''}`}
        onClick={() => !isRenamingThis && onSelectScript(script.name)}
        title={isRenamingThis ? undefined : (script.description || script.name)}
      >
        <span className="script-tree-item-icon">
          <FileCode size={14} />
        </span>

        {isRenamingThis ? (
          <input
            className="script-tree-rename-input"
            value={renaming[script.name]}
            onChange={e =>
              setRenaming(prev => ({ ...prev, [script.name]: e.target.value }))
            }
            onKeyDown={e => handleRenameKeyDown(e, script.name)}
            onBlur={() => commitRename(script.name)}
            onClick={e => e.stopPropagation()}
            autoFocus
            disabled={isRenameLoading}
          />
        ) : (
          <span className="script-tree-item-name">{script.name}</span>
        )}

        {!isRenamingThis && (
          <span className="script-tree-item-actions">
            <span
              className="script-tree-action-btn"
              role="button"
              aria-label={`Rename ${script.name}`}
              title="Rename"
              onClick={e => startRename(e, script.name)}
            >
              <Edit2 size={11} />
            </span>
            <span
              className="script-tree-action-btn"
              role="button"
              aria-label={`Move ${script.name}`}
              title="Move to folder"
              onClick={e => handleMove(e, script.name, script.folderPath)}
            >
              <FolderInput size={11} />
            </span>
            <span
              className="script-tree-action-btn delete"
              role="button"
              aria-label={`Delete ${script.name}`}
              title={`Delete ${script.name}`}
              onClick={e => handleDelete(e, script.name)}
            >
              <Trash2 size={11} />
            </span>
          </span>
        )}
      </div>
    )
  }

  const isEmpty = filtered.length === 0 && folders.length === 0

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
        {isEmpty && (
          <div className="script-tree-empty">
            {filter ? 'No scripts match filter.' : 'No scripts saved yet.'}
          </div>
        )}

        {/* Folders with their scripts */}
        {folders.map(folder => {
          const folderScripts = scriptsByFolder[folder] || []
          const isCollapsed = !!collapsed[folder]
          return (
            <div key={folder} className="script-tree-folder">
              <div
                className="script-tree-folder-header"
                onClick={() => toggleFolder(folder)}
                title={folder}
              >
                <span className="script-tree-folder-arrow">
                  {isCollapsed ? <ChevronRight size={13} /> : <ChevronDown size={13} />}
                </span>
                <span className="script-tree-folder-icon">
                  {isCollapsed ? <Folder size={14} /> : <FolderOpen size={14} />}
                </span>
                <span className="script-tree-folder-name">{folder}</span>
                <span className="script-tree-folder-count">{folderScripts.length}</span>
              </div>
              {!isCollapsed && (
                <div className="script-tree-folder-contents">
                  {folderScripts.length === 0 ? (
                    <div className="script-tree-folder-empty">Empty folder</div>
                  ) : (
                    folderScripts.map(s => renderScriptItem(s, true))
                  )}
                </div>
              )}
            </div>
          )
        })}

        {/* Root-level scripts (no folder) */}
        {rootScripts.map(s => renderScriptItem(s, false))}
      </div>
    </div>
  )
}

export default ScriptTreePanel
