import { ChevronDown, Plus } from 'lucide-react'
import type { FormEventHandler } from 'react'
import type { AppBoard, Priority } from '../../../shared/types'
import { ModalShell } from '../../../shared/components/ModalShell'

type Props = { open: boolean; board: AppBoard | null; title: string; listId: number | null; priority: Priority; dueDate: string; onTitle: (value: string) => void; onList: (id: number) => void; onPriority: (value: Priority) => void; onDueDate: (value: string) => void; onClose: () => void; onSubmit: FormEventHandler<HTMLFormElement> }

export function TaskModal(props: Props) {
  if (!props.open) return null
  const { board, title, listId, priority, dueDate, onTitle, onList, onPriority, onDueDate, onClose, onSubmit } = props
  return (
    <ModalShell kicker={board?.name ?? 'PREVIEW BOARD'} title="Create a task" onClose={onClose} onSubmit={onSubmit}>
      <label className="field-label" htmlFor="task-title">Task name</label>
      <input
        autoFocus
        id="task-title"
        className="text-field"
        placeholder="What needs to get done?"
        value={title}
        onChange={event => onTitle(event.target.value)}
        required
        maxLength={255}
      />
      <div className="modal-row">
        <div>
          <label className="field-label" htmlFor="task-list">List</label>
          {board ? (
            <select id="task-list" className="text-field" value={listId ?? ''} onChange={event => onList(Number(event.target.value))}>
              {board.lists.map(list => <option key={list.id} value={list.id}>{list.name}</option>)}
            </select>
          ) : (
            <div className="modal-choice"><span className="column-status slate" /> To do <ChevronDown size={14} /></div>
          )}
        </div>
        <div>
          <label className="field-label" htmlFor="task-priority">Priority</label>
          <select id="task-priority" className="text-field" value={priority} onChange={event => onPriority(event.target.value as Priority)}>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </div>
      </div>
      <label className="field-label" htmlFor="task-due">Due date</label>
      <input id="task-due" className="text-field" type="date" value={dueDate} onChange={event => onDueDate(event.target.value)} />
      <div className="modal-actions">
        <button type="button" className="outline-button" onClick={onClose}>Cancel</button>
        <button className="primary-button" type="submit"><Plus size={16} /> Create task</button>
      </div>
    </ModalShell>
  )
}
