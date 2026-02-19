import { useCallback, useState } from 'react'

export function useGatewayFetch(gatewayUrl: string) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchData = useCallback(async <T = unknown>(
    path: string,
    options?: RequestInit
  ): Promise<T | null> => {
    setLoading(true)
    setError(null)
    try {
      const url = path.startsWith('http') ? path : `${gatewayUrl}/${path}`
      const res = await fetch(url, {
        ...options,
        headers: {
          'Content-Type': 'application/json',
          ...options?.headers,
        },
      })
      if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${res.statusText}`)
      }
      const raw = await res.json()
      return (raw.data !== undefined ? raw.data : raw) as T
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Unknown error'
      setError(message)
      return null
    } finally {
      setLoading(false)
    }
  }, [gatewayUrl])

  return { fetchData, loading, error }
}
