interface PoolStats {
  poolSize: number
  activeExecutors: number
  availableExecutors: number
  borrowedCount: number
  healthCheckStatus: string
}

interface PoolStatsPanelProps {
  stats: PoolStats | null
}

function PoolStatsPanel({ stats }: PoolStatsPanelProps) {
  if (!stats) {
    return (
      <div className="diag-panel">
        <h3 className="diag-panel__title">Process Pool</h3>
        <p className="diag-panel__empty">No pool data available.</p>
      </div>
    )
  }

  const { poolSize, activeExecutors, availableExecutors, borrowedCount, healthCheckStatus } = stats

  const activePct = poolSize > 0 ? Math.round((activeExecutors / poolSize) * 100) : 0
  const availPct = poolSize > 0 ? Math.round((availableExecutors / poolSize) * 100) : 0

  // Bar fill colour: green if low utilisation, yellow if > 60%, red if > 85%
  const barClass =
    activePct >= 85
      ? 'pool-util-fill--danger'
      : activePct >= 60
      ? 'pool-util-fill--warn'
      : 'pool-util-fill--ok'

  const isHealthy =
    healthCheckStatus?.toLowerCase() === 'healthy' ||
    healthCheckStatus?.toLowerCase() === 'ok' ||
    healthCheckStatus?.toLowerCase() === 'running'

  return (
    <div className="diag-panel">
      <h3 className="diag-panel__title">Process Pool</h3>

      {/* Utilisation bar */}
      <div className="pool-util">
        <div className="pool-util__labels">
          <span className="pool-util__label">Utilisation</span>
          <span className="pool-util__pct">{activePct}%</span>
        </div>
        <div className="pool-util__bar">
          <div
            className={`pool-util__fill pool-util__fill--active ${barClass}`}
            style={{ width: `${activePct}%` }}
          />
          <div
            className="pool-util__fill pool-util__fill--avail"
            style={{ width: `${availPct}%`, marginLeft: `${activePct}%` }}
          />
        </div>
        <div className="pool-util__legend">
          <span className="pool-legend pool-legend--active">Active</span>
          <span className="pool-legend pool-legend--avail">Available</span>
        </div>
      </div>

      {/* Stats rows */}
      <div className="diag-stats-grid">
        <div className="diag-stat">
          <span className="diag-stat__label">Pool Size</span>
          <span className="diag-stat__value">{poolSize}</span>
        </div>
        <div className="diag-stat">
          <span className="diag-stat__label">Active</span>
          <span className="diag-stat__value">{activeExecutors}</span>
        </div>
        <div className="diag-stat">
          <span className="diag-stat__label">Available</span>
          <span className="diag-stat__value">{availableExecutors}</span>
        </div>
        <div className="diag-stat">
          <span className="diag-stat__label">Total Borrowed</span>
          <span className="diag-stat__value">{borrowedCount}</span>
        </div>
      </div>

      {/* Health check status */}
      <div className="pool-health-row">
        <span
          className={`pool-health-dot ${isHealthy ? 'pool-health-dot--healthy' : 'pool-health-dot--down'}`}
        />
        <span className="pool-health-label">
          Health check: <strong>{healthCheckStatus || 'Unknown'}</strong>
        </span>
      </div>
    </div>
  )
}

export default PoolStatsPanel
