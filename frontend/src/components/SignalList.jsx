const formatNumber = (value) =>
  new Intl.NumberFormat('en-IN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
  }).format(value ?? 0)

const formatTime = (value) => {
  if (!value) return '--'
  return new Date(value).toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function SignalList({ signals }) {
  return (
    <section className="panel">
      <div className="panel-header">
        <h2>Recent Signals</h2>
      </div>

      <div className="signal-list">
        {signals.length === 0 ? (
          <p className="muted-copy">No stored signals yet.</p>
        ) : (
          signals.map((signal) => (
            <div key={signal.id ?? signal.timestamp} className="signal-row">
              <div className={`signal-type ${signal.type?.toLowerCase()}`}>{signal.type}</div>
              <div>{formatNumber(signal.entry)}</div>
              <div>{formatNumber(signal.stopLoss)}</div>
              <div>{formatNumber(signal.target)}</div>
              <div>{formatTime(signal.timestamp)}</div>
            </div>
          ))
        )}
      </div>
    </section>
  )
}
