import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { ArrowRight } from 'lucide-react'
import {
  createBoard,
  createCard,
  createWorkspace,
  getBoard,
  getProfile,
  inviteWorkspaceMember,
  listBoards,
  listWorkspaceMembers,
  listWorkspaces,
  moveCard,
  registerAccount,
  signIn,
  signOut,
} from './api/client'
import type {
  Board,
  BoardSummary,
  CardPriority,
  UserProfile,
  Workspace,
  WorkspaceMember,
} from './api/client'
import { AccountModal } from './features/auth'
import { BoardView, CreateBoardModal, ProjectHeader, ProjectSummary, TaskModal } from './features/board'
import { CreateWorkspaceModal, Sidebar } from './features/workspace'
import { Toast, TopBar } from './shared/components'
import { cardToTask, listTones, previewColumns, type Column } from './shared/types'

function App() {
  const [preview, setPreview] = useState(previewColumns)
  const [query, setQuery] = useState('')
  const [filter, setFilter] = useState('All tasks')
  const [taskModalOpen, setTaskModalOpen] = useState(false)
  const [authOpen, setAuthOpen] = useState(false)
  const [authMode, setAuthMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  const [authError, setAuthError] = useState('')
  const [notice, setNotice] = useState('')
  const [taskTitle, setTaskTitle] = useState('')
  const [profile, setProfile] = useState<UserProfile | null>(null)
  const [workspaces, setWorkspaces] = useState<Workspace[]>([])
  const [activeWorkspaceId, setActiveWorkspaceId] = useState<number | null>(null)
  const [workspaceMembers, setWorkspaceMembers] = useState<WorkspaceMember[]>([])
  const [workspaceMenuOpen, setWorkspaceMenuOpen] = useState(false)
  const [workspaceModalOpen, setWorkspaceModalOpen] = useState(false)
  const [workspaceName, setWorkspaceName] = useState('')
  const [workspaceDescription, setWorkspaceDescription] = useState('')
  const [workspaceError, setWorkspaceError] = useState('')
  const [boards, setBoards] = useState<BoardSummary[]>([])
  const [activeBoardId, setActiveBoardId] = useState<number | null>(null)
  const [boardData, setBoardData] = useState<Board | null>(null)
  const [boardLoading, setBoardLoading] = useState(false)
  const [boardError, setBoardError] = useState('')
  const [boardModalOpen, setBoardModalOpen] = useState(false)
  const [boardName, setBoardName] = useState('')
  const [boardModalError, setBoardModalError] = useState('')
  const [taskListId, setTaskListId] = useState<number | null>(null)
  const [taskPriority, setTaskPriority] = useState<CardPriority>('MEDIUM')
  const [taskDueDate, setTaskDueDate] = useState('')

  const activeWorkspace = workspaces.find(workspace => workspace.id === activeWorkspaceId) ?? workspaces[0]
  const displayColumns = useMemo<Column[]>(() => boardData
    ? boardData.lists.map((list, index) => ({ id: String(list.id), listId: list.id, title: list.name, tone: listTones[index % listTones.length], tasks: list.cards.map(card => cardToTask(card, boardData.name)) }))
    : profile ? [] : preview, [boardData, profile, preview])
  const visibleColumns = useMemo(() => displayColumns.map(column => ({ ...column, tasks: column.tasks.filter(task => `${task.title} ${task.project} ${task.labels.join(' ')}`.toLowerCase().includes(query.toLowerCase()) && (filter === 'All tasks' || task.priority === filter)) })), [displayColumns, query, filter])
  const total = displayColumns.reduce((sum, column) => sum + column.tasks.length, 0)
  const inProgressTotal = displayColumns.filter(column => /progress|doing|review/i.test(column.title)).reduce((sum, column) => sum + column.tasks.length, 0)
  const completedTotal = displayColumns.filter(column => /done|complete/i.test(column.title)).reduce((sum, column) => sum + column.tasks.length, 0)
  const upcomingTask = displayColumns.flatMap(column => column.tasks).find(task => task.due !== 'No due date')
  const canEdit = !profile || activeWorkspace?.role !== 'VIEWER'

  useEffect(() => {
    getProfile().then(async current => {
      setProfile(current)
      const items = await listWorkspaces()
      setWorkspaces(items)
      setActiveWorkspaceId(items[0]?.id ?? null)
    }).catch(() => { /* A signed-out visitor can still preview the sample board. */ })
  }, [])

  useEffect(() => {
    if (!activeWorkspaceId || !profile) {
      setWorkspaceMembers([])
      return
    }
    listWorkspaceMembers(activeWorkspaceId).then(setWorkspaceMembers).catch(error => {
      setWorkspaceMembers([])
      setNotice(error instanceof Error ? error.message : 'Could not load workspace members.')
    })
  }, [activeWorkspaceId, profile])

  useEffect(() => {
    let active = true
    setBoardData(null)
    setActiveBoardId(null)
    if (!activeWorkspaceId || !profile) {
      setBoards([])
      setBoardLoading(false)
      setBoardError('')
      return () => { active = false }
    }
    setBoardLoading(true)
    setBoardError('')
    listBoards(activeWorkspaceId).then(items => {
      if (!active) return
      setBoards(items)
      setActiveBoardId(items[0]?.id ?? null)
    }).catch(error => { if (active) setBoardError(error instanceof Error ? error.message : 'Could not load boards.') })
      .finally(() => { if (active) setBoardLoading(false) })
    return () => { active = false }
  }, [activeWorkspaceId, profile])

  useEffect(() => {
    let active = true
    if (!activeBoardId || !profile) {
      setBoardData(null)
      return () => { active = false }
    }
    setBoardData(null)
    setBoardLoading(true)
    setBoardError('')
    getBoard(activeBoardId).then(board => { if (active) setBoardData(board) })
      .catch(error => {
        if (active) {
          setBoardData(null)
          setBoardError(error instanceof Error ? error.message : 'Could not load this board.')
        }
      })
      .finally(() => { if (active) setBoardLoading(false) })
    return () => { active = false }
  }, [activeBoardId, profile])

  async function reloadBoard(boardId = activeBoardId) {
    if (!boardId) return
    setBoardData(await getBoard(boardId))
  }

  function openTaskModal(listId?: number) {
    setTaskListId(listId ?? boardData?.lists[0]?.id ?? null)
    setTaskPriority('MEDIUM')
    setTaskDueDate('')
    setTaskTitle('')
    setTaskModalOpen(true)
  }

  async function addTask(event: FormEvent) {
    event.preventDefault()
    if (!taskTitle.trim()) return
    if (profile) {
      if (!boardData || !taskListId) {
        setNotice('Create or select a board before adding a task.')
        return
      }
      try {
        await createCard(taskListId, { title: taskTitle.trim(), priority: taskPriority, ...(taskDueDate ? { dueDate: new Date(`${taskDueDate}T23:59:59`).toISOString() } : {}) })
        await reloadBoard()
        setTaskTitle('')
        setTaskModalOpen(false)
        setNotice('Task saved to the database.')
      } catch (error) { setNotice(error instanceof Error ? error.message : 'Could not save this task.') }
      return
    }
    setPreview(columns => columns.map(column => {
      if (column.id !== 'todo') return column
      const task = {
        id: Date.now(),
        title: taskTitle.trim(),
        project: 'Product design',
        priority: 'Medium' as const,
        labels: ['New task'],
        due: 'Nov 20',
        assignees: ['AL'],
      }
      return { ...column, tasks: [task, ...column.tasks] }
    }))
    setTaskTitle('')
    setTaskModalOpen(false)
    setNotice('Added to the preview board.')
  }

  async function moveTask(taskId: number, fromId: string) {
    const source = displayColumns.find(column => column.id === fromId)?.tasks.find(task => task.id === taskId)
    if (!source) return
    if (boardData) {
      const fromIndex = boardData.lists.findIndex(list => String(list.id) === fromId)
      const destination = boardData.lists[(fromIndex + 1) % boardData.lists.length]
      if (!destination || source.version === undefined) return
      const lastCard = destination.cards.reduce((last, card) => Math.max(last, card.position), 0)
      try {
        await moveCard(source.id, { listId: destination.id, position: lastCard + 1000, version: source.version })
        await reloadBoard()
        setNotice(`Moved to ${destination.name} and saved.`)
      } catch (error) {
        if (error instanceof Error && error.message.includes('changed since')) await reloadBoard()
        setNotice(error instanceof Error ? error.message : 'Could not move this task.')
      }
      return
    }
    const order = ['todo', 'progress', 'review', 'done']
    const destination = order[(order.indexOf(fromId) + 1) % order.length]
    setPreview(columns => columns.map(column => {
      if (column.id === fromId) {
        return { ...column, tasks: column.tasks.filter(task => task.id !== taskId) }
      }
      if (column.id === destination) {
        return { ...column, tasks: [...column.tasks, source] }
      }
      return column
    }))
    setNotice(`Moved in the preview board to ${preview.find(column => column.id === destination)?.title}`)
  }

  async function submitAuth(event: FormEvent) {
    event.preventDefault()
    setAuthError('')
    try {
      if (authMode === 'login') {
        const data = await signIn(email, password)
        setProfile({ id: data.id, email: data.email, fullName: data.fullName })
        const items = await listWorkspaces()
        setWorkspaces(items)
        setActiveWorkspaceId(items[0]?.id ?? null)
        setAuthOpen(false)
        setNotice(`Welcome back${data.fullName ? `, ${data.fullName.split(' ')[0]}` : ''}!`)
      } else {
        await registerAccount(email, password, fullName)
        setAuthMode('login')
        setNotice('Account created. Sign in to get started.')
      }
    } catch (error) {
      setAuthError(error instanceof Error && error.message.includes('Failed to fetch')
        ? 'Can’t reach the API. Start the backend and try again.'
        : error instanceof Error ? error.message : 'Something went wrong.')
    }
  }

  async function submitWorkspace(event: FormEvent) {
    event.preventDefault()
    setWorkspaceError('')
    if (!profile) {
      setWorkspaceModalOpen(false)
      setAuthMode('login')
      setAuthOpen(true)
      setNotice('Sign in to create a workspace.')
      return
    }
    try {
      const workspace = await createWorkspace(workspaceName.trim(), workspaceDescription.trim())
      setWorkspaces(current => [...current, workspace])
      setActiveWorkspaceId(workspace.id)
      setWorkspaceName('')
      setWorkspaceDescription('')
      setWorkspaceModalOpen(false)
      setNotice(`Workspace “${workspace.name}” created.`)
    } catch (error) { setWorkspaceError(error instanceof Error ? error.message : 'Could not create workspace.') }
  }

  async function submitBoard(event: FormEvent) {
    event.preventDefault()
    setBoardModalError('')
    if (!profile) {
      setBoardModalOpen(false)
      setAuthMode('login')
      setAuthOpen(true)
      setNotice('Sign in to create a board.')
      return
    }
    if (!activeWorkspace) {
      setBoardModalError('Create a workspace first.')
      return
    }
    try {
      const created = await createBoard(activeWorkspace.id, boardName.trim())
      setBoards(current => [...current, {
        id: created.id,
        workspaceId: created.workspaceId,
        name: created.name,
        position: current.length + 1,
        archived: false,
        createdAt: created.createdAt,
      }])
      setBoardData(created)
      setActiveBoardId(created.id)
      setBoardName('')
      setBoardModalOpen(false)
      setNotice(`Board “${created.name}” saved to the database.`)
    } catch (error) { setBoardModalError(error instanceof Error ? error.message : 'Could not create board.') }
  }

  async function inviteMember() {
    if (!profile) {
      setAuthMode('login')
      setAuthOpen(true)
      setNotice('Sign in to invite teammates.')
      return
    }
    if (!activeWorkspace) {
      setNotice('Create a workspace before inviting teammates.')
      return
    }
    const inviteEmail = window.prompt('Email address to invite')?.trim()
    if (!inviteEmail) return
    try {
      const invitation = await inviteWorkspaceMember(activeWorkspace.id, inviteEmail)
      setNotice(`Invitation created for ${invitation.email}. Share this token: ${invitation.invitationToken}`)
    } catch (error) { setNotice(error instanceof Error ? error.message : 'Could not create invitation.') }
  }

  async function handleSignOut() {
    await signOut()
    setProfile(null)
    setWorkspaces([])
    setActiveWorkspaceId(null)
    setWorkspaceMembers([])
    setBoards([])
    setActiveBoardId(null)
    setBoardData(null)
    setNotice('You have signed out.')
  }

  async function retryBoardLoad() {
    try {
      if (activeBoardId) await reloadBoard(activeBoardId)
      else if (activeWorkspaceId) setBoards(await listBoards(activeWorkspaceId))
      setBoardError('')
    } catch (error) { setBoardError(error instanceof Error ? error.message : 'Could not load boards.') }
  }

  return (
    <div className="app-shell">
      <Sidebar
        profile={profile}
        workspaces={workspaces}
        activeWorkspace={activeWorkspace}
        activeWorkspaceId={activeWorkspaceId}
        workspaceMenu={workspaceMenuOpen}
        onToggleWorkspaceMenu={() => setWorkspaceMenuOpen(open => !open)}
        onSelectWorkspace={id => {
          setActiveWorkspaceId(id)
          setWorkspaceMenuOpen(false)
        }}
        onCreateWorkspace={() => {
          setWorkspaceMenuOpen(false)
          setWorkspaceModalOpen(true)
        }}
        boards={boards}
        activeBoardId={activeBoardId}
        onSelectBoard={setActiveBoardId}
        onCreateBoard={() => setBoardModalOpen(true)}
        onNotice={setNotice}
        onAuth={() => setAuthOpen(true)}
        onSignOut={handleSignOut}
      />

      <main className="main-area" id="top">
        <TopBar
          workspace={activeWorkspace}
          boardName={boardData?.name ?? (profile ? 'No board selected' : 'Product design')}
          profile={profile}
          onNotice={setNotice}
          onAccount={profile ? handleSignOut : () => setAuthOpen(true)}
        />
        <div className="content">
          <ProjectHeader
            title={boardData?.name ?? (profile ? 'Select or create a board' : 'Product design')}
            description={boardData ? `${activeWorkspace?.name} workspace` : profile ? 'Choose a board from your workspace or create one to start.' : 'The little details make the big difference.'}
            isBoardLoaded={Boolean(boardData)}
            profile={profile}
            members={workspaceMembers}
            canAddTask={canEdit && (!profile || Boolean(boardData))}
            onInvite={inviteMember}
            onAddTask={() => openTaskModal()}
            onCreateBoard={() => setBoardModalOpen(true)}
          />
          <ProjectSummary total={total} inProgress={inProgressTotal} completed={completedTotal} upcoming={upcomingTask} isPreview={!profile} />
          <BoardView
            columns={visibleColumns}
            query={query}
            filter={filter}
            profile={profile}
            boards={boards}
            boardLoading={boardLoading}
            boardError={boardError}
            hasBoard={Boolean(boardData)}
            canEdit={canEdit}
            onQueryChange={setQuery}
            onFilterChange={setFilter}
            onNotice={setNotice}
            onRetry={retryBoardLoad}
            onCreateBoard={() => setBoardModalOpen(true)}
            onAddTask={openTaskModal}
            onMoveTask={moveTask}
          />
          <footer className="board-footer">
            <span>
              <span className="live-dot" />
              {profile ? boardData ? 'Changes saved to database' : 'Connected to workspace API' : 'Preview board · changes are not saved'}
            </span>
            <span>
              {activeWorkspace ? `Workspace: ${activeWorkspace.name}` : 'Preview mode'}
              <span className="footer-dot">·</span>
              <button onClick={() => setNotice('Activity API is not available yet.')}>
                View activity <ArrowRight size={12} />
              </button>
            </span>
          </footer>
        </div>
      </main>
      <Toast message={notice} onDismiss={() => setNotice('')} />
      <TaskModal
        open={taskModalOpen}
        board={boardData}
        title={taskTitle}
        listId={taskListId}
        priority={taskPriority}
        dueDate={taskDueDate}
        onTitle={setTaskTitle}
        onList={setTaskListId}
        onPriority={setTaskPriority}
        onDueDate={setTaskDueDate}
        onClose={() => setTaskModalOpen(false)}
        onSubmit={addTask}
      />
      <AccountModal
        open={authOpen}
        mode={authMode}
        email={email}
        password={password}
        fullName={fullName}
        error={authError}
        onEmail={setEmail}
        onPassword={setPassword}
        onFullName={setFullName}
        onMode={mode => {
          setAuthMode(mode)
          setAuthError('')
        }}
        onClose={() => setAuthOpen(false)}
        onSubmit={submitAuth}
      />
      <CreateWorkspaceModal
        open={workspaceModalOpen}
        name={workspaceName}
        description={workspaceDescription}
        error={workspaceError}
        onName={setWorkspaceName}
        onDescription={setWorkspaceDescription}
        onClose={() => setWorkspaceModalOpen(false)}
        onSubmit={submitWorkspace}
      />
      <CreateBoardModal
        open={boardModalOpen}
        workspaceName={activeWorkspace?.name}
        name={boardName}
        error={boardModalError}
        onName={setBoardName}
        onClose={() => setBoardModalOpen(false)}
        onSubmit={submitBoard}
      />
    </div>
  )
}

export default App
