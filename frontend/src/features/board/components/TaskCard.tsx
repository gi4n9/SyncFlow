import { CalendarDays, Check, MoreHorizontal } from 'lucide-react'
import { people, type Task } from '../../../shared/types'

export function TaskCard({ task, canEdit, onMove }: { task: Task; canEdit: boolean; onMove: () => void }) {
  const projectTone = task.project === 'Engineering' ? 'dot-orange' : task.project === 'Planning' ? 'dot-green' : 'dot-indigo'
  return (
    <article className="task-card">
      <div className="task-top">
        <span className="task-project">
          <span className={`project-dot ${projectTone}`} />
          {task.project}
        </span>
        <button className="task-menu" aria-label="Move task" disabled={!canEdit} onClick={onMove}>
          <MoreHorizontal size={16} />
        </button>
      </div>
      <h3>{task.title}</h3>
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
            <span className={`avatar avatar-small ${people[id]?.color ?? 'sky'}`} title={people[id]?.name ?? id} key={id}>{id}</span>
          ))}
        </div>
      </div>
    </article>
  )
}
