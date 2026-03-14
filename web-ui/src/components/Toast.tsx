import { useEffect } from 'react'
import { X } from 'lucide-react'
import './Toast.css'

interface ToastProps {
  message: string
  type: 'success' | 'error'
  onClose: () => void
}

function Toast({ message, type, onClose }: ToastProps) {
  useEffect(() => {
    const duration = type === 'success' ? 4000 : 6000
    const timer = window.setTimeout(onClose, duration)
    return () => window.clearTimeout(timer)
  }, [type, onClose])

  return (
    <div className={`toast toast-${type}`}>
      <span>{message}</span>
      <button className="toast-close" onClick={onClose} aria-label="Close">
        <X size={14} />
      </button>
    </div>
  )
}

export default Toast
