import { useState, useEffect, useRef } from 'react'
import { X, Download } from 'lucide-react'

interface PackageInstallModalProps {
  isOpen: boolean
  onClose: () => void
  onInstall: (packageName: string, version?: string) => void
}

function PackageInstallModal({ isOpen, onClose, onInstall }: PackageInstallModalProps) {
  const [packageName, setPackageName] = useState('')
  const [versionConstraint, setVersionConstraint] = useState('')
  const nameInputRef = useRef<HTMLInputElement>(null)

  // Focus name input when modal opens; reset fields on close
  useEffect(() => {
    if (isOpen) {
      setPackageName('')
      setVersionConstraint('')
      setTimeout(() => nameInputRef.current?.focus(), 50)
    }
  }, [isOpen])

  if (!isOpen) return null

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = packageName.trim()
    if (!trimmed) return
    const ver = versionConstraint.trim() || undefined
    onInstall(trimmed, ver)
    onClose()
  }

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (e.target === e.currentTarget) onClose()
  }

  return (
    <div className="pkg-modal-backdrop" onClick={handleBackdropClick}>
      <div className="pkg-modal" role="dialog" aria-modal="true" aria-labelledby="pkg-modal-title">
        {/* Header */}
        <div className="pkg-modal__header">
          <h3 id="pkg-modal-title" className="pkg-modal__title">
            <Download size={16} />
            Install Package
          </h3>
          <button className="pkg-modal__close" onClick={onClose} title="Close">
            <X size={16} />
          </button>
        </div>

        {/* Form */}
        <form className="pkg-modal__body" onSubmit={handleSubmit}>
          <div className="pkg-modal__field">
            <label className="pkg-modal__label" htmlFor="pkg-name-input">
              Package name
            </label>
            <input
              id="pkg-name-input"
              ref={nameInputRef}
              className="pkg-modal__input"
              type="text"
              placeholder="e.g. requests"
              value={packageName}
              onChange={(e) => setPackageName(e.target.value)}
              autoComplete="off"
              spellCheck={false}
            />
          </div>

          <div className="pkg-modal__field">
            <label className="pkg-modal__label" htmlFor="pkg-version-input">
              Version constraint{' '}
              <span className="pkg-modal__label-hint">(optional)</span>
            </label>
            <input
              id="pkg-version-input"
              className="pkg-modal__input"
              type="text"
              placeholder="e.g. >=1.0.0 or ==2.28.0"
              value={versionConstraint}
              onChange={(e) => setVersionConstraint(e.target.value)}
              autoComplete="off"
              spellCheck={false}
            />
          </div>

          <div className="pkg-modal__preview">
            {packageName.trim()
              ? `pip install "${packageName.trim()}${versionConstraint.trim() ? versionConstraint.trim() : ''}"`
              : 'Enter a package name above'}
          </div>

          <div className="pkg-modal__footer">
            <button type="button" className="pkg-modal__btn pkg-modal__btn--cancel" onClick={onClose}>
              Cancel
            </button>
            <button
              type="submit"
              className="pkg-modal__btn pkg-modal__btn--install"
              disabled={!packageName.trim()}
            >
              <Download size={13} />
              Install
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default PackageInstallModal
