import { ReactNode } from 'react'
import { Play, Trash2, Save, Loader } from 'lucide-react'

interface Props {
  onExecute: () => void
  onClear: () => void
  onSave: () => void
  isExecuting: boolean
  children?: ReactNode
}

function ExecutionToolbar({ onExecute, onClear, onSave, isExecuting, children }: Props) {
  return (
    <div className="execution-toolbar">
      {/* Run button */}
      <button
        className={`exec-toolbar-btn exec-toolbar-btn--run ${isExecuting ? 'exec-toolbar-btn--running' : ''}`}
        onClick={onExecute}
        disabled={isExecuting}
        title="Run script (Ctrl+Enter)"
        aria-label="Run script"
      >
        {isExecuting ? (
          <>
            <Loader size={14} className="spin-sm" />
            Running...
          </>
        ) : (
          <>
            <Play size={14} />
            Run
          </>
        )}
      </button>

      <div className="exec-toolbar-separator" />

      {/* Version selector slot (children) */}
      {children && <div className="exec-toolbar-slot">{children}</div>}

      <div className="exec-toolbar-spacer" />

      {/* Save button */}
      <button
        className="exec-toolbar-btn exec-toolbar-btn--secondary"
        onClick={onSave}
        disabled={isExecuting}
        title="Save script"
        aria-label="Save script"
      >
        <Save size={13} />
        Save
      </button>

      {/* Clear button */}
      <button
        className="exec-toolbar-btn exec-toolbar-btn--ghost"
        onClick={onClear}
        title="Clear editor and output"
        aria-label="Clear editor"
      >
        <Trash2 size={13} />
        Clear
      </button>
    </div>
  )
}

export default ExecutionToolbar
