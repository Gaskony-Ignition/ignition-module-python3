import { useState, useEffect } from 'react'
import { Edit2, Save, Download, Copy } from 'lucide-react'

interface ScriptDetailPanelProps {
  gatewayUrl: string
  scriptName: string | null
  onScriptSaved: () => void
}

interface ScriptData {
  name: string
  code: string
  description: string
  updatedAt?: string
}

function ScriptDetailPanel({ gatewayUrl, scriptName, onScriptSaved }: ScriptDetailPanelProps) {
  const [script, setScript] = useState<ScriptData | null>(null)
  const [isEditing, setIsEditing] = useState<boolean>(false)
  const [editCode, setEditCode] = useState<string>('')
  const [editDescription, setEditDescription] = useState<string>('')
  const [loading, setLoading] = useState<boolean>(false)
  const [saving, setSaving] = useState<boolean>(false)
  const [error, setError] = useState<string>('')
  const [copyMsg, setCopyMsg] = useState<string>('')

  useEffect(() => {
    if (!scriptName) {
      setScript(null)
      setIsEditing(false)
      return
    }

    setLoading(true)
    setError('')
    setIsEditing(false)

    fetch(`${gatewayUrl}/api/v1/scripts/load/${encodeURIComponent(scriptName)}`)
      .then(res => res.ok ? res.json() : Promise.reject(`HTTP ${res.status}`))
      .then(raw => {
        const data = raw.data || raw
        setScript({
          name: data.name || scriptName,
          code: data.code || '',
          description: data.description || '',
          updatedAt: data.updatedAt || data.lastModified || '',
        })
        setEditCode(data.code || '')
        setEditDescription(data.description || '')
      })
      .catch(err => {
        console.error('Failed to load script:', err)
        setError(`Failed to load script: ${err}`)
      })
      .finally(() => setLoading(false))
  }, [gatewayUrl, scriptName])

  const handleEdit = () => {
    if (!script) return
    setEditCode(script.code)
    setEditDescription(script.description)
    setIsEditing(true)
  }

  const handleSave = async () => {
    if (!script) return
    setSaving(true)
    setError('')

    try {
      const res = await fetch(`${gatewayUrl}/api/v1/scripts/save`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: script.name,
          code: editCode,
          description: editDescription,
        }),
      })

      if (!res.ok) throw new Error(`HTTP ${res.status}`)

      setScript(prev => prev ? { ...prev, code: editCode, description: editDescription } : null)
      setIsEditing(false)
      onScriptSaved()
    } catch (err) {
      setError(`Failed to save: ${err}`)
    } finally {
      setSaving(false)
    }
  }

  const handleCancelEdit = () => {
    setIsEditing(false)
    if (script) {
      setEditCode(script.code)
      setEditDescription(script.description)
    }
  }

  const handleExport = () => {
    if (!script) return
    const blob = new Blob([script.code], { type: 'text/x-python' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${script.name}.py`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  }

  const handleCopy = () => {
    if (!script) return
    navigator.clipboard.writeText(script.code).then(() => {
      setCopyMsg('Copied!')
      setTimeout(() => setCopyMsg(''), 2000)
    }).catch(() => {
      setCopyMsg('Failed')
      setTimeout(() => setCopyMsg(''), 2000)
    })
  }

  if (!scriptName) {
    return (
      <div className="script-detail-empty">
        <p>Select a script from the list to view its details.</p>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="script-detail-empty">
        <p>Loading {scriptName}...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="script-detail-empty">
        <p className="script-detail-error">{error}</p>
      </div>
    )
  }

  if (!script) {
    return (
      <div className="script-detail-empty">
        <p>Script not found.</p>
      </div>
    )
  }

  return (
    <div className="script-detail-panel">
      {/* Header */}
      <div className="script-detail-header">
        <div className="script-detail-meta">
          <h2 className="script-detail-name">{script.name}</h2>
          {script.updatedAt && (
            <span className="script-detail-updated">
              Last modified: {new Date(script.updatedAt).toLocaleString()}
            </span>
          )}
        </div>
        <div className="script-detail-actions">
          {!isEditing && (
            <>
              <button
                className="script-action-btn"
                onClick={handleCopy}
                title="Copy code"
              >
                <Copy size={14} />
                <span>{copyMsg || 'Copy'}</span>
              </button>
              <button
                className="script-action-btn"
                onClick={handleExport}
                title="Export as .py"
              >
                <Download size={14} />
                <span>Export</span>
              </button>
              <button
                className="script-action-btn primary"
                onClick={handleEdit}
                title="Edit script"
              >
                <Edit2 size={14} />
                <span>Edit</span>
              </button>
            </>
          )}
          {isEditing && (
            <>
              <button
                className="script-action-btn"
                onClick={handleCancelEdit}
                title="Cancel editing"
              >
                Cancel
              </button>
              <button
                className="script-action-btn primary"
                onClick={handleSave}
                disabled={saving}
                title="Save script"
              >
                <Save size={14} />
                <span>{saving ? 'Saving...' : 'Save'}</span>
              </button>
            </>
          )}
        </div>
      </div>

      {/* Description */}
      <div className="script-detail-description-row">
        <label className="script-detail-label">Description</label>
        {isEditing ? (
          <input
            type="text"
            className="script-detail-desc-input"
            value={editDescription}
            onChange={e => setEditDescription(e.target.value)}
            placeholder="Optional description..."
          />
        ) : (
          <span className="script-detail-desc-text">
            {script.description || <em>No description</em>}
          </span>
        )}
      </div>

      {/* Code area */}
      <div className="script-detail-code-area">
        {isEditing ? (
          <textarea
            className="script-detail-editor"
            value={editCode}
            onChange={e => setEditCode(e.target.value)}
            spellCheck={false}
            autoComplete="off"
            autoCorrect="off"
            autoCapitalize="off"
          />
        ) : (
          <pre className="script-detail-code-block">
            <code>{script.code || '# (empty script)'}</code>
          </pre>
        )}
      </div>
    </div>
  )
}

export default ScriptDetailPanel
