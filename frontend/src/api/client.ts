type AuthPayload = {
  id: number
  email: string
  fullName: string
  accessToken: string
  tokenType: string
  expiresIn: number
}

export type UserProfile = {
  id: number
  email: string
  fullName: string
}
export type Workspace = {
  id: number
  name: string
  description: string | null
  ownerId: number
  role: 'OWNER' | 'MEMBER' | 'VIEWER'
}
export type WorkspaceMember = {
  userId: number
  email: string
  fullName: string
  role: 'OWNER' | 'MEMBER' | 'VIEWER'
  joinedAt: string
}
export type WorkspaceInvitation = {
  workspaceId: number
  email: string
  role: 'MEMBER' | 'VIEWER'
  invitationToken: string
  expiresAt: string
}
export type CardPriority = 'LOW' | 'MEDIUM' | 'HIGH'
export type CardAssignee = {
  userId: number
  email: string
  fullName: string
}
export type CardLabel = { id: number; name: string; color: string }
export type Card = {
  id: number
  title: string
  description: string | null
  position: number
  dueDate: string | null
  priority: CardPriority
  version: number
  createdById: number | null
  assignees: CardAssignee[]
  labels: CardLabel[]
}
export type BoardList = {
  id: number
  name: string
  position: number
  cards: Card[]
}
export type Board = {
  id: number
  workspaceId: number
  name: string
  archived: boolean
  createdAt: string
  lists: BoardList[]
}
export type BoardSummary = {
  id: number
  workspaceId: number
  name: string
  position: number
  archived: boolean
  createdAt: string
}

export type PageResponse<T> = { content: T[]; page: number; size: number; totalElements: number; hasNext: boolean }
export type CommentItem = { id: number; cardId: number; authorId: number; authorName: string; authorEmail: string; content: string; createdAt: string; editedAt: string | null }
export type ActivityItem = { id: number; workspaceId: number; boardId: number | null; cardId: number | null; actorId: number | null; actorName: string | null; action: string; metadata: Record<string, unknown>; createdAt: string }
export type NotificationItem = { id: number; type: string; message: string; payload: Record<string, unknown>; read: boolean; createdAt: string }
export type BoardRealtimeEvent = { id: number; boardId: number; cardId: number | null; actorId: number | null; actorName: string | null; action: string; metadata: Record<string, unknown>; createdAt: string }
export type SearchCard = { id: number; title: string; description: string | null; dueDate: string | null; priority: CardPriority; version: number; boardId: number; boardName: string; workspaceId: number; listId: number; listName: string; assignees: CardAssignee[]; labels: CardLabel[] }

let accessToken: string | null = null
let accessTokenExpiresAt = 0
let refreshInFlight: Promise<AuthPayload> | null = null

if (typeof localStorage !== 'undefined') {
  try {
    localStorage.removeItem('syncflow_access_token')
    localStorage.removeItem('syncflow_refresh_token')
  } catch {
    // Storage can be unavailable in restricted browser contexts; auth stays memory-only.
  }
}

function problemMessage(data: unknown, fallback: string) {
  if (data && typeof data === 'object' && 'detail' in data && typeof data.detail === 'string') {
    return data.detail
  }
  return fallback
}

async function readJson(response: Response) {
  const raw = await response.text()
  if (!raw) return {} as Record<string, unknown>
  try {
    return JSON.parse(raw) as Record<string, unknown>
  } catch {
    return { detail: raw }
  }
}

