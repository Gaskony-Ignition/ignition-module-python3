import { Download, Trash2, Loader } from 'lucide-react'

interface PackageCardProps {
  name: string
  version?: string
  status: 'installed' | 'not_installed' | 'installing' | 'uninstalling'
  description?: string
  onInstall: () => void
  onUninstall: () => void
}

function PackageCard({
  name,
  version,
  status,
  description,
  onInstall,
  onUninstall,
}: PackageCardProps) {
  const isBusy = status === 'installing' || status === 'uninstalling'
  const isInstalled = status === 'installed'

  const statusLabel = {
    installed: 'Installed',
    not_installed: 'Not installed',
    installing: 'Installing…',
    uninstalling: 'Removing…',
  }[status]

  const statusClass = {
    installed: 'pkg-badge--installed',
    not_installed: 'pkg-badge--available',
    installing: 'pkg-badge--busy',
    uninstalling: 'pkg-badge--busy',
  }[status]

  return (
    <div className={`package-card ${isInstalled ? 'package-card--installed' : ''}`}>
      {/* Left: name + description */}
      <div className="package-card__info">
        <div className="package-card__name-row">
          <span className="package-card__name">{name}</span>
          {version && (
            <span className="package-card__version">{version}</span>
          )}
        </div>
        {description && (
          <p className="package-card__description">{description}</p>
        )}
      </div>

      {/* Right: status badge + action button */}
      <div className="package-card__actions">
        <span className={`pkg-badge ${statusClass}`}>{statusLabel}</span>

        {isInstalled ? (
          <button
            className="pkg-btn pkg-btn--danger"
            onClick={onUninstall}
            disabled={isBusy}
            title={`Uninstall ${name}`}
          >
            {isBusy ? (
              <Loader size={13} className="pkg-btn__spinner" />
            ) : (
              <Trash2 size={13} />
            )}
            {isBusy ? 'Removing…' : 'Uninstall'}
          </button>
        ) : (
          <button
            className="pkg-btn pkg-btn--primary"
            onClick={onInstall}
            disabled={isBusy}
            title={`Install ${name}`}
          >
            {isBusy ? (
              <Loader size={13} className="pkg-btn__spinner" />
            ) : (
              <Download size={13} />
            )}
            {status === 'installing' ? 'Installing…' : 'Install'}
          </button>
        )}
      </div>
    </div>
  )
}

export default PackageCard
