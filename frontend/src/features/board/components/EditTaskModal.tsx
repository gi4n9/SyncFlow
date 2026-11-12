import { LockKeyhole, Save } from 'lucide-react'
import type { FormEventHandler } from 'react'
import type { Priority, Task } from '../../../shared/types'
import type { WorkspaceMember } from '../../../api/client'
import { ModalShell } from '../../../shared/components/ModalShell'

type Props = {
  task: Task | null
  title: string
  description: string
  priority: Priority
  dueDate: string
  labels: string
  members: WorkspaceMember[]
  assigneeIds: number[]
  error: string
  onTitle: (value: string) => void
  onDescription: (value: string) => void
  onPriority: (value: Priority) => void
  onDueDate: (value: string) => void
  onLabels: (value: string) => void
  onAssignees: (value: number[]) => void
  onClose: () => void
  onSubmit: FormEventHandler<HTMLFormElement>
}

export function EditTaskModal(props: Props) {
  const { task, title, description, priority, dueDate, labels, members, assigneeIds, error, onTitle, onDescription, onPriority, onDueDate, onLabels, onAssignees, onClose, onSubmit } = props
  if (!task) return null

  return (
    <ModalShell kicker={task.project} title="Edit task" onClose={onClose} onSubmit={onSubmit}>
      <label className="field-label" htmlFor="edit-task-title">Task name</label>
      <input id="edit-task-title" className="text-field" value={title} onChange={event => onTitle(event.target.value)} required maxLength={255} />

      <label className="field-label" htmlFor="edit-task-description">Description</label>
      <textarea id="edit-task-description" className="text-field task-description-field" value={description} onChange={event => onDescription(event.target.value)} maxLength={10000} rows={4} />

      <label className="field-label" htmlFor="edit-task-labels">Labels</label>
      <input id="edit-task-labels" className="text-field" value={labels} onChange={event => onLabels(event.target.value)} placeholder="Design, Sprint 08" maxLength={300} />
      <small className="form-hint">Separate labels with commas. Labels are shared across this board.</small>

      <div className="modal-row">
        <div>
          <label className="field-label" htmlFor="edit-task-priority">Priority</label>
          <select id="edit-task-priority" className="text-field" value={priority} onChange={event => onPriority(event.target.value as Priority)}>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </div>
        <div>
          <label className="field-label" htmlFor="edit-task-due">Due date</label>
          <input id="edit-task-due" className="text-field" type="date" value={dueDate} onChange={event => onDueDate(event.target.value)} />
        </div>
      </div>

      <span className="field-label">Assignees</span>
      {members.length ? (
        <div className="assignee-options">
          {members.map(member => (
            <label className="assignee-option" key={member.userId}>
              <input
                type="checkbox"
                checked={assigneeIds.includes(member.userId)}
                onChange={event => onAssignees(event.target.checked
                  ? [...assigneeIds, member.userId]
                  : assigneeIds.filter(id => id !== member.userId))}
              />
              <span><b>{member.fullName}</b><small>{member.email} · {member.role.toLowerCase()}</small></span>
            </label>
          ))}
        </div>
      ) : <p className="assignee-empty">No workspace members are available to assign.</p>}

      {error && <div className="auth-error"><LockKeyhole size={15} />{error}</div>}
      <div className="modal-actions">
        <button type="button" className="outline-button" onClick={onClose}>Cancel</button>
        <button className="primary-button" type="submit"><Save size={16} /> Save changes</button>
      </div>
    </ModalShell>
  )
}
