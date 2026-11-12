import { Activity, ArrowDownUp, LayoutDashboard, ListTodo, MoreHorizontal, Plus, Search, SlidersHorizontal, Users } from 'lucide-react'
import type { AppBoardSummary, AppUser, Column } from '../../../shared/types'
import type { CardLabel, SearchCard, WorkspaceMember } from '../../../api/client'
import { TaskCard } from './TaskCard'

type Props = {
  columns: Column[]; query: string; filter: string; profile: AppUser | null; boards: AppBoardSummary[]
  boardLoading: boolean; boardError: string; hasBoard: boolean; canEdit: boolean
  members: WorkspaceMember[]; labels: CardLabel[]
  searchScope: 'board' | 'workspace'; assigneeFilter: string; labelFilter: string; dueFromFilter: string; dueToFilter: string
  searchActive: boolean; searchResults: SearchCard[] | null; searchResultsHasNext: boolean; searchPage: number; searchLoading: boolean; searchError: string
  onQueryChange: (value: string) => void; onFilterChange: (value: string) => void
  onNotice: (message: string) => void; onRetry: () => void; onCreateBoard: () => void
  onAddTask: (listId?: number) => void; onEditTask: (taskId: number) => void; onMoveTask: (taskId: number, fromId: string) => void; onOpenTask: (taskId: number) => void
  onSearchScopeChange: (value: 'board' | 'workspace') => void; onAssigneeFilterChange: (value: string) => void; onLabelFilterChange: (value: string) => void
  onDueFromFilterChange: (value: string) => void; onDueToFilterChange: (value: string) => void; onSearchPageChange: (page: number) => void; onOpenSearchResult: (card: SearchCard) => void
}

export function BoardView(props: Props) {
  const { columns, query, filter, profile, boards, boardLoading, boardError, hasBoard, canEdit, members, labels, searchScope, assigneeFilter, labelFilter, dueFromFilter, dueToFilter, searchActive, searchResults, searchResultsHasNext, searchPage, searchLoading, searchError, onQueryChange, onFilterChange, onNotice, onRetry, onCreateBoard, onAddTask, onEditTask, onMoveTask, onOpenTask, onSearchScopeChange, onAssigneeFilterChange, onLabelFilterChange, onDueFromFilterChange, onDueToFilterChange, onSearchPageChange, onOpenSearchResult } = props
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
          {profile && <>
            <select className="filter-select search-scope-select" aria-label="Search scope" value={searchScope} onChange={event => onSearchScopeChange(event.target.value as 'board' | 'workspace')}>
              <option value="board">This board</option><option value="workspace">Workspace</option>
            </select>
            <select className="filter-select" aria-label="Filter by assignee" value={assigneeFilter} onChange={event => onAssigneeFilterChange(event.target.value)}>
              <option value="">Any assignee</option>{members.map(member => <option key={member.userId} value={member.userId}>{member.fullName}</option>)}
            </select>
            <select className="filter-select" aria-label="Filter by label" value={labelFilter} onChange={event => onLabelFilterChange(event.target.value)}>
              <option value="">Any label</option>{labels.map(label => <option key={label.id} value={label.name}>{label.name}</option>)}
            </select>
            <label className="date-filter"><span>Due from</span><input aria-label="Due from" type="date" value={dueFromFilter} onChange={event => onDueFromFilterChange(event.target.value)} /></label>
            <label className="date-filter"><span>Due to</span><input aria-label="Due to" type="date" value={dueToFilter} onChange={event => onDueToFilterChange(event.target.value)} /></label>
          </>}
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
      {profile && searchActive && <div className="search-results-panel">
        <header><div><b>Search results</b><span>{searchLoading ? 'Searching…' : `${searchResults?.length ?? 0} shown`}</span></div></header>
        {searchError ? <p className="search-empty">{searchError}</p> : searchLoading && !searchResults?.length ? <p className="search-empty">Searching cards…</p> : !searchResults?.length ? <p className="search-empty">No cards match these filters.</p> : <div className="search-result-list">
          {searchResults.map(card => <button className="search-result" key={card.id} onClick={() => onOpenSearchResult(card)}>
            <span className={`priority priority-${card.priority.toLowerCase()}`}><i />{card.priority}</span>
            <span className="search-result-main"><b>{card.title}</b><small>{card.boardName} · {card.listName}</small></span>
            <span className="search-result-labels">{card.labels.map(label => <i key={label.id}>{label.name}</i>)}</span>
            <span className="search-result-due">{card.dueDate ? new Date(card.dueDate).toLocaleDateString() : 'No due date'}</span>
          </button>)}
        </div>}
        <footer><span>Page {searchPage + 1}</span><div><button disabled={searchPage === 0 || searchLoading} onClick={() => onSearchPageChange(searchPage - 1)}>Previous</button><button disabled={!searchResultsHasNext || searchLoading} onClick={() => onSearchPageChange(searchPage + 1)}>Next</button></div></footer>
      </div>}
      {(!profile || hasBoard) && (
        <div className="kanban" style={profile && searchActive ? { display: 'none' } : undefined}>
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
                  <TaskCard key={task.id} task={task} canEdit={canEdit} onEdit={() => onEditTask(task.id)} onMove={() => onMoveTask(task.id, column.id)} onOpen={() => onOpenTask(task.id)} />
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
