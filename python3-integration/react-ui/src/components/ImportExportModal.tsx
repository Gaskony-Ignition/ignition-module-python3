import { useState, useRef } from 'react'
import { X, Upload } from 'lucide-react'

interface ImportExportModalProps {
  isOpen: boolean
  onClose: () => void
  gatewayUrl: string
  onImported: () => void
}

function ImportExportModal({ isOpen, onClose, gatewayUrl, onImported }: ImportExportModalProps) {
  const [scriptName, setScriptName] = useState<string>('')
  const [description, setDescription] = useState<string>('')
  const [fileContent, setFileContent] = useState<string>('')
  const [fileName, setFileName] = useState<string>('')
  const [status, setStatus] = useState<'idle' | 'saving' | 'success' | 'error'>('idle')
  const [errorMsg, setErrorMsg] = useState<string>('')
  const fileInputRef = useRef<HTMLInputElement>(null)

  if (!isOpen) return null

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return

    const baseName = file.name.replace(/\.py$/i, '')
    setFileName(file.name)
    if (!scriptName) {
      setScriptName(baseName)
    }

    const reader = new FileReader()
    reader.onload = (ev) => {
      setFileContent(ev.target?.result as string || '')
    }
    reader.onerror = () => {
      setErrorMsg('Failed to read file.')
      setStatus('error')
    }
    reader.readAsText(file)
  }

  const handleImport = async () => {
    if (!scriptName.trim()) {
      setErrorMsg('Script name is required.')
      setStatus('error')
      return
    }
    if (!fileContent) {
      setErrorMsg('Please select a .py file.')
      setStatus('error')
      return
    }

    setStatus('saving')
    setErrorMsg('')

    try {
      const res = await fetch(`${gatewayUrl}/api/v1/scripts/save`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: scriptName.trim(),
          code: fileContent,
          description: description.trim(),
        }),
      })

      if (!res.ok) throw new Error(`HTTP ${res.status}`)

      setStatus('success')
      onImported()

      // Auto-close after 1.5 seconds on success
      setTimeout(() => {
        handleClose()
      }, 1500)
    } catch (err) {
      setErrorMsg(`Import failed: ${err}`)
      setStatus('error')
    }
  }

  const handleClose = () => {
    setScriptName('')
    setDescription('')
    setFileContent('')
    setFileName('')
    setStatus('idle')
    setErrorMsg('')
    if (fileInputRef.current) fileInputRef.current.value = ''
    onClose()
  }

  return (
    <div
      className="modal-overlay"
      onClick={(e) => { if (e.target === e.currentTarget) handleClose() }}
      role="dialog"
      aria-modal="true"
      aria-label="Import Python Script"
    >
      <div className="modal-dialog">
        <div className="modal-header">
          <h3 className="modal-title">
            <Upload size={16} />
            Import Python Script
          </h3>
          <button
            className="modal-close-btn"
            onClick={handleClose}
            aria-label="Close"
          >
            <X size={16} />
          </button>
        </div>

        <div className="modal-body">
          {/* File picker */}
          <div className="modal-field">
            <label className="modal-label">Select .py file</label>
            <div className="modal-file-row">
              <input
                ref={fileInputRef}
                type="file"
                accept=".py"
                onChange={handleFileChange}
                className="modal-file-input"
                id="import-file-input"
              />
              <label htmlFor="import-file-input" className="modal-file-btn">
                <Upload size={13} />
                Choose file
              </label>
              {fileName && (
                <span className="modal-file-name">{fileName}</span>
              )}
            </div>
          </div>

          {/* Script name */}
          <div className="modal-field">
            <label className="modal-label" htmlFor="import-script-name">Script name *</label>
            <input
              id="import-script-name"
              type="text"
              className="modal-input"
              value={scriptName}
              onChange={e => setScriptName(e.target.value)}
              placeholder="e.g. my_script"
              autoFocus
            />
          </div>

          {/* Description */}
          <div className="modal-field">
            <label className="modal-label" htmlFor="import-description">Description (optional)</label>
            <input
              id="import-description"
              type="text"
              className="modal-input"
              value={description}
              onChange={e => setDescription(e.target.value)}
              placeholder="Brief description..."
            />
          </div>

          {/* Status messages */}
          {status === 'error' && (
            <div className="modal-status error">{errorMsg}</div>
          )}
          {status === 'success' && (
            <div className="modal-status success">Script imported successfully!</div>
          )}
        </div>

        <div className="modal-footer">
          <button className="modal-btn" onClick={handleClose}>
            Cancel
          </button>
          <button
            className="modal-btn primary"
            onClick={handleImport}
            disabled={status === 'saving' || !fileContent}
          >
            {status === 'saving' ? 'Importing...' : 'Import'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default ImportExportModal
