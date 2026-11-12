import { getWebSocketAccessToken } from './client'
import type { BoardRealtimeEvent, NotificationItem } from './client'

export function connectNotificationSocket(onNotification: (item: NotificationItem) => void, boardId: number | null = null,
                                          onBoardEvent?: (event: BoardRealtimeEvent) => void,
                                          onConnected?: () => void) {
  let socket: WebSocket | null = null
  let stopped = false
  let retryTimer: number | undefined
  let retryDelay = 1000

  const connect = async () => {
    try {
      const token = await getWebSocketAccessToken()
      if (stopped || !token) return
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      socket = new WebSocket(`${protocol}//${window.location.host}/ws`)
      let buffer = ''
      socket.onopen = () => {
        socket?.send(`CONNECT\naccept-version:1.2\nheart-beat:10000,10000\nAuthorization:Bearer ${token}\n\n\0`)
      }
      socket.onmessage = event => {
        buffer += String(event.data)
        const frames = buffer.split('\0')
        buffer = frames.pop() ?? ''
        for (const frame of frames) {
          if (frame.startsWith('CONNECTED')) {
            retryDelay = 1000
            socket?.send('SUBSCRIBE\nid:notifications\ndestination:/user/queue/notifications\nack:auto\n\n\0')
            if (boardId !== null) socket?.send(`SUBSCRIBE\nid:board-${boardId}\ndestination:/topic/boards/${boardId}\nack:auto\n\n\0`)
            onConnected?.()
          } else if (frame.startsWith('MESSAGE')) {
            const body = frame.slice(frame.indexOf('\n\n') + 2).trim()
            const destination = frame.split('\n').find(line => line.startsWith('destination:'))?.slice('destination:'.length)
            try {
              const payload = JSON.parse(body)
              if (destination?.startsWith('/topic/boards/')) onBoardEvent?.(payload as BoardRealtimeEvent)
              else if (destination === '/user/queue/notifications') onNotification(payload as NotificationItem)
            } catch { /* Ignore malformed broker messages. */ }
          } else if (frame.startsWith('ERROR')) {
            socket?.close()
          }
        }
      }
      socket.onclose = () => {
        if (stopped) return
        retryTimer = window.setTimeout(connect, retryDelay)
        retryDelay = Math.min(retryDelay * 2, 30_000)
      }
      socket.onerror = () => socket?.close()
    } catch {
      if (!stopped) {
        retryTimer = window.setTimeout(connect, retryDelay)
        retryDelay = Math.min(retryDelay * 2, 30_000)
      }
    }
  }
  void connect()
  return () => {
    stopped = true
    if (retryTimer !== undefined) window.clearTimeout(retryTimer)
    socket?.close(1000, 'Session ended')
  }
}
