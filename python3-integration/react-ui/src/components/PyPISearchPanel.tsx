import { useState, useEffect, useRef, useCallback } from 'react'
import { Search, Loader, ExternalLink, Download, AlertCircle } from 'lucide-react'
import { apiGet } from '../utils/api'
import './PyPISearchPanel.css'

interface PyPIResult {
  name: string
  version: string
  summary: string
}

interface SearchResponse {
  success: boolean
  query: string
  count: number
  results: PyPIResult[]
}

interface Props {
  onInstall: (packageName: string, version?: string) => void
}

function PyPISearchPanel({ onInstall }: Props) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<PyPIResult[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [searched, setSearched] = useState(false)
  const debounceRef = useRef<number | null>(null)

  const doSearch = useCallback(async (q: string) => {
    if (!q.trim()) {
      setResults([])
      setSearched(false)
      return
    }
    setLoading(true)
    setError(null)
    setSearched(true)
    try {
      const data = await apiGet<SearchResponse>(
        `/api/v1/packages/search-pypi?q=${encodeURIComponent(q.trim())}`
      )
      setResults(data.results || [])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Search failed')
      setResults([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)
    if (!query.trim()) {
      setResults([])
      setSearched(false)
      return
    }
    debounceRef.current = window.setTimeout(() => doSearch(query), 500)
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current) }
  }, [query, doSearch])

  return (
    <div className="pypi-search">
      <div className="pypi-search__input-wrap">
        <Search size={14} className="pypi-search__icon" />
        <input
          className="pypi-search__input"
          type="text"
          placeholder="Search PyPI packages..."
          value={query}
          onChange={e => setQuery(e.target.value)}
          spellCheck={false}
          autoFocus
        />
        {loading && <Loader size={14} className="pypi-search__spinner" />}
      </div>

      {error && (
        <div className="pypi-search__error">
          <AlertCircle size={14} />
          {error}
        </div>
      )}

      <div className="pypi-search__results">
        {!loading && searched && results.length === 0 && !error && (
          <div className="pypi-search__empty">
            No packages found for &quot;{query}&quot;
          </div>
        )}
        {results.map(pkg => (
          <div key={pkg.name} className="pypi-search__result">
            <div className="pypi-search__result-info">
              <div className="pypi-search__result-header">
                <span className="pypi-search__result-name">{pkg.name}</span>
                <span className="pypi-search__result-version">{pkg.version}</span>
              </div>
              {pkg.summary && (
                <p className="pypi-search__result-summary">{pkg.summary}</p>
              )}
            </div>
            <div className="pypi-search__result-actions">
              <a
                className="pypi-search__link"
                href={`https://pypi.org/project/${encodeURIComponent(pkg.name)}/`}
                target="_blank"
                rel="noopener noreferrer"
                title="View on PyPI"
              >
                <ExternalLink size={13} />
              </a>
              <button
                className="pypi-search__install-btn"
                onClick={() => onInstall(pkg.name)}
                title={`Install ${pkg.name}`}
              >
                <Download size={13} />
                Install
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

export default PyPISearchPanel
