import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { ArrowRight } from 'lucide-react'
import {
  createBoard,
  createCard,
  createWorkspace,
  listNotifications,
  unreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead,
  acceptWorkspaceInvitation,
  acceptPendingWorkspaceInvitation,
  declineWorkspaceInvitation,
  getBoard,
  getProfile,
  inviteWorkspaceMember,
  listBoards,
  listWorkspaceMembers,
  listWorkspaces,
  leaveWorkspace,
  moveCard,
  listBoardLabels,
  listWorkspaceLabels,
  searchCards,
  registerAccount,
  signIn,
  signOut,
  setCardAssignees,
  setCardLabels,
  updateCard,
} from './api/client'
import type { CardLabel, NotificationItem, SearchCard } from './api/client'
import { connectNotificationSocket } from './api/notificationSocket'
import type {
  Board,
  BoardSummary,
  CardPriority,
  UserProfile,
  Workspace,
  WorkspaceMember,
} from './api/client'
import { AccountModal } from './features/auth'
import { BoardView, CreateBoardModal, EditTaskModal, ProjectHeader, ProjectSummary, TaskModal } from './features/board'
import { AcceptInvitationModal, CreateWorkspaceModal, InviteMemberModal, LeaveWorkspaceModal, Sidebar } from './features/workspace'
import { InvitationDecisionModal } from './features/workspace/components/InvitationDecisionModal'
import { TaskDiscussionModal } from './features/board/components/TaskDiscussionModal'
import { Toast, TopBar } from './shared/components'
import { cardToTask, listTones, previewColumns, type Column, type Task } from './shared/types'

