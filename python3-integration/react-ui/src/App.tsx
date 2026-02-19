import { useState, useEffect, useRef, useCallback } from 'react'
import Sidebar from './components/Sidebar'
import ErrorBoundary from './components/ErrorBoundary'
import GlobalStatusBar from './components/GlobalStatusBar'
import DashboardView from './components/DashboardView'
import IDEView from './components/IDEView'
import TerminalView from './components/TerminalView'
import ScriptsView from './components/ScriptsView'
import VersionsView from './components/VersionsView'
import PackagesView from './components/PackagesView'
import DiagnosticsView from './components/DiagnosticsView'
import './styles.css'
import './App.css'

const SINGLETON_KEY = '__python3_ide_singleton_v1__'
const INSTANCE_ID = `inst-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`

let IS_PRIMARY_MODULE = false
try {
  Object.defineProperty(window, SINGLETON_KEY, {
    value: INSTANCE_ID,
    writable: false,
    configurable: false,
    enumerable: false
  })
  IS_PRIMARY_MODULE = true
} catch {
  IS_PRIMARY_MODULE = false
}

const GATEWAY_URL = window.location.origin + '/data/python3integration'

function App() {
  if (!IS_PRIMARY_MODULE) {
    return null
  }
  return <AppContent />
}

function AppContent() {
  const [activeView, setActiveView] = useState<string>('dashboard')
  const [connectionStatus, setConnectionStatus] = useState<'connecting' | 'connected' | 'disconnected'>('connecting')
  const [pythonVersion, setPythonVersion] = useState<string>('')
  const healthCheckIntervalRef = useRef<number | null>(null)
  const healthCheckAttempts = useRef<number>(0)
  const currentHealthInterval = useRef<number>(5000)

  const checkHealth = useCallback(async () => {
    try {
      const res = await fetch(`${GATEWAY_URL}/api/v1/health`, { signal: AbortSignal.timeout(5000) })
      if (res.ok) {
        const data = await res.json()
        const payload = data.data || data
        setConnectionStatus('connected')
        healthCheckAttempts.current = 0
        currentHealthInterval.current = 15000
        if (payload.pythonVersion) {
          setPythonVersion(payload.pythonVersion)
        }
      } else {
        throw new Error(`HTTP ${res.status}`)
      }
    } catch {
      healthCheckAttempts.current++
      setConnectionStatus(healthCheckAttempts.current > 2 ? 'disconnected' : 'connecting')
      currentHealthInterval.current = Math.min(5000 * Math.pow(2, healthCheckAttempts.current), 30000)
    }

    if (healthCheckIntervalRef.current) {
      window.clearTimeout(healthCheckIntervalRef.current)
    }
    healthCheckIntervalRef.current = window.setTimeout(checkHealth, currentHealthInterval.current)
  }, [])

  useEffect(() => {
    checkHealth()
    return () => {
      if (healthCheckIntervalRef.current) {
        window.clearTimeout(healthCheckIntervalRef.current)
      }
    }
  }, [checkHealth])

  const renderView = () => {
    switch (activeView) {
      case 'dashboard':
        return <DashboardView gatewayUrl={GATEWAY_URL} onNavigate={setActiveView} />
      case 'ide':
        return <IDEView gatewayUrl={GATEWAY_URL} />
      case 'terminal':
        return <TerminalView gatewayUrl={GATEWAY_URL} />
      case 'scripts':
        return <ScriptsView gatewayUrl={GATEWAY_URL} />
      case 'versions':
        return <VersionsView gatewayUrl={GATEWAY_URL} />
      case 'packages':
        return <PackagesView gatewayUrl={GATEWAY_URL} />
      case 'diagnostics':
        return <DiagnosticsView gatewayUrl={GATEWAY_URL} />
      default:
        return <DashboardView gatewayUrl={GATEWAY_URL} onNavigate={setActiveView} />
    }
  }

  return (
    <div className="app-wrapper">
      <div className="app-container">
        {connectionStatus === 'disconnected' && (
          <div className="connection-banner">
            Unable to connect to Python 3 Integration gateway. Retrying...
          </div>
        )}
        <div className="app-content">
          <Sidebar activeView={activeView} onNavigate={setActiveView} gatewayUrl={GATEWAY_URL} />
          <main className="main-content">
            <ErrorBoundary>
              {renderView()}
            </ErrorBoundary>
          </main>
        </div>
        <GlobalStatusBar connectionStatus={connectionStatus} pythonVersion={pythonVersion} gatewayUrl={GATEWAY_URL} />
      </div>
    </div>
  )
}

export default App
