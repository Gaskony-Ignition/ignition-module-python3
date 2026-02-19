import { useState, useEffect, useRef, useCallback } from 'react'
import { RefreshCw, ChevronDown, ChevronRight, Loader } from 'lucide-react'
import { apiPost } from '../utils/api'
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
    ]

    const results = await Promise.allSettled(
      endpoints.map((url) =>
        fetch(url, { credentials: 'same-origin' })
          .then((r) => (r.ok ? r.json() : null))
          .then((raw) => (raw ? raw.data || raw : null))
          .catch(() => null)
      )
    )

    const [h, ps, diag, m] = results.map((r) =>
      r.status === 'fulfilled' ? r.value : null
    )

    setHealth(h as Record<string, unknown> | null)
    setPoolStats(ps as Record<string, unknown> | null)
    setDiagnostics(diag as Record<string, unknown> | null)
    setMetrics(m as Record<string, unknown> | null)
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

  const handleResizePool = async (newSize: number) => {
    try {
      await apiPost('/api/v1/pool-size', { size: newSize })
      await fetchAll()
    } catch (err) {
      throw err
    }
  }

  // --- Health status ---
  const overallStatus: string =
    (health?.status as string) ||
    (health?.overall as string) ||
    (health?.healthy ? 'HEALTHY' : health?.healthy === false ? 'DOWN' : 'UNKNOWN')
  const statusNorm = overallStatus.toUpperCase()

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

  return (
    <div className="diagnostics-view">
      {/* Header */}
      <div className="diag-header">
        <div className="diag-header__left">
          <h2 className="diag-header__title">
            Diagnostics
            <span
              className={`diag-status-dot ${
                statusNorm === 'HEALTHY' || statusNorm === 'OK' || statusNorm === 'UP'
                  ? 'diag-status-dot--healthy'
                  : statusNorm === 'DEGRADED'
                  ? 'diag-status-dot--degraded'
                  : statusNorm === 'DOWN' || statusNorm === 'ERROR'
                  ? 'diag-status-dot--down'
                  : 'diag-status-dot--unknown'
              }`}
              title={`System status: ${overallStatus}`}
            />
          </h2>
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
        {/* 1. Pool Stats */}
        <PoolStatsPanel stats={normalizedPool} onResizePool={handleResizePool} />

        {/* 3. Execution Metrics */}
        <MetricsPanel metrics={normalizedMetrics} />

        {/* 4. Raw Diagnostics (collapsible) */}
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
