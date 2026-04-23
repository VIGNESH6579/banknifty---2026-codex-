const formatNumber = (value) =>
  new Intl.NumberFormat('en-IN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
  }).format(value ?? 0)

const formatMinute = (value) => {
  if (!value) return '--'
  return new Date(value).toLocaleTimeString('en-IN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function CandleTable({ candles }) {
  return (
    <section className="panel">
      <div className="panel-header">
        <h2>Last 50 One-Minute Candles</h2>
      </div>

      <div className="candle-table">
        <div className="candle-head">
          <span>Time</span>
          <span>Open</span>
          <span>High</span>
          <span>Low</span>
          <span>Close</span>
          <span>Volume</span>
        </div>

        {candles.length === 0 ? (
          <p className="muted-copy">Candles will populate from the live SmartAPI feed.</p>
        ) : (
          [...candles].reverse().map((candle) => (
            <div key={candle.startTime} className="candle-row">
              <span>{formatMinute(candle.startTime)}</span>
              <span>{formatNumber(candle.open)}</span>
              <span>{formatNumber(candle.high)}</span>
              <span>{formatNumber(candle.low)}</span>
              <span>{formatNumber(candle.close)}</span>
              <span>{formatNumber(candle.volume)}</span>
            </div>
          ))
        )}
      </div>
    </section>
  )
}
