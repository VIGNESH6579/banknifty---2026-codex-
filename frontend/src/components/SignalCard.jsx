const formatNumber = (value) =>
  new Intl.NumberFormat('en-IN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
  }).format(value ?? 0)

const formatExpiry = (value) => {
  if (!value) return '--'
  return new Date(value).toLocaleString('en-IN', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function SignalCard({ signal, active = false }) {
  if (!signal) {
    return (
      <article className="signal-card empty-card">
        <div className="card-title-row">
          <h2>Latest Signal</h2>
        </div>
        <p className="muted-copy">Waiting for a qualified BANKNIFTY setup.</p>
      </article>
    )
  }

  return (
    <article className={`signal-card ${signal.type?.toLowerCase()} ${active ? 'active' : ''}`}>
      <div className="card-title-row">
        <h2>{signal.type} Signal</h2>
        <span className="confidence-badge">{Math.round((signal.confidence ?? 0) * 100)}%</span>
      </div>

      <div className="signal-grid">
        <div>
          <span className="field-label">Entry</span>
          <strong>{formatNumber(signal.entry)}</strong>
        </div>
        <div>
          <span className="field-label">Stop Loss</span>
          <strong>{formatNumber(signal.stopLoss)}</strong>
        </div>
        <div>
          <span className="field-label">Target</span>
          <strong>{formatNumber(signal.target)}</strong>
        </div>
        <div>
          <span className="field-label">Status</span>
          <strong>{signal.status}</strong>
        </div>
      </div>

      <div className="expiry-banner">
        <span className="field-label">Weekly Expiry</span>
        <strong>{formatExpiry(signal.expiry)}</strong>
      </div>
    </article>
  )
}
