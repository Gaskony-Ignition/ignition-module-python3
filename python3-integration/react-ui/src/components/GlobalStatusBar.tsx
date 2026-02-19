import { useState, useEffect } from 'react'
import { Wifi, WifiOff, Loader } from 'lucide-react'
import './GlobalStatusBar.css'

interface Props {
  connectionStatus: 'connecting' | 'connected' | 'disconnected'
  pythonVersion: string
  gatewayUrl: string
}

function GlobalStatusBar({ connectionStatus, pythonVersion, gatewayUrl }: Props) {
  const [poolStats, setPoolStats] = useState<{ active: number; available: number; total: number } | null>(null)

  useEffect(() => {
    if (connectionStatus !== 'connected') return

    const fetchStats = async () => {
      try {
        const res = await fetch(`${gatewayUrl}/api/v1/pool-stats`)
        if (res.ok) {
          const raw = await res.json()
          const data = raw.data || raw
          setPoolStats({
            active: data.activeExecutors || data.borrowed || 0,
            available: data.availableExecutors || data.available || 0,
            total: data.totalExecutors || data.poolSize || 0,
          })
        }
      } catch { /* ignore */ }
    }

    fetchStats()
    const interval = setInterval(fetchStats, 15000)
    return () => clearInterval(interval)
  }, [connectionStatus, gatewayUrl])

  const statusIcon = () => {
    switch (connectionStatus) {
      case 'connected': return <Wifi size={12} />
      case 'connecting': return <Loader size={12} className="spin" />
      case 'disconnected': return <WifiOff size={12} />
    }
  }

  const statusColor = () => {
    switch (connectionStatus) {
      case 'connected': return 'var(--success)'
      case 'connecting': return 'var(--warning)'
      case 'disconnected': return 'var(--error)'
    }
  }

  return (
    <div className="global-status-bar">
      <div className="status-bar-left">
        <span className="status-indicator" style={{ color: statusColor() }}>
          {statusIcon()}
          <span className="status-text">{connectionStatus}</span>
        </span>
        {pythonVersion && <span className="status-item">Python {pythonVersion}</span>}
      </div>
      <div className="status-bar-right">
        {poolStats && <span className="status-item">Pool: {poolStats.active}/{poolStats.total} active</span>}
      </div>
    </div>
  )
}

export default GlobalStatusBar
