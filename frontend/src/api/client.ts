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

let accessToken: string | null = null
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
