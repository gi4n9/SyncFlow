import { useEffect, useRef, useState } from 'react'
import { ArrowRight, CalendarDays, Check, MoreHorizontal, Pencil, X } from 'lucide-react'
import { people, type Task } from '../../../shared/types'

export function TaskCard({ task, canEdit, onEdit, onMove, onOpen }: { task: Task; canEdit: boolean; onEdit: () => void; onMove: () => void; onOpen: () => void }) {
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!menuOpen) return
    const dismissOutside = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setMenuOpen(false)
    }
    const dismissEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') setMenuOpen(false) }
    document.addEventListener('pointerdown', dismissOutside)
    document.addEventListener('keydown', dismissEscape)
    return () => {
      document.removeEventListener('pointerdown', dismissOutside)
      document.removeEventListener('keydown', dismissEscape)
    }
  }, [menuOpen])
  const projectTone = task.project === 'Engineering' ? 'dot-orange' : task.project === 'Planning' ? 'dot-green' : 'dot-indigo'
  return (
    <article className="task-card">
      <div className="task-top">
        <span className="task-project">
          <span className={`project-dot ${projectTone}`} />
          {task.project}
        </span>
        <div className="task-menu-container" ref={menuRef}>
          <button className="task-menu" aria-label={`Task actions for ${task.title}`} aria-haspopup="menu" aria-expanded={menuOpen} disabled={!canEdit} onClick={() => setMenuOpen(open => !open)}>
            <MoreHorizontal size={16} />
          </button>
          {menuOpen && <div className="task-actions-menu" role="menu">
            <div className="task-actions-heading"><span>Task actions</span><button type="button" aria-label="Close task actions" onClick={() => setMenuOpen(false)}><X size={14} /></button></div>
            <button type="button" role="menuitem" onClick={() => { setMenuOpen(false); onEdit() }}><Pencil size={14} /> Edit task</button>
            <button type="button" role="menuitem" onClick={() => { setMenuOpen(false); onMove() }}><ArrowRight size={14} /> Move to next list</button>
          </div>}
        </div>
      </div>
      <h3><button className="task-title-button" onClick={onOpen}>{task.title}</button></h3>
      <div className="task-labels">
        {task.labels.map((label) => {
          const tone = label === 'Sprint 08' ? 'tag-purple' : label === 'UI design' ? 'tag-blue' : label === 'Backend' ? 'tag-green' : ''
          return <span className={`tag ${tone}`} key={label}>{label}</span>
        })}
      </div>
      <div className="task-meta">
        <span className={`priority priority-${task.priority.toLowerCase()}`}>
          <i />
          {task.priority}
        </span>
        <span className="due-date"><CalendarDays size={13} />{task.due}</span>
        <div className="task-spacer" />
        {task.checklist && <span className="checklist"><Check size={12} />{task.checklist}</span>}
        {task.comments && <span className="comment-count"><span>▤</span>{task.comments}</span>}
        <div className="task-avatars">
          {task.assignees.slice(0, 2).map((id) => (
            <span className={`avatar avatar-small ${people[id]?.color ?? 'sky'}`} title={task.assigneeNames?.[id] ?? people[id]?.name ?? id} key={id}>{id}</span>
          ))}
        </div>
      </div>
    </article>
  )
}
