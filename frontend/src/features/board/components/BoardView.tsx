import { Activity, ArrowDownUp, LayoutDashboard, ListTodo, MoreHorizontal, Plus, Search, SlidersHorizontal, Users } from 'lucide-react'
import type { AppBoardSummary, AppUser, Column } from '../../../shared/types'
import { TaskCard } from './TaskCard'

type Props = {
  columns: Column[]; query: string; filter: string; profile: AppUser | null; boards: AppBoardSummary[]
  boardLoading: boolean; boardError: string; hasBoard: boolean; canEdit: boolean
  onQueryChange: (value: string) => void; onFilterChange: (value: string) => void
  onNotice: (message: string) => void; onRetry: () => void; onCreateBoard: () => void
  onAddTask: (listId?: number) => void; onMoveTask: (taskId: number, fromId: string) => void
}

export function BoardView(props: Props) {
  const { columns, query, filter, profile, boards, boardLoading, boardError, hasBoard, canEdit, onQueryChange, onFilterChange, onNotice, onRetry, onCreateBoard, onAddTask, onMoveTask } = props
  return (
    <section className="board-section">
      <div className="board-toolbar">
        <div className="view-tabs">
          <button className="view-tab selected"><LayoutDashboard size={15} /> Board</button>
          <button className="view-tab" onClick={() => onNotice('List view is coming soon.')}><ListTodo size={15} /> List</button>
          <button className="view-tab" onClick={() => onNotice('Timeline view is coming soon.')}><Activity size={15} /> Timeline</button>
        </div>
        <div className="board-tools">
          <label className="board-search">
            <Search size={15} />
            <input id="search" placeholder="Search tasks..." value={query} onChange={event => onQueryChange(event.target.value)} />
            <kbd>⌘ F</kbd>
          </label>
          <select className="filter-select" aria-label="Filter tasks" value={filter} onChange={event => onFilterChange(event.target.value)}>
            <option>All tasks</option>
            <option>High</option>
            <option>Medium</option>
            <option>Low</option>
          </select>
          <button className="tool-button" onClick={() => onNotice('Showing members in this workspace.')}><Users size={16} /></button>
          <button className="tool-button" onClick={() => onNotice('Tasks are grouped by list.')}><SlidersHorizontal size={16} /></button>
          <button className="tool-button sort-button" onClick={() => onNotice('Tasks are ordered by list position.')}>
            <ArrowDownUp size={15} />
            <span>Sort</span>
          </button>
        </div>
      </div>

      {profile && boardError && (
        <div className="board-empty">
          <p>{boardError}</p>
          <button className="outline-button" onClick={onRetry}>Retry loading</button>
        </div>
      )}
      {profile && boardLoading && !hasBoard && <div className="board-empty">Loading boards and tasks…</div>}
      {profile && !boardLoading && !boardError && !hasBoard && (
        <div className="board-empty">
          <h2>{boards.length ? 'Select a board' : 'No boards in this workspace yet'}</h2>
          <p>Create a board to start saving tasks to your workspace.</p>
          <button className="primary-button" onClick={onCreateBoard}><Plus size={16} /> Create board</button>
        </div>
      )}
      {(!profile || hasBoard) && (
        <div className="kanban">
          {columns.map(column => (
            <div className="kanban-column" key={column.id}>
              <div className="column-head">
                <div className={`column-status ${column.tone}`} />
                <h2>{column.title}</h2>
                <span className="task-count">{column.tasks.length}</span>
                <button className="column-menu" aria-label={`Add task to ${column.title}`} disabled={!canEdit} onClick={() => onAddTask(column.listId)}>
                  <Plus size={16} />
                </button>
                <button className="column-menu" aria-label="Column options"><MoreHorizontal size={17} /></button>
              </div>
              <div className="task-list">
                {column.tasks.map(task => (
                  <TaskCard key={task.id} task={task} canEdit={canEdit} onMove={() => onMoveTask(task.id, column.id)} />
                ))}
              </div>
              <button className="add-card" disabled={!canEdit} onClick={() => onAddTask(column.listId)}>
                <Plus size={15} /> Add a task
              </button>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
