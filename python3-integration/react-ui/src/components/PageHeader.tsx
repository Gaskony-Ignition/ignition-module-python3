import { useState, useEffect } from 'react'
import { Code2, Wifi, WifiOff } from 'lucide-react'
import './PageHeader.css'

interface PageHeaderProps {
  gatewayUrl: string
  connectionStatus: 'connecting' | 'connected' | 'disconnected' | 'auth_required'
}

function PageHeader({ gatewayUrl, connectionStatus }: PageHeaderProps) {
  const FALLBACK_VERSION = '3.6.1'
  const [moduleVersion, setModuleVersion] = useState<string>(FALLBACK_VERSION)

  useEffect(() => {
    if (!gatewayUrl) return
    fetch(`${gatewayUrl}/api/v1/version`, { credentials: 'same-origin' })
      .then(res => res.ok ? res.json() : null)
      .then(raw => {
        const data = raw?.data || raw
        if (data?.moduleVersion) {
          setModuleVersion(data.moduleVersion)
        }
      })
      .catch(() => {})
  }, [gatewayUrl])

  const isConnected = connectionStatus === 'connected'

  return (
    <div className="page-header">
      <div className="page-header__left">
        <Code2 size={16} className="page-header__icon" />
        <span className="page-header__title">Python 3 Integration</span>
      </div>
      <div className="page-header__right">
        <div className={`page-header__status ${isConnected ? 'page-header__status--connected' : 'page-header__status--disconnected'}`}>
          {isConnected ? <Wifi size={12} /> : <WifiOff size={12} />}
          <span>{isConnected ? 'Connected' : connectionStatus === 'connecting' ? 'Connecting' : 'Disconnected'}</span>
        </div>
        <span className="page-header__version">v{moduleVersion}</span>
      </div>
    </div>
  )
}

export default PageHeader
