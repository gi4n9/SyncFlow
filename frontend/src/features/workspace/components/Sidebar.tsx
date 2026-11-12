import { useEffect, useRef } from 'react'
import { ArrowRight, CalendarDays, ChevronDown, CircleHelp, Command, FolderKanban, LayoutDashboard, ListTodo, LogOut, MoreHorizontal, Plus, Settings2, Sparkles, UserPlus } from 'lucide-react'
import type { AppBoardSummary, AppUser, AppWorkspace } from '../../../shared/types'

type SidebarProps = {
  profile: AppUser | null
  workspaces: AppWorkspace[]
  activeWorkspace: AppWorkspace | undefined
  activeWorkspaceId: number | null
  workspaceMenu: boolean
  onToggleWorkspaceMenu: () => void
  onSelectWorkspace: (id: number) => void
  onCreateWorkspace: () => void
  onJoinWorkspace: () => void
  onLeaveWorkspace: () => void
  boards: AppBoardSummary[]
  activeBoardId: number | null
  onSelectBoard: (id: number) => void
  onCreateBoard: () => void
  onNotice: (message: string) => void
  onAuth: () => void
  onSignOut: () => void
}

export function Sidebar(props: SidebarProps) {
  const { profile, workspaces, activeWorkspace, activeWorkspaceId, workspaceMenu, onToggleWorkspaceMenu, onSelectWorkspace, onCreateWorkspace, onJoinWorkspace, onLeaveWorkspace, boards, activeBoardId, onSelectBoard, onCreateBoard, onNotice, onAuth, onSignOut } = props
  const initials = profile?.fullName.split(/\s+/).map(part => part[0]).slice(0, 2).join('').toUpperCase() ?? 'AL'
  const workspaceMenuRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!workspaceMenu) return
    const dismissOutside = (event: PointerEvent) => {
      if (!workspaceMenuRef.current?.contains(event.target as Node)) onToggleWorkspaceMenu()
    }
    const dismissEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') onToggleWorkspaceMenu() }
    document.addEventListener('pointerdown', dismissOutside)
    document.addEventListener('keydown', dismissEscape)
    return () => {
      document.removeEventListener('pointerdown', dismissOutside)
      document.removeEventListener('keydown', dismissEscape)
    }
  }, [workspaceMenu, onToggleWorkspaceMenu])

  return (
    <aside className="sidebar">
      <a className="brand" href="#top">
        <span className="brand-mark"><Command size={19} strokeWidth={2.4} /></span>
        <span>syncflow<span className="brand-period">.</span></span>
      </a>

      <div className="workspace-menu-container" ref={workspaceMenuRef}>
        <button className="workspace-switch" aria-expanded={workspaceMenu} onClick={onToggleWorkspaceMenu}>
          <span className="workspace-mark">{(activeWorkspace?.name ?? 'Studio North').slice(0, 1).toUpperCase()}</span>
          <span className="workspace-copy">
            <b>{activeWorkspace?.name ?? 'Studio North'}</b>
            <small>{activeWorkspace?.role ?? 'Sample workspace'}</small>
          </span>
          <ChevronDown size={15} />
        </button>

      {workspaceMenu && (
        <div className="workspace-menu">
          <div className="workspace-menu-heading"><b>Workspaces</b></div>
          {workspaces.map((workspace) => (
            <button
              key={workspace.id}
              onClick={() => onSelectWorkspace(workspace.id)}
              className={workspace.id === activeWorkspaceId ? 'current' : ''}
            >
              <span className="workspace-mark">{workspace.name.slice(0, 1).toUpperCase()}</span>
              {workspace.name}
              <small>{workspace.role.toLowerCase()}</small>
            </button>
          ))}
          <button className="new-workspace" onClick={onCreateWorkspace}>
            <Plus size={14} /> Create workspace
          </button>
          <button className="new-workspace" onClick={onJoinWorkspace}>
            <UserPlus size={14} /> Join with invitation
          </button>
          {profile && activeWorkspace && <>
            <div className="workspace-menu-divider" />
            <button className="leave-workspace-action" onClick={onLeaveWorkspace}>
              <LogOut size={14} /> Leave “{activeWorkspace.name}”
            </button>
          </>}
        </div>
      )}
      </div>

      <div className="side-label">
        WORKSPACE
        <button aria-label="Workspace settings"><Settings2 size={14} /></button>
      </div>
      <nav className="nav-group">
        <button className="nav-item"><LayoutDashboard size={17} /> Overview</button>
        <button className="nav-item active"><FolderKanban size={17} /> Projects <span className="nav-count">{boards.length}</span></button>
        <button className="nav-item"><ListTodo size={17} /> My tasks</button>
        <button className="nav-item"><CalendarDays size={17} /> Calendar</button>
      </nav>

      <div className="side-label project-label">
        BOARDS
        <button aria-label="Create board" onClick={onCreateBoard}><Plus size={14} /></button>
      </div>
      <div className="project-list">
        {profile ? boards.map((board, index) => (
          <button key={board.id} className={board.id === activeBoardId ? 'active-board' : ''} onClick={() => onSelectBoard(board.id)}>
            <span className={`project-dot dot-${['indigo', 'orange', 'green', 'blue'][index % 4]}`} />
            {board.name}
            <span>{board.archived ? 'Archived' : ''}</span>
          </button>
        )) : (
          <>
            <button><span className="project-dot dot-indigo" /> Product design <span>8</span></button>
            <button><span className="project-dot dot-orange" /> Engineering <span>12</span></button>
            <button><span className="project-dot dot-green" /> Marketing <span>5</span></button>
            <button><span className="project-dot dot-blue" /> Operations <span>3</span></button>
          </>
        )}
        {profile && boards.length === 0 && (
          <button onClick={onCreateBoard}><Plus size={14} /> Create your first board</button>
        )}
      </div>

      <div className="sidebar-bottom">
        <div className="upgrade-card">
          <div className="upgrade-icon"><Sparkles size={17} /></div>
          <b>Make room for more</b>
          <p>You’re using 3 of 5 projects on your free plan.</p>
          <button onClick={() => onNotice('You’re all set — unlimited projects are coming soon.')}>
            Explore plans <ArrowRight size={13} />
          </button>
        </div>
        <button className="nav-item"><CircleHelp size={17} /> Help &amp; support</button>
        <div className="profile">
          <div className="avatar peach">{initials}</div>
          <div className="profile-copy">
            <b>{profile?.fullName ?? 'Alex Lee'}</b>
            <small>{profile?.email ?? 'Sample account'}</small>
          </div>
          <button aria-label={profile ? 'Sign out' : 'Profile menu'} onClick={profile ? onSignOut : onAuth}>
            {profile ? <LogOut size={16} /> : <MoreHorizontal size={18} />}
          </button>
        </div>
      </div>
    </aside>
  )
}
