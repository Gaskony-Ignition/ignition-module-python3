import { useState, useEffect, useCallback } from 'react'
import ScriptTreePanel from './ScriptTreePanel'
import ScriptDetailPanel from './ScriptDetailPanel'
import ImportExportModal from './ImportExportModal'
import { Plus, Upload, RefreshCw } from 'lucide-react'
import './ScriptsView.css'

interface ScriptEntry {
  name: string
  description?: string
  updatedAt?: string
}

interface Props {
  gatewayUrl: string
}

function ScriptsView({ gatewayUrl }: Props) {
  const [scripts, setScripts] = useState<ScriptEntry[]>([])
  const [selectedScript, setSelectedScript] = useState<string | null>(null)
  const [showImportModal, setShowImportModal] = useState<boolean>(false)
  const [loading, setLoading] = useState<boolean>(false)
  const [error, setError] = useState<string>('')

  const fetchScripts = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const res = await fetch(`${gatewayUrl}/api/v1/scripts/list`)
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const raw = await res.json()
      const data = raw.data || raw
      // data could be an array or { scripts: [...] }
      const list: ScriptEntry[] = Array.isArray(data)
        ? data
        : Array.isArray(data.scripts)
          ? data.scripts
          : []
      setScripts(list)
    } catch (err) {
      console.error('Failed to fetch scripts:', err)
      setError(`Failed to load scripts: ${err}`)
    } finally {
      setLoading(false)
    }
  }, [gatewayUrl])

  useEffect(() => {
    fetchScripts()
  }, [fetchScripts])

  const handleNewScript = async () => {
    const name = window.prompt('Enter script name:')
    if (!name || !name.trim()) return
    const trimmed = name.trim()

    try {
      const res = await fetch(`${gatewayUrl}/api/v1/scripts/save`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: trimmed, code: '', description: '' }),
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      await fetchScripts()
      setSelectedScript(trimmed)
    } catch (err) {
      alert(`Failed to create script: ${err}`)
    }
  }

  const handleDeleteScript = async (name: string) => {
    try {
      const res = await fetch(
        `${gatewayUrl}/api/v1/scripts/delete/${encodeURIComponent(name)}`,
        { method: 'DELETE' }
      )
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      if (selectedScript === name) {
        setSelectedScript(null)
      }
      await fetchScripts()
    } catch (err) {
      alert(`Failed to delete script: ${err}`)
    }
  }

  return (
    <div className="scripts-view">
      {/* Header */}
      <div className="scripts-header">
        <span className="scripts-header-title">Scripts</span>
        <div className="scripts-header-actions">
          <button
            className="scripts-header-btn"
            onClick={fetchScripts}
            disabled={loading}
            title="Refresh"
          >
            <RefreshCw size={14} />
            <span>Refresh</span>
          </button>
          <button
            className="scripts-header-btn"
            onClick={() => setShowImportModal(true)}
            title="Import .py file"
          >
            <Upload size={14} />
            <span>Import .py</span>
          </button>
          <button
            className="scripts-header-btn primary"
            onClick={handleNewScript}
            title="New Script"
          >
            <Plus size={14} />
            <span>New Script</span>
          </button>
        </div>
      </div>

      {/* Error banner */}
      {error && (
        <div className="scripts-error-banner">{error}</div>
      )}

      {/* Two-panel layout */}
      <div className="scripts-panels">
        <div className="scripts-left-panel">
          <ScriptTreePanel
            scripts={scripts}
            selectedScript={selectedScript}
            onSelectScript={setSelectedScript}
            onDeleteScript={handleDeleteScript}
            onRefresh={fetchScripts}
          />
        </div>
        <div className="scripts-right-panel">
          <ScriptDetailPanel
            gatewayUrl={gatewayUrl}
            scriptName={selectedScript}
            onScriptSaved={fetchScripts}
          />
        </div>
      </div>

      {/* Import Modal */}
      <ImportExportModal
        isOpen={showImportModal}
        onClose={() => setShowImportModal(false)}
        gatewayUrl={gatewayUrl}
        onImported={() => {
          setShowImportModal(false)
          fetchScripts()
        }}
      />
    </div>
  )
}

export default ScriptsView