async function authPost(path: string, body?: unknown) {
  const response = await fetch(path, {
    method: 'POST',
    credentials: 'include',
    headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const data = await readJson(response)
  if (!response.ok) {
    throw new Error(problemMessage(data, 'Request failed. Please try again.'))
  }
  return data
}

export async function signIn(email: string, password: string) {
  const data = await authPost('/api/v1/auth/login', { email, password }) as unknown as AuthPayload
  accessToken = data.accessToken
  accessTokenExpiresAt = Date.now() + data.expiresIn * 1000
  return data
}

export async function getProfile() {
  const response = await apiFetch('/api/v1/auth/me')
  return parseResponse<UserProfile>(response)
}

export async function signOut() {
  try {
    await authPost('/api/v1/auth/logout')
  } finally {
    accessToken = null
  }
}

export async function listWorkspaces() {
  return parseResponse<Workspace[]>(await apiFetch('/api/v1/workspaces'))
}

export async function createWorkspace(name: string, description: string) {
  const response = await apiFetch('/api/v1/workspaces', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description }),
  })
  return parseResponse<Workspace>(response)
}

export async function listWorkspaceMembers(workspaceId: number) {
  return parseResponse<WorkspaceMember[]>(await apiFetch(`/api/v1/workspaces/${workspaceId}/members`))
}

export async function inviteWorkspaceMember(workspaceId: number, email: string, role: 'MEMBER' | 'VIEWER' = 'MEMBER') {
  const response = await apiFetch(`/api/v1/workspaces/${workspaceId}/invitations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, role }),
  })
  return parseResponse<WorkspaceInvitation>(response)
}

export async function acceptWorkspaceInvitation(token: string) {
  return parseResponse<{ workspaceId: number; workspaceName: string; role: string }>(await apiFetch(
    `/api/v1/invitations/${encodeURIComponent(token.trim())}/accept`, { method: 'POST' },
  ))
}
export async function leaveWorkspace(workspaceId: number) {
  const response = await apiFetch(`/api/v1/workspaces/${workspaceId}/members/me`, { method: 'DELETE' })
  if (!response.ok) await parseResponse<void>(response)
}
export async function acceptPendingWorkspaceInvitation(invitationId: number) {
  return parseResponse<{ workspaceId: number; workspaceName: string; role: string }>(await apiFetch(`/api/v1/my/invitations/${invitationId}/accept`, { method: 'POST' }))
}
export async function declineWorkspaceInvitation(invitationId: number) {
  return parseResponse<void>(await apiFetch(`/api/v1/my/invitations/${invitationId}/decline`, { method: 'POST' }))
}

export async function listBoards(workspaceId: number) {
  return parseResponse<BoardSummary[]>(await apiFetch(`/api/v1/workspaces/${workspaceId}/boards`))
}

export async function getBoard(boardId: number) {
  return parseResponse<Board>(await apiFetch(`/api/v1/boards/${boardId}`))
}

export async function createBoard(workspaceId: number, name: string) {
  return parseResponse<Board>(await apiFetch(`/api/v1/workspaces/${workspaceId}/boards`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  }))
}

export async function createCard(listId: number, input: { title: string; priority: CardPriority; dueDate?: string }) {
  return parseResponse<Card>(await apiFetch(`/api/v1/lists/${listId}/cards`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function moveCard(cardId: number, input: { listId: number; position: number; version: number }) {
  return parseResponse<Card>(await apiFetch(`/api/v1/cards/${cardId}/move`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function updateCard(cardId: number, input: { title: string; description: string | null; dueDate: string | null; priority: CardPriority; version: number }) {
  return parseResponse<Card>(await apiFetch(`/api/v1/cards/${cardId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}

export async function deleteCard(cardId: number) {
  const response = await apiFetch(`/api/v1/cards/${cardId}`, { method: 'DELETE' })
  if (!response.ok) await parseResponse<void>(response)
}

export async function setCardAssignees(cardId: number, input: { userIds: number[]; version: number }) {
  return parseResponse<Card>(await apiFetch(`/api/v1/cards/${cardId}/assignees`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }))
}
export async function setCardLabels(cardId: number, labels: string[]) {
  return parseResponse<Card>(await apiFetch(`/api/v1/cards/${cardId}/labels`, {
    method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ labels }),
  }))
}
export async function listBoardLabels(boardId: number) {
  return parseResponse<CardLabel[]>(await apiFetch(`/api/v1/boards/${boardId}/labels`))
}
export async function listWorkspaceLabels(workspaceId: number) {
  return parseResponse<CardLabel[]>(await apiFetch(`/api/v1/workspaces/${workspaceId}/labels`))
}
export async function searchCards(filters: { boardId?: number; workspaceId?: number; q?: string; assigneeId?: number; label?: string; dueFrom?: string; dueTo?: string; priority?: CardPriority; page?: number; size?: number }) {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(filters)) if (value !== undefined && value !== '') params.set(key, String(value))
  return parseResponse<PageResponse<SearchCard>>(await apiFetch(`/api/v1/search/cards?${params.toString()}`))
}

