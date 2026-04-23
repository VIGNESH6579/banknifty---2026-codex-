const formatPrice = (value) =>
  new Intl.NumberFormat('en-IN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
  }).format(value ?? 0)

const formatTime = (value) => {
  if (!value) return '--'
  return new Date(value).toLocaleTimeString('en-IN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

export default function PriceTicker({ symbol, livePrice, lastUpdated, connectionStatus }) {
  return (
    <section className="hero-band">
      <div>
        <p className="eyebrow">Real-time index stream</p>
        <h1>{symbol}</h1>
      </div>

      <div className="price-cluster">
        <div className="price-value">{formatPrice(livePrice)}</div>
        <div className="status-row">
          <span className={`status-pill status-${connectionStatus?.toLowerCase()}`}>{connectionStatus}</span>
          <span className="timestamp">Updated {formatTime(lastUpdated)}</span>
        </div>
      </div>
    </section>
  )
}