function App() {
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    try {
      const saved = localStorage.getItem('syncflow_theme')
      if (saved === 'light' || saved === 'dark') return saved
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    } catch { return 'light' }
  })
  const [preview, setPreview] = useState(previewColumns)
  const [query, setQuery] = useState('')
  const [filter, setFilter] = useState('All tasks')
  const [searchScope, setSearchScope] = useState<'board' | 'workspace'>('board')
  const [assigneeFilter, setAssigneeFilter] = useState('')
  const [labelFilter, setLabelFilter] = useState('')
  const [dueFromFilter, setDueFromFilter] = useState('')
  const [dueToFilter, setDueToFilter] = useState('')
  const [searchResults, setSearchResults] = useState<SearchCard[] | null>(null)
  const [searchResultsHasNext, setSearchResultsHasNext] = useState(false)
  const [searchPage, setSearchPage] = useState(0)
  const [searchLoading, setSearchLoading] = useState(false)
  const [searchError, setSearchError] = useState('')
  const [searchRefresh, setSearchRefresh] = useState(0)
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
  const [leaveWorkspaceOpen, setLeaveWorkspaceOpen] = useState(false)
  const [leavingWorkspace, setLeavingWorkspace] = useState(false)
  const [workspaceModalOpen, setWorkspaceModalOpen] = useState(false)
  const [workspaceName, setWorkspaceName] = useState('')
  const [workspaceDescription, setWorkspaceDescription] = useState('')
  const [workspaceError, setWorkspaceError] = useState('')
  const [boards, setBoards] = useState<BoardSummary[]>([])
  const [activeBoardId, setActiveBoardId] = useState<number | null>(null)
  const [boardData, setBoardData] = useState<Board | null>(null)
  const [boardLabels, setBoardLabels] = useState<CardLabel[]>([])
  const [boardLoading, setBoardLoading] = useState(false)
  const [boardError, setBoardError] = useState('')
  const [boardModalOpen, setBoardModalOpen] = useState(false)
  const [boardName, setBoardName] = useState('')
  const [boardModalError, setBoardModalError] = useState('')
  const [taskListId, setTaskListId] = useState<number | null>(null)
  const [taskPriority, setTaskPriority] = useState<CardPriority>('MEDIUM')
  const [taskDueDate, setTaskDueDate] = useState('')
  const [editingTask, setEditingTask] = useState<Task | null>(null)
  const [editingTitle, setEditingTitle] = useState('')
  const [editingDescription, setEditingDescription] = useState('')
  const [editingPriority, setEditingPriority] = useState<CardPriority>('MEDIUM')
  const [editingDueDate, setEditingDueDate] = useState('')
  const [editingLabels, setEditingLabels] = useState('')
  const [editingAssigneeIds, setEditingAssigneeIds] = useState<number[]>([])
  const [editingError, setEditingError] = useState('')
  const [invitationModalOpen, setInvitationModalOpen] = useState(false)
  const [invitationToken, setInvitationToken] = useState('')
  const [invitationError, setInvitationError] = useState('')
  const [inviteCreateModalOpen, setInviteCreateModalOpen] = useState(false)
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState<'MEMBER' | 'VIEWER'>('MEMBER')
  const [inviteLink, setInviteLink] = useState('')
  const [inviteCopied, setInviteCopied] = useState(false)
  const [inviteCreateError, setInviteCreateError] = useState('')
  const [pendingInvitationToken, setPendingInvitationToken] = useState<string | null>(null)
  const [pendingInvitationDecision, setPendingInvitationDecision] = useState<{ id: number; workspaceName: string } | null>(null)
  const [discussionTaskId, setDiscussionTaskId] = useState<number | null>(null)
  const [discussionTaskTitle, setDiscussionTaskTitle] = useState('')
  const [boardActivityOpen, setBoardActivityOpen] = useState(false)
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [notificationsHasNext, setNotificationsHasNext] = useState(false)
  const [notificationsPage, setNotificationsPage] = useState(0)
  const [unreadCount, setUnreadCount] = useState(0)
  const [notificationsOpen, setNotificationsOpen] = useState(false)

  useEffect(() => {
    if (!profile) { setNotifications([]); setUnreadCount(0); setNotificationsPage(0); setNotificationsHasNext(false); return }
    let active = true
    const load = () => Promise.all([listNotifications(notificationsPage), unreadNotificationCount()]).then(([page, unread]) => {
      if (active) {
        setNotifications(current => notificationsPage === 0 ? page.content : [...current, ...page.content.filter(item => !current.some(existing => existing.id === item.id))])
        setNotificationsHasNext(page.hasNext)
        setUnreadCount(unread.count)
      }
    }).catch(() => undefined)
    void load()
    const disconnect = connectNotificationSocket(item => {
      setNotifications(current => [item, ...current.filter(existing => existing.id !== item.id)].slice(0, 20))
      if (!item.read) setUnreadCount(count => count + 1)
    }, activeBoardId, () => {
      if (active && activeBoardId !== null) {
        void reloadBoard(activeBoardId).catch(() => undefined)
        setSearchRefresh(value => value + 1)
      }
    }, () => {
      if (active && activeBoardId !== null) void reloadBoard(activeBoardId).catch(() => undefined)
    })
    return () => { active = false; disconnect() }
  }, [profile, activeBoardId, notificationsPage])

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
  const searchActive = Boolean(query.trim() || filter !== 'All tasks' || assigneeFilter || labelFilter || dueFromFilter || dueToFilter)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', theme === 'dark' ? '#17191e' : '#f7f8fa')
    try { localStorage.setItem('syncflow_theme', theme) } catch { /* Theme remains active for this session. */ }
  }, [theme])

  useEffect(() => {
    const inviteFromUrl = new URLSearchParams(window.location.search).get('invite')
    if (inviteFromUrl) setPendingInvitationToken(inviteFromUrl)
    getProfile().then(async current => {
      setProfile(current)
      const items = await listWorkspaces()
      setWorkspaces(items)
      setActiveWorkspaceId(items[0]?.id ?? null)
    }).catch(() => {
      if (inviteFromUrl) {
        setAuthMode('login')
        setAuthOpen(true)
        setNotice('Sign in with the invited email address to review this workspace invitation.')
      }
    })
  }, [])

  useEffect(() => {
    if (!pendingInvitationToken) return
    if (!profile) {
      setAuthMode('login')
      setAuthOpen(true)
      setNotice('Sign in with the invited email to review this workspace invitation.')
      return
    }
    setInvitationToken(pendingInvitationToken)
    setInvitationError('')
    setInvitationModalOpen(true)
  }, [profile, pendingInvitationToken])

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

  useEffect(() => {
    let active = true
    setBoardLabels([])
    if (!profile || (searchScope === 'board' && !activeBoardId) || !activeWorkspaceId) return () => { active = false }
    const request = searchScope === 'workspace'
      ? listWorkspaceLabels(activeWorkspaceId)
      : listBoardLabels(activeBoardId!)
    request.then(labels => { if (active) setBoardLabels(labels) })
      .catch(() => { if (active) setBoardLabels([]) })
    return () => { active = false }
  }, [activeBoardId, activeWorkspaceId, profile, searchScope])

  useEffect(() => {
    let active = true
    if (!profile || !searchActive || (searchScope === 'board' && !activeBoardId)) {
      setSearchResults(null)
      setSearchResultsHasNext(false)
      setSearchLoading(false)
      setSearchError('')
      return () => { active = false }
    }
    const timer = window.setTimeout(async () => {
      setSearchLoading(true)
      setSearchError('')
      try {
        let dueTo: string | undefined
        if (dueToFilter) {
          const nextDay = new Date(`${dueToFilter}T00:00:00.000Z`)
          nextDay.setUTCDate(nextDay.getUTCDate() + 1)
          dueTo = nextDay.toISOString()
        }
        const result = await searchCards({
          ...(searchScope === 'board' && activeBoardId ? { boardId: activeBoardId } : { workspaceId: activeWorkspaceId ?? undefined }),
          q: query.trim() || undefined,
          priority: filter === 'All tasks' ? undefined : filter.toUpperCase() as CardPriority,
          assigneeId: assigneeFilter ? Number(assigneeFilter) : undefined,
          label: labelFilter || undefined,
          dueFrom: dueFromFilter ? new Date(`${dueFromFilter}T00:00:00.000Z`).toISOString() : undefined,
          dueTo,
          page: searchPage,
          size: 30,
        })
        if (active) {
          setSearchResults(result.content)
          setSearchResultsHasNext(result.hasNext)
        }
      } catch (error) {
        if (active) {
          setSearchResults([])
          setSearchResultsHasNext(false)
          setSearchError(error instanceof Error ? error.message : 'Search failed.')
        }
      } finally { if (active) setSearchLoading(false) }
    }, query.trim() ? 250 : 0)
    return () => { active = false; window.clearTimeout(timer) }
  }, [profile, activeBoardId, activeWorkspaceId, searchActive, searchScope, query, filter, assigneeFilter, labelFilter, dueFromFilter, dueToFilter, searchPage, searchRefresh])

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

  function openEditTask(taskId: number) {
    const task = displayColumns.flatMap(column => column.tasks).find(item => item.id === taskId)
    if (!task) return
    setEditingTask(task)
    setEditingTitle(task.title)
    setEditingDescription(task.description ?? '')
    setEditingPriority(task.priority.toUpperCase() as CardPriority)
    setEditingDueDate(task.dueDate?.slice(0, 10) ?? '')
    setEditingLabels(task.labels.join(', '))
    setEditingAssigneeIds(task.assigneeUserIds ?? [])
    setEditingError('')
  }

  function openTaskDiscussion(taskId: number) {
    const task = displayColumns.flatMap(column => column.tasks).find(item => item.id === taskId)
    if (task) setDiscussionTaskTitle(task.title)
    setDiscussionTaskId(taskId)
  }

  function openSearchResult(result: SearchCard) {
    setActiveBoardId(result.boardId)
    setDiscussionTaskTitle(result.title)
    setDiscussionTaskId(result.id)
  }

  async function readNotification(id: number) {
    try {
      const updated = await markNotificationRead(id)
      setNotifications(current => current.map(item => item.id === id ? updated : item))
      setUnreadCount(count => Math.max(0, count - 1))
    } catch (error) { setNotice(error instanceof Error ? error.message : 'Could not mark notification as read.') }
  }

  async function readAllNotifications() {
    try {
      await markAllNotificationsRead()
      setNotifications(current => current.map(item => ({ ...item, read: true })))
      setUnreadCount(0)
    } catch (error) { setNotice(error instanceof Error ? error.message : 'Could not mark notifications as read.') }
  }

  async function selectNotification(item: NotificationItem) {
    setNotificationsOpen(false)
    if (!item.read) await readNotification(item.id)
    const invitationId = item.payload?.invitationId
    if (item.type === 'WORKSPACE_INVITATION' && typeof invitationId === 'number') {
      setPendingInvitationDecision({ id: invitationId, workspaceName: String(item.payload.workspaceName ?? 'workspace') })
      return
    }
    setNotice(item.message)
  }

  async function decidePendingInvitation(accept: boolean) {
    if (!pendingInvitationDecision) return
    try {
      if (accept) {
        const accepted = await acceptPendingWorkspaceInvitation(pendingInvitationDecision.id)
        const updated = await listWorkspaces()
        setWorkspaces(updated)
        setActiveWorkspaceId(accepted.workspaceId)
        setNotice(`Joined workspace “${accepted.workspaceName}” as ${accepted.role.toLowerCase()}.`)
      } else {
        await declineWorkspaceInvitation(pendingInvitationDecision.id)
        setNotice('Invitation declined. You have not joined the workspace.')
      }
      setPendingInvitationDecision(null)
      setNotifications(current => current.filter(item => item.payload?.invitationId !== pendingInvitationDecision.id))
    } catch (error) { setNotice(error instanceof Error ? error.message : 'Could not process this invitation.') }
  }

  async function saveTaskChanges(event: FormEvent) {
    event.preventDefault()
    if (!editingTask || !editingTitle.trim()) return
    setEditingError('')
    try {
      const dueDate = editingDueDate ? new Date(`${editingDueDate}T23:59:59`).toISOString() : null
      if (profile) {
        if (editingTask.version === undefined) throw new Error('Task version is missing. Reload the board and try again.')
        const updated = await updateCard(editingTask.id, {
          title: editingTitle.trim(),
          description: editingDescription.trim() || null,
          dueDate,
          priority: editingPriority,
          version: editingTask.version,
        })
        const oldAssigneeIds = [...(editingTask.assigneeUserIds ?? [])].sort((a, b) => a - b)
        const newAssigneeIds = [...editingAssigneeIds].sort((a, b) => a - b)
        if (oldAssigneeIds.join(',') !== newAssigneeIds.join(',')) {
          await setCardAssignees(editingTask.id, { userIds: editingAssigneeIds, version: updated.version })
        }
        const oldLabels = [...editingTask.labels].map(label => label.trim().toLowerCase()).sort()
        const nextLabels = editingLabels.split(',').map(label => label.trim()).filter(Boolean)
        if (oldLabels.join(',') !== nextLabels.map(label => label.toLowerCase()).sort().join(',')) {
          await setCardLabels(editingTask.id, nextLabels)
        }
        await reloadBoard()
      } else {
        setPreview(columns => columns.map(column => ({
          ...column,
          tasks: column.tasks.map(task => task.id === editingTask.id ? {
            ...task,
            title: editingTitle.trim(),
            description: editingDescription.trim() || null,
            priority: editingPriority[0] + editingPriority.slice(1).toLowerCase() as Task['priority'],
            dueDate,
            due: dueDate ? new Date(dueDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) : 'No due date',
          } : task),
        })))
      }
      setEditingTask(null)
      setNotice(profile ? 'Task changes saved.' : 'Preview task updated.')
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Could not update this task.'
      if (message.includes('changed since')) await reloadBoard().catch(() => undefined)
      setEditingError(message)
    }
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
    const order = ['draft', 'pending', 'progress', 'blocked', 'review', 'reopen', 'done', 'cancelled']
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

  async function confirmLeaveWorkspace() {
    if (!activeWorkspace || activeWorkspace.role === 'OWNER') return
    setLeavingWorkspace(true)
    try {
      await leaveWorkspace(activeWorkspace.id)
      const remaining = await listWorkspaces()
      setWorkspaces(remaining)
      setActiveWorkspaceId(remaining[0]?.id ?? null)
      setLeaveWorkspaceOpen(false)
      setNotice(`You left “${activeWorkspace.name}”.`)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : 'Could not leave this workspace.')
    } finally { setLeavingWorkspace(false) }
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

  function inviteMember() {
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
    setInviteEmail('')
    setInviteRole('MEMBER')
    setInviteLink('')
    setInviteCopied(false)
    setInviteCreateError('')
    setInviteCreateModalOpen(true)
  }

  async function createInvitation(event: FormEvent) {
    event.preventDefault()
    setInviteCreateError('')
    if (!activeWorkspace) return
    try {
      const invitation = await inviteWorkspaceMember(activeWorkspace.id, inviteEmail.trim(), inviteRole)
      const link = new URL('/', window.location.origin)
      link.searchParams.set('invite', invitation.invitationToken)
      setInviteLink(link.toString())
    } catch (error) {
      setInviteCreateError(error instanceof Error ? error.message : 'Could not create invitation link.')
    }
  }

  async function copyInvitationLink() {
    try {
      await navigator.clipboard.writeText(inviteLink)
      setInviteCopied(true)
      setInviteCreateError('')
    } catch {
      setInviteCreateError('Clipboard access failed. Select and copy the link from the field above.')
    }
  }

  function openJoinWorkspace() {
    setWorkspaceMenuOpen(false)
    if (!profile) {
      setAuthMode('login')
      setAuthOpen(true)
      setNotice('Sign in with the invited email before joining a workspace.')
      return
    }
    setInvitationError('')
    setInvitationToken('')
    setInvitationModalOpen(true)
  }

  async function acceptInvitation(event: FormEvent) {
    event.preventDefault()
    setInvitationError('')
    try {
      let token = invitationToken.trim()
      try { token = new URL(token).searchParams.get('invite') ?? token } catch { /* Accept a token pasted from an older invitation message. */ }
      const accepted = await acceptWorkspaceInvitation(token)
      const updatedWorkspaces = await listWorkspaces()
      setWorkspaces(updatedWorkspaces)
      setActiveWorkspaceId(accepted.workspaceId)
      setInvitationToken('')
      setInvitationModalOpen(false)
      setPendingInvitationToken(null)
      setNotice(`Joined workspace “${accepted.workspaceName}” as ${accepted.role.toLowerCase()}.`)
      const url = new URL(window.location.href)
      url.searchParams.delete('invite')
      window.history.replaceState({}, '', url)
    } catch (error) {
      setInvitationError(error instanceof Error ? error.message : 'Could not accept this invitation.')
    }
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
        onJoinWorkspace={openJoinWorkspace}
        onLeaveWorkspace={() => {
          setWorkspaceMenuOpen(false)
          setLeaveWorkspaceOpen(true)
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
          theme={theme}
          onToggleTheme={() => setTheme(current => current === 'light' ? 'dark' : 'light')}
          onNotice={setNotice}
          notifications={notifications}
          unreadCount={unreadCount}
          notificationsOpen={notificationsOpen}
          notificationsHasNext={notificationsHasNext}
          onToggleNotifications={() => setNotificationsOpen(open => !open)}
          onSelectNotification={item => void selectNotification(item)}
          onReadAll={() => void readAllNotifications()}
          onLoadMoreNotifications={() => setNotificationsPage(page => page + 1)}
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
            members={workspaceMembers}
            labels={boardLabels}
            searchScope={searchScope}
            assigneeFilter={assigneeFilter}
            labelFilter={labelFilter}
            dueFromFilter={dueFromFilter}
            dueToFilter={dueToFilter}
            searchActive={searchActive}
            searchResults={searchResults}
            searchResultsHasNext={searchResultsHasNext}
            searchPage={searchPage}
            searchLoading={searchLoading}
            searchError={searchError}
            onQueryChange={value => { setQuery(value); setSearchPage(0) }}
            onFilterChange={value => { setFilter(value); setSearchPage(0) }}
            onSearchScopeChange={value => { setSearchScope(value); setSearchPage(0) }}
            onAssigneeFilterChange={value => { setAssigneeFilter(value); setSearchPage(0) }}
            onLabelFilterChange={value => { setLabelFilter(value); setSearchPage(0) }}
            onDueFromFilterChange={value => { setDueFromFilter(value); setSearchPage(0) }}
            onDueToFilterChange={value => { setDueToFilter(value); setSearchPage(0) }}
            onSearchPageChange={setSearchPage}
            onOpenSearchResult={openSearchResult}
            onNotice={setNotice}
            onRetry={retryBoardLoad}
            onCreateBoard={() => setBoardModalOpen(true)}
            onAddTask={openTaskModal}
            onEditTask={openEditTask}
            onMoveTask={moveTask}
            onOpenTask={openTaskDiscussion}
          />
          <footer className="board-footer">
            <span>
              <span className="live-dot" />
              {profile ? boardData ? 'Changes saved to database' : 'Connected to workspace API' : 'Preview board · changes are not saved'}
            </span>
            <span>
              {activeWorkspace ? `Workspace: ${activeWorkspace.name}` : 'Preview mode'}
              <span className="footer-dot">·</span>
              <button onClick={() => setBoardActivityOpen(true)}>
                View activity <ArrowRight size={12} />
              </button>
            </span>
          </footer>
        </div>
      </main>
      <Toast message={notice} onDismiss={() => setNotice('')} />
      {discussionTaskId !== null && <TaskDiscussionModal cardId={discussionTaskId} taskTitle={discussionTaskTitle || displayColumns.flatMap(column => column.tasks).find(task => task.id === discussionTaskId)?.title || 'Task discussion'} currentUserId={profile?.id ?? 0} isOwner={activeWorkspace?.role === 'OWNER'} members={workspaceMembers} onClose={() => setDiscussionTaskId(null)} onNotice={setNotice} />}
      {boardActivityOpen && activeBoardId && <TaskDiscussionModal boardId={activeBoardId} currentUserId={profile?.id ?? 0} isOwner={activeWorkspace?.role === 'OWNER'} members={workspaceMembers} onClose={() => setBoardActivityOpen(false)} onNotice={setNotice} />}
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
      <EditTaskModal
        task={editingTask}
        title={editingTitle}
        description={editingDescription}
        priority={editingPriority}
        dueDate={editingDueDate}
        labels={editingLabels}
        members={workspaceMembers}
        assigneeIds={editingAssigneeIds}
        error={editingError}
        onTitle={setEditingTitle}
        onDescription={setEditingDescription}
        onPriority={setEditingPriority}
        onDueDate={setEditingDueDate}
        onLabels={setEditingLabels}
        onAssignees={setEditingAssigneeIds}
        onClose={() => setEditingTask(null)}
        onSubmit={saveTaskChanges}
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
      <LeaveWorkspaceModal open={leaveWorkspaceOpen} workspaceName={activeWorkspace?.name ?? ''} isOwner={activeWorkspace?.role === 'OWNER'} busy={leavingWorkspace} onClose={() => setLeaveWorkspaceOpen(false)} onConfirm={() => void confirmLeaveWorkspace()} />
      <AcceptInvitationModal
        open={invitationModalOpen}
        token={invitationToken}
        error={invitationError}
        onToken={setInvitationToken}
        onClose={() => setInvitationModalOpen(false)}
        onSubmit={acceptInvitation}
      />
      <InvitationDecisionModal open={Boolean(pendingInvitationDecision)} workspaceName={pendingInvitationDecision?.workspaceName ?? ''} onClose={() => setPendingInvitationDecision(null)} onAccept={() => void decidePendingInvitation(true)} onDecline={() => void decidePendingInvitation(false)} />
      <InviteMemberModal
        open={inviteCreateModalOpen}
        email={inviteEmail}
        role={inviteRole}
        invitationLink={inviteLink}
        copied={inviteCopied}
        error={inviteCreateError}
        onEmail={setInviteEmail}
        onRole={setInviteRole}
        onClose={() => {
          setInviteCreateModalOpen(false)
          setInviteLink('')
          setInviteCopied(false)
        }}
        onSubmit={createInvitation}
        onCopy={copyInvitationLink}
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
