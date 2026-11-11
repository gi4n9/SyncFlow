import { Bell, Search } from 'lucide-react'
import type { AppUser, AppWorkspace } from '../types'

export function TopBar({ workspace, boardName, profile, onNotice, onAccount }: { workspace?: AppWorkspace; boardName: string; profile: AppUser | null; onNotice: (message: string) => void; onAccount: () => void }) {
  const initials = profile?.fullName.split(/\s+/).map(part => part[0]).slice(0, 2).join('').toUpperCase() ?? 'AL'
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
        <button className="icon-button notification-button" aria-label="Notifications" onClick={() => onNotice('You’re all caught up!')}>
          <Bell size={18} />
          <i />
        </button>
        <div className="top-divider" />
        <button className="top-avatar avatar peach" aria-label={profile ? 'Sign out' : 'Sign in'} onClick={onAccount}>
          {initials}
        </button>
      </div>
    </header>
  )
}
