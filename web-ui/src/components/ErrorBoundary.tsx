import { Component, ReactNode } from 'react'

interface Props { children: ReactNode }
interface State { hasError: boolean; error: Error | null }

class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{
          padding: '24px',
          color: 'var(--accent-error, #f38ba8)',
          backgroundColor: 'var(--bg-secondary, #181825)',
          borderRadius: '8px',
          margin: '16px',
          fontFamily: 'var(--font-mono, monospace)',
          fontSize: '13px',
        }}>
          <h3 style={{ marginBottom: '8px', fontSize: '16px' }}>Something went wrong</h3>
          <p style={{ color: 'var(--text-secondary, #bac2de)', marginBottom: '12px' }}>
            An error occurred in this view. Try refreshing the page.
          </p>
          <pre style={{
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            padding: '12px',
            backgroundColor: 'var(--bg-tertiary, #1e1e2e)',
            borderRadius: '4px',
          }}>
            {this.state.error?.message}
          </pre>
          <button
            onClick={() => this.setState({ hasError: false, error: null })}
            style={{
              marginTop: '12px',
              padding: '8px 16px',
              backgroundColor: 'var(--accent-primary, #89b4fa)',
              color: '#fff',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '13px',
            }}
          >
            Try Again
          </button>
        </div>
      )
    }
    return this.props.children
  }
}

export default ErrorBoundary
