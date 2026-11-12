import { useEffect, useRef } from 'react'
import { Bell, Moon, Search, Sun, X } from 'lucide-react'
import type { AppWorkspace } from '../types'
import type { NotificationItem } from '../../api/client'

export function TopBar({ workspace, boardName, onNotice, theme = 'light', onToggleTheme, notifications = [], unreadCount = 0, notificationsOpen = false, notificationsHasNext = false, onToggleNotifications, onSelectNotification, onReadAll, onLoadMoreNotifications }: { workspace?: AppWorkspace; boardName: string; onNotice: (message: string) => void; theme?: 'light' | 'dark'; onToggleTheme?: () => void; notifications?: NotificationItem[]; unreadCount?: number; notificationsOpen?: boolean; notificationsHasNext?: boolean; onToggleNotifications?: () => void; onSelectNotification?: (item: NotificationItem) => void; onReadAll?: () => void; onLoadMoreNotifications?: () => void }) {
  const notificationRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!notificationsOpen) return
    const dismissOutside = (event: PointerEvent) => {
      if (!notificationRef.current?.contains(event.target as Node)) onToggleNotifications?.()
    }
    const dismissEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') onToggleNotifications?.() }
    document.addEventListener('pointerdown', dismissOutside)
    document.addEventListener('keydown', dismissEscape)
    return () => {
      document.removeEventListener('pointerdown', dismissOutside)
      document.removeEventListener('keydown', dismissEscape)
    }
  }, [notificationsOpen, onToggleNotifications])
  return (
    <header className="topbar">
      <div className="breadcrumbs">
        <span>{workspace?.name ?? 'Projects'}</span>
        <span className="crumb-slash">/</span>
        <b>{boardName}</b>
      </div>
      <div className="top-actions">
        <button className="icon-button search-shortcut" onClick={() => document.getElementById('search')?.focus()}>
          <Search size={17} />
          <span>Search</span>
          <kbd>⌘ K</kbd>
        </button>
        <button className="icon-button theme-toggle" aria-label={`Switch to ${theme === 'light' ? 'dark' : 'light'} theme`} title={`Switch to ${theme === 'light' ? 'dark' : 'light'} theme`} onClick={onToggleTheme}>
          {theme === 'light' ? <Moon size={17} strokeWidth={1.8} /> : <Sun size={17} strokeWidth={1.8} />}
        </button>
        <div className="notification-control" ref={notificationRef}>
          <button className={`icon-button notification-button ${unreadCount > 0 ? 'has-unread' : ''}`} aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ''}`} aria-expanded={notificationsOpen} onClick={onToggleNotifications ?? (() => onNotice('Sign in to view notifications.'))}>
            <Bell size={18} strokeWidth={1.8} />
            {unreadCount > 0 && <i />}
            {unreadCount > 0 && <span className="notification-badge">{unreadCount > 99 ? '99+' : unreadCount}</span>}
          </button>
        {notificationsOpen && <section className="notification-popover">
          <header><b>Notifications</b><div><button disabled={!unreadCount} onClick={onReadAll}>Mark all read</button><button className="notification-close" aria-label="Close notifications" onClick={onToggleNotifications}><X size={14} /></button></div></header>
          <div className="notification-list">
            {notifications.length === 0 ? <p className="notification-empty">You’re all caught up.</p> : notifications.map(item => <button key={item.id} className={`notification-item ${item.read ? '' : 'unread'}`} onClick={() => onSelectNotification?.(item)}>
              <span>{item.message}</span><small>{new Date(item.createdAt).toLocaleString()}</small>
            </button>)}
            {notificationsHasNext && <button className="notification-load-more" onClick={onLoadMoreNotifications}>Load older notifications</button>}
          </div>
        </section>}
        </div>
      </div>
    </header>
  )
}
