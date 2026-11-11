import { Activity, CalendarDays, Check, FolderKanban } from 'lucide-react'
import type { Task } from '../../../shared/types'

export function ProjectSummary({ total, inProgress, completed, upcoming, isPreview }: { total: number; inProgress: number; completed: number; upcoming?: Task; isPreview: boolean }) {
  return (
    <div className="summary-grid">
      <div className="summary-card">
        <span className="summary-icon violet-icon"><FolderKanban size={17} /></span>
        <span className="summary-label">Total tasks</span>
        <b>{total}<small> tasks</small></b>
        <div className="summary-foot">{isPreview ? 'Preview data' : 'From this board'}</div>
      </div>
      <div className="summary-card">
        <span className="summary-icon blue-icon"><Activity size={17} /></span>
        <span className="summary-label">In progress</span>
        <b>{inProgress}<small> tasks</small></b>
        <div className="mini-progress"><i /></div>
        <div className="summary-foot">Across active lists</div>
      </div>
      <div className="summary-card">
        <span className="summary-icon green-icon"><Check size={17} /></span>
        <span className="summary-label">Completed</span>
        <b>{completed}<small> tasks</small></b>
        <div className="summary-foot">In done lists</div>
      </div>
      <div className="summary-card due-card">
        <span className="summary-icon peach-icon"><CalendarDays size={17} /></span>
        <span className="summary-label">Upcoming deadline</span>
        <b>{upcoming?.due ?? '—'}</b>
        <div className="summary-foot">{upcoming?.title ?? 'No dated tasks'}</div>
      </div>
    </div>
  )
}
