import { useEffect, useMemo, useState } from 'react'
import CandleTable from '../components/CandleTable.jsx'
import PriceTicker from '../components/PriceTicker.jsx'
import SignalCard from '../components/SignalCard.jsx'
import SignalList from '../components/SignalList.jsx'
import { createMarketSocket, fetchSignals, fetchSnapshot } from '../services/websocket.js'

const emptySnapshot = {
  symbol: 'BANKNIFTY',
  livePrice: 0,
  lastUpdated: null,
  latestSignal: null,
  candles: [],
}

export default function DashboardPage() {
  const [snapshot, setSnapshot] = useState(emptySnapshot)
  const [signals, setSignals] = useState([])
  const [connectionStatus, setConnectionStatus] = useState('CONNECTING')

  useEffect(() => {
    let mounted = true

    Promise.all([fetchSnapshot(), fetchSignals()])
      .then(([initialSnapshot, initialSignals]) => {
        if (!mounted) return
        setSnapshot(initialSnapshot)
        setSignals(initialSignals)
      })
      .catch(() => {
        if (!mounted) return
        setConnectionStatus('ERROR')
      })

    const cleanup = createMarketSocket({
      onSnapshot: (nextSnapshot) => {
        setSnapshot(nextSnapshot)
        if (nextSnapshot.latestSignal) {
          setSignals((current) => {
            const withoutDuplicate = current.filter((item) => item.id !== nextSnapshot.latestSignal.id)
            return [nextSnapshot.latestSignal, ...withoutDuplicate].slice(0, 20)
          })
        }
      },
      onStatusChange: setConnectionStatus,
    })

    return () => {
      mounted = false
      cleanup()
    }
  }, [])

  const latestSignal = useMemo(() => snapshot.latestSignal ?? signals[0] ?? null, [snapshot, signals])

  return (
    <main className="app-shell">
      <PriceTicker
        symbol={snapshot.symbol}
        livePrice={snapshot.livePrice}
        lastUpdated={snapshot.lastUpdated}
        connectionStatus={connectionStatus}
      />

      <section className="content-grid">
        <SignalCard signal={latestSignal} active={latestSignal?.status === 'OPEN'} />
        <SignalList signals={signals} />
      </section>

      <CandleTable candles={snapshot.candles ?? []} />
    </main>
  )
}
