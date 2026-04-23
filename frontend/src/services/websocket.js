import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

const resolveBaseUrl = () => {
  const configured = import.meta.env.VITE_API_BASE_URL
  if (configured) {
    return configured.replace(/\/$/, '')
  }
  return window.location.origin
}

export const createMarketSocket = ({ onSnapshot, onStatusChange }) => {
  const baseUrl = resolveBaseUrl()

  const client = new Client({
    webSocketFactory: () => new SockJS(`${baseUrl}/ws/market`),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      onStatusChange?.('LIVE')
      client.subscribe('/topic/market', (message) => {
        onSnapshot?.(JSON.parse(message.body))
      })
    },
    onDisconnect: () => onStatusChange?.('DISCONNECTED'),
    onStompError: () => onStatusChange?.('ERROR'),
    onWebSocketClose: () => onStatusChange?.('RECONNECTING'),
  })

  client.activate()
  return () => client.deactivate()
}

export const fetchSnapshot = async () => {
  const response = await fetch(`${resolveBaseUrl()}/api/market/snapshot`)
  if (!response.ok) {
    throw new Error('Failed to load market snapshot')
  }
  return response.json()
}

export const fetchSignals = async () => {
  const response = await fetch(`${resolveBaseUrl()}/api/signals`)
  if (!response.ok) {
    throw new Error('Failed to load signals')
  }
  return response.json()
}