export async function listComments(cardId: number, page = 0) {
  return parseResponse<PageResponse<CommentItem>>(await apiFetch(`/api/v1/cards/${cardId}/comments?page=${page}&size=20`))
}
export async function createComment(cardId: number, content: string) {
  return parseResponse<CommentItem>(await apiFetch(`/api/v1/cards/${cardId}/comments`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ content }) }))
}
export async function updateComment(commentId: number, content: string) {
  return parseResponse<CommentItem>(await apiFetch(`/api/v1/comments/${commentId}`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ content }) }))
}
export async function deleteComment(commentId: number) {
  return parseResponse<void>(await apiFetch(`/api/v1/comments/${commentId}`, { method: 'DELETE' }))
}
export async function listCardActivity(cardId: number, page = 0) {
  return parseResponse<PageResponse<ActivityItem>>(await apiFetch(`/api/v1/cards/${cardId}/activities?page=${page}&size=20`))
}
export async function listBoardActivity(boardId: number, page = 0) {
  return parseResponse<PageResponse<ActivityItem>>(await apiFetch(`/api/v1/boards/${boardId}/activities?page=${page}&size=20`))
}
export async function listNotifications(page = 0) {
  return parseResponse<PageResponse<NotificationItem>>(await apiFetch(`/api/v1/notifications?page=${page}&size=20`))
}
export async function unreadNotificationCount() {
  return parseResponse<{ count: number }>(await apiFetch('/api/v1/notifications/unread-count'))
}
export async function markNotificationRead(id: number) {
  return parseResponse<NotificationItem>(await apiFetch(`/api/v1/notifications/${id}/read`, { method: 'PATCH' }))
}
export async function markAllNotificationsRead() {
  return parseResponse<void>(await apiFetch('/api/v1/notifications/read-all', { method: 'PATCH' }))
}

export async function getWebSocketAccessToken() {
  if (!accessToken || accessTokenExpiresAt < Date.now() + 30_000) await refreshAccessToken()
  return accessToken
}

async function parseResponse<T>(response: Response): Promise<T> {
  const data = await readJson(response)
  if (!response.ok) {
    throw new Error(problemMessage(data, `Request failed (${response.status}).`))
  }
  return data as T
}

export async function registerAccount(email: string, password: string, fullName: string) {
  return authPost('/api/v1/auth/register', { email, password, fullName })
}

function refreshAccessToken() {
  if (!refreshInFlight) {
    refreshInFlight = authPost('/api/v1/auth/refresh')
      .then(data => {
        const session = data as unknown as AuthPayload
        accessToken = session.accessToken
        accessTokenExpiresAt = Date.now() + session.expiresIn * 1000
        return session
      })
      .finally(() => {
        refreshInFlight = null
      })
  }
  return refreshInFlight
}

export async function apiFetch(path: string, init: RequestInit = {}) {
  const send = () => {
    const headers = new Headers(init.headers)
    if (accessToken) {
      headers.set('Authorization', `Bearer ${accessToken}`)
    }
    return fetch(path, { ...init, headers, credentials: 'include' })
  }

  let response = await send()
  const noRefresh = ['/api/v1/auth/login', '/api/v1/auth/register', '/api/v1/auth/refresh', '/api/v1/auth/logout']
  if (response.status !== 401 || noRefresh.includes(path)) return response

  try {
    await refreshAccessToken()
    response = await send()
  } catch {
    accessToken = null
  }
  return response
}
