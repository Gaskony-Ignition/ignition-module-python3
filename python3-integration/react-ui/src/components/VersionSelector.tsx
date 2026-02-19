import { useState, useEffect } from 'react'
import { Code2 } from 'lucide-react'

interface VersionEntry {
  version?: string
  path?: string
  active?: boolean
  default?: boolean
}

interface Props {
  gatewayUrl: string
  selectedVersion: string
  onVersionChange: (v: string) => void
}

function VersionSelector({ gatewayUrl, selectedVersion, onVersionChange }: Props) {
  const [versions, setVersions] = useState<string[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    const load = async () => {
      try {
        const res = await fetch(`${gatewayUrl}/api/v1/versions`, {
          signal: AbortSignal.timeout(6000),
        })
        if (!res.ok) return
        const raw = await res.json()
        const data: unknown = raw?.data ?? raw

        let list: string[] = []
        if (Array.isArray((data as Record<string, unknown>)?.versions)) {
          list = ((data as Record<string, unknown>).versions as VersionEntry[]).map(
            (v: VersionEntry) => v.version ?? String(v)
          )
        } else if (Array.isArray((data as Record<string, unknown>)?.installedVersions)) {
          list = (data as Record<string, unknown>).installedVersions as string[]
        } else if (Array.isArray(data)) {
          list = (data as VersionEntry[]).map((v: VersionEntry) => v.version ?? String(v))
        }

        if (!cancelled) {
          setVersions(list)
          // Auto-select first if nothing selected yet
          if (list.length > 0 && !selectedVersion) {
            onVersionChange(list[0])
          }
        }
      } catch {
        // ignore network errors
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => { cancelled = true }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [gatewayUrl])

  if (loading) {
    return (
      <div className="version-selector version-selector--loading" title="Loading Python versions...">
        <Code2 size={13} />
        <span className="version-selector__label">Loading...</span>
      </div>
    )
  }

  if (versions.length === 0) {
    return (
      <div className="version-selector version-selector--empty" title="No Python versions found">
        <Code2 size={13} />
        <span className="version-selector__label">Default Python</span>
      </div>
    )
  }

  return (
    <div className="version-selector">
      <Code2 size={13} className="version-selector__icon" />
      <select
        className="version-selector__select"
        value={selectedVersion}
        onChange={e => onVersionChange(e.target.value)}
        title="Select Python version"
        aria-label="Select Python version"
      >
        {versions.map(v => (
          <option key={v} value={v}>{v}</option>
        ))}
      </select>
    </div>
  )
}

export default VersionSelector
