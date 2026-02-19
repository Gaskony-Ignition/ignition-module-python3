import { useState, useRef, useCallback, useEffect } from 'react'
import CodeEditor from './CodeEditor'
import OutputPanel from './OutputPanel'
import ExecutionToolbar from './ExecutionToolbar'
import VersionSelector from './VersionSelector'
import { apiPost } from '../utils/api'
import './IDEView.css'

interface Props {
  gatewayUrl: string
}

const DEFAULT_CODE = `# Python 3 Integration IDE
# Press Ctrl+Enter or click Run to execute

print("Hello from Python 3!")
`

const DEFAULT_OUTPUT_HEIGHT = 220

function IDEView({ gatewayUrl }: Props) {
  const [code, setCode] = useState(DEFAULT_CODE)
  const [output, setOutput] = useState('')
  const [error, setError] = useState('')
  const [executionTime, setExecutionTime] = useState<number | null>(null)
  const [isExecuting, setIsExecuting] = useState(false)
  const [selectedVersion, setSelectedVersion] = useState('')

  // Resize state
  const [outputHeight, setOutputHeight] = useState(DEFAULT_OUTPUT_HEIGHT)
  const isDragging = useRef(false)
  const dragStartY = useRef(0)
  const dragStartHeight = useRef(DEFAULT_OUTPUT_HEIGHT)
  const containerRef = useRef<HTMLDivElement>(null)

  // ---- Execute ----
  const handleExecute = useCallback(async () => {
    if (isExecuting) return
    setIsExecuting(true)
    setOutput('')
    setError('')
    setExecutionTime(null)

    try {
      const body: Record<string, unknown> = { code, variables: {} }
      if (selectedVersion) body.version = selectedVersion

      const data = await apiPost<{
        success: boolean
        result?: unknown
        output?: unknown
        error?: string
        traceback?: string
        executionTimeMs?: number
        executionTime?: number
      }>('/api/v1/exec', body, 60_000)

      if (data.success) {
        const resultText = data.result !== undefined
          ? String(data.result)
          : data.output !== undefined
            ? String(data.output)
            : ''
        setOutput(resultText)
        const timing = data.executionTimeMs ?? data.executionTime ?? null
        setExecutionTime(typeof timing === 'number' ? timing : null)
      } else {
        let errMsg = data.error ?? 'Execution failed'
        if (data.traceback) errMsg = errMsg + '\n\n' + data.traceback
        setError(errMsg)
        const timing = data.executionTimeMs ?? data.executionTime ?? null
        setExecutionTime(typeof timing === 'number' ? timing : null)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Network error')
    } finally {
      setIsExecuting(false)
    }
  }, [code, isExecuting, selectedVersion])

  // ---- Save ----
  const handleSave = useCallback(async () => {
    const name = window.prompt('Script name:', 'My Script')
    if (!name || !name.trim()) return

    const description = window.prompt('Description (optional):', '') ?? ''

    try {
      const data = await apiPost<{ success?: boolean; error?: string; message?: string }>(
        '/api/v1/scripts/save',
        { name: name.trim(), code, description },
        10_000,
      )
      if (data.success !== false) {
        setOutput(prev => (prev ? prev + '\n' : '') + `[Saved as "${name.trim()}"]`)
      } else {
        setError(`Save failed: ${data.error ?? data.message ?? 'Unknown error'}`)
      }
    } catch (err) {
      setError(`Save failed: ${err instanceof Error ? err.message : 'Network error'}`)
    }
  }, [code])

  // ---- Clear ----
  const handleClear = useCallback(() => {
    setOutput('')
    setError('')
    setExecutionTime(null)
  }, [])

  const handleClearAll = useCallback(() => {
    setCode(DEFAULT_CODE)
    setOutput('')
    setError('')
    setExecutionTime(null)
  }, [])

  // ---- Resize handle drag ----
  const handleResizeMouseDown = useCallback((e: React.MouseEvent) => {
    e.preventDefault()
    isDragging.current = true
    dragStartY.current = e.clientY
    dragStartHeight.current = outputHeight
    document.body.style.cursor = 'row-resize'
    document.body.style.userSelect = 'none'
  }, [outputHeight])

  useEffect(() => {
    const onMouseMove = (e: MouseEvent) => {
      if (!isDragging.current || !containerRef.current) return
      const deltaY = dragStartY.current - e.clientY
      const containerHeight = containerRef.current.getBoundingClientRect().height
      const newHeight = Math.max(80, Math.min(containerHeight - 150, dragStartHeight.current + deltaY))
      setOutputHeight(newHeight)
    }

    const onMouseUp = () => {
      if (!isDragging.current) return
      isDragging.current = false
      document.body.style.cursor = ''
      document.body.style.userSelect = ''
    }

    document.addEventListener('mousemove', onMouseMove)
    document.addEventListener('mouseup', onMouseUp)
    return () => {
      document.removeEventListener('mousemove', onMouseMove)
      document.removeEventListener('mouseup', onMouseUp)
    }
  }, [])

  return (
    <div className="ide-view" ref={containerRef}>
      {/* Toolbar */}
      <ExecutionToolbar
        onExecute={handleExecute}
        onClear={handleClearAll}
        onSave={handleSave}
        isExecuting={isExecuting}
      >
        <VersionSelector
          gatewayUrl={gatewayUrl}
          selectedVersion={selectedVersion}
          onVersionChange={setSelectedVersion}
        />
      </ExecutionToolbar>

      {/* Editor */}
      <div className="ide-editor-area">
        <CodeEditor
          value={code}
          onChange={setCode}
          onExecute={handleExecute}
        />
      </div>

      {/* Resize handle */}
      <div
        className="ide-resize-handle"
        onMouseDown={handleResizeMouseDown}
        title="Drag to resize output panel"
        role="separator"
        aria-label="Resize output panel"
      />

      {/* Output panel */}
      <div
        className="ide-output-area"
        style={{ height: outputHeight }}
      >
        <OutputPanel
          output={output}
          error={error}
          executionTime={executionTime}
          isExecuting={isExecuting}
          onClear={handleClear}
        />
      </div>
    </div>
  )
}

export default IDEView
