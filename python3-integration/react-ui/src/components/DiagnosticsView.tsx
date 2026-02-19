import { useState, useEffect, useRef, useCallback } from 'react'
import { RefreshCw, ChevronDown, ChevronRight, AlertTriangle, CheckCircle, XCircle, Loader } from 'lucide-react'
import PoolStatsPanel from './PoolStatsPanel'
import MetricsPanel from './MetricsPanel'
import './DiagnosticsView.css'

const AUTO_REFRESH_MS = 10_000

interface Props {
  gatewayUrl: string
}

function DiagnosticsView({ gatewayUrl }: Props) {
  const [health, setHealth] = useState<Record<string, unknown> | null>(null)
  const [poolStats, setPoolStats] = useState<Record<string, unknown> | null>(null)
  const [diagnostics, setDiagnostics] = useState<Record<string, unknown> | null>(null)
  const [metrics, setMetrics] = useState<Record<string, unknown> | null>(null)
  const [alerts, setAlerts] = useState<unknown[] | null>(null)
  const [circuitBreaker, setCircuitBreaker] = useState<Record<string, unknown> | null>(null)
  const [loading, setLoading] = useState(true)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)
  const [secondsAgo, setSecondsAgo] = useState(0)
  const [rawExpanded, setRawExpanded] = useState(false)
  const [refreshing, setRefreshing] = useState(false)

  const timerRef = useRef<number | null>(null)
  const clockRef = useRef<number | null>(null)

  const fetchAll = useCallback(async () => {
    const endpoints = [
      `${gatewayUrl}/api/v1/health`,
      `${gatewayUrl}/api/v1/pool-stats`,
      `${gatewayUrl}/api/v1/diagnostics`,
      `${gatewayUrl}/api/v1/metrics`,
      `${gatewayUrl}/api/v1/monitoring/alerts`,
      `${gatewayUrl}/api/v1/monitoring/circuit-breaker`,
    ]

    const results = await Promise.allSettled(
      endpoints.map((url) =>
        fetch(url)
          .then((r) => (r.ok ? r.json() : null))
          .then((raw) => (raw ? raw.data || raw : null))
          .catch(() => null)
      )
    )

    const [h, ps, diag, m, a, cb] = results.map((r) =>
      r.status === 'fulfilled' ? r.value : null
    )

    setHealth(h as Record<string, unknown> | null)
    setPoolStats(ps as Record<string, unknown> | null)
    setDiagnostics(diag as Record<string, unknown> | null)
    setMetrics(m as Record<string, unknown> | null)

    // alerts: may be { alerts: [...] } or plain array
    if (a) {
      setAlerts(Array.isArray(a) ? a : (a as Record<string, unknown[]>).alerts || [])
    } else {
      setAlerts(null)
    }

    setCircuitBreaker(cb as Record<string, unknown> | null)
    setLastUpdated(new Date())
    setSecondsAgo(0)
  }, [gatewayUrl])

  // Auto-refresh loop
  useEffect(() => {
    let mounted = true

    const run = async () => {
      setLoading(true)
      await fetchAll()
      if (mounted) setLoading(false)
    }

    run()

    timerRef.current = window.setInterval(async () => {
      if (mounted) await fetchAll()
    }, AUTO_REFRESH_MS)

    return () => {
      mounted = false
      if (timerRef.current) window.clearInterval(timerRef.current)
      if (clockRef.current) window.clearInterval(clockRef.current)
    }
  }, [fetchAll])

  // "Last updated X seconds ago" ticker
  useEffect(() => {
    if (clockRef.current) window.clearInterval(clockRef.current)
    clockRef.current = window.setInterval(() => {
      setSecondsAgo((s) => s + 1)
    }, 1000)
    return () => {
      if (clockRef.current) window.clearInterval(clockRef.current)
    }
  }, [lastUpdated])

  const handleRefresh = async () => {
    setRefreshing(true)
    await fetchAll()
    setRefreshing(false)
  }

  // --- Health banner ---
  const overallStatus: string =
    (health?.status as string) ||
    (health?.overall as string) ||
    (health?.healthy ? 'HEALTHY' : health?.healthy === false ? 'DOWN' : 'UNKNOWN')
  const statusNorm = overallStatus.toUpperCase()
  const bannerClass =
    statusNorm === 'HEALTHY' || statusNorm === 'OK' || statusNorm === 'UP'
      ? 'diag-banner--healthy'
      : statusNorm === 'DEGRADED'
      ? 'diag-banner--degraded'
      : statusNorm === 'DOWN' || statusNorm === 'ERROR'
      ? 'diag-banner--down'
      : 'diag-banner--unknown'

  // --- Pool stats normalization ---
  const normalizedPool = poolStats
    ? {
        poolSize: (poolStats.poolSize as number) ?? (poolStats.totalExecutors as number) ?? 0,
        activeExecutors: (poolStats.activeExecutors as number) ?? (poolStats.busy as number) ?? 0,
        availableExecutors:
          (poolStats.availableExecutors as number) ?? (poolStats.available as number) ?? 0,
        borrowedCount: (poolStats.borrowedCount as number) ?? (poolStats.totalBorrowed as number) ?? 0,
        healthCheckStatus:
          (poolStats.healthCheckStatus as string) ??
          (poolStats.status as string) ??
          'Unknown',
      }
    : null

  // --- Metrics normalization ---
  const normalizedMetrics = metrics
    ? {
        totalExecutions: (metrics.totalExecutions as number) ?? 0,
        successfulExecutions: (metrics.successfulExecutions as number) ?? 0,
        failedExecutions: (metrics.failedExecutions as number) ?? 0,
        averageLatencyMs:
          (metrics.averageLatencyMs as number) ?? (metrics.avgLatencyMs as number) ?? 0,
        maxLatencyMs: (metrics.maxLatencyMs as number) ?? 0,
        errorRate: (metrics.errorRate as number) ?? 0,
      }
    : null

  // --- Circuit breaker ---
  const cbState: string = circuitBreaker
    ? (circuitBreaker.state as string) || (circuitBreaker.status as string) || 'UNKNOWN'
    : 'UNKNOWN'
  const cbClass =
    cbState.toUpperCase() === 'CLOSED'
      ? 'cb-state--closed'
      : cbState.toUpperCase() === 'OPEN'
      ? 'cb-state--open'
      : cbState.toUpperCase() === 'HALF_OPEN'
      ? 'cb-state--half-open'
      : 'cb-state--unknown'

  return (
    <div className="diagnostics-view">
      {/* Header */}
      <div className="diag-header">
        <div className="diag-header__left">
          <h2 className="diag-header__title">Diagnostics</h2>
          {lastUpdated && (
            <span className="diag-header__updated">
              Last updated: {secondsAgo}s ago
            </span>
          )}
        </div>
        <button
          className={`diag-refresh-btn ${refreshing ? 'spinning' : ''}`}
          onClick={handleRefresh}
          disabled={refreshing || loading}
          title="Refresh diagnostics"
        >
          {loading || refreshing ? (
            <Loader size={13} className="diag-refresh-btn__spinner" />
          ) : (
            <RefreshCw size={13} />
          )}
          Refresh
        </button>
      </div>

      {/* Scrollable body */}
      <div className="diag-body">
        {/* 1. Health banner */}
        <div className={`diag-banner ${bannerClass}`}>
          {bannerClass === 'diag-banner--healthy' ? (
            <CheckCircle size={16} />
          ) : bannerClass === 'diag-banner--down' ? (
            <XCircle size={16} />
          ) : (
            <AlertTriangle size={16} />
          )}
          System Status: <strong>{overallStatus}</strong>
          {health?.message != null && (
            <span className="diag-banner__message"> — {String(health.message)}</span>
          )}
        </div>

        {/* 2. Pool Stats */}
        <PoolStatsPanel stats={normalizedPool} />

        {/* 3. Execution Metrics */}
        <MetricsPanel metrics={normalizedMetrics} />

        {/* 4. Circuit Breaker */}
        <div className="diag-panel">
          <h3 className="diag-panel__title">Circuit Breaker</h3>
          {circuitBreaker ? (
            <div className="cb-card">
              <span className={`cb-state ${cbClass}`}>{cbState}</span>
              <div className="cb-details">
                {(circuitBreaker.failureCount !== undefined) && (
                  <div className="cb-detail-row">
                    <span className="cb-detail-label">Failure count</span>
                    <span className="cb-detail-value">
                      {String(circuitBreaker.failureCount)}
                    </span>
                  </div>
                )}
                {(circuitBreaker.failureThreshold !== undefined) && (
                  <div className="cb-detail-row">
                    <span className="cb-detail-label">Failure threshold</span>
                    <span className="cb-detail-value">
                      {String(circuitBreaker.failureThreshold)}
                    </span>
                  </div>
                )}
                {(circuitBreaker.lastFailureTime !== undefined) && (
                  <div className="cb-detail-row">
                    <span className="cb-detail-label">Last failure</span>
                    <span className="cb-detail-value">
                      {String(circuitBreaker.lastFailureTime)}
                    </span>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <p className="diag-panel__empty">No circuit breaker data available.</p>
          )}
        </div>

        {/* 5. Active Alerts */}
        <div className="diag-panel">
          <h3 className="diag-panel__title">Active Alerts</h3>
          {alerts === null ? (
            <p className="diag-panel__empty">No alerts data available.</p>
          ) : alerts.length === 0 ? (
            <div className="diag-no-alerts">
              <CheckCircle size={16} />
              No active alerts
            </div>
          ) : (
            <div className="diag-alerts-list">
              {alerts.map((alert, i) => {
                const a = alert as Record<string, unknown>
                return (
                  <div key={i} className="diag-alert-item">
                    <AlertTriangle size={13} className="diag-alert-item__icon" />
                    <div className="diag-alert-item__content">
                      <span className="diag-alert-item__name">
                        {(a.name as string) || (a.type as string) || `Alert ${i + 1}`}
                      </span>
                      {a.message != null && (
                        <span className="diag-alert-item__message">
                          {String(a.message)}
                        </span>
                      )}
                    </div>
                    {a.severity != null && (
                      <span className="diag-alert-item__severity">
                        {String(a.severity)}
                      </span>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </div>

        {/* 6. Raw Diagnostics (collapsible) */}
        <div className="diag-panel">
          <button
            className="diag-raw-toggle"
            onClick={() => setRawExpanded((v) => !v)}
          >
            {rawExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            Raw Diagnostics
          </button>
          {rawExpanded && (
            <pre className="diag-raw-json">
              {diagnostics
                ? JSON.stringify(diagnostics, null, 2)
                : '(no diagnostics data)'}
            </pre>
          )}
        </div>
      </div>
    </div>
  )
}

export default DiagnosticsView
