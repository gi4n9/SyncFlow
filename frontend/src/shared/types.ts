import type { Card, UserProfile, Workspace, WorkspaceMember, Board, BoardSummary, CardPriority } from '../api/client'

export type Task = {
  id: number
  title: string
  project: string
  priority: 'High' | 'Medium' | 'Low'
  labels: string[]
  due: string
  assignees: string[]
  comments?: number
  checklist?: string
  version?: number
}
export type Column = {
  id: string
  title: string
  tone: string
  tasks: Task[]
  listId?: number
}
export type AppUser = UserProfile
export type AppWorkspace = Workspace
export type AppWorkspaceMember = WorkspaceMember
export type AppBoard = Board
export type AppBoardSummary = BoardSummary
export type Priority = CardPriority

export const previewColumns: Column[] = [
  { id: 'todo', title: 'To do', tone: 'slate', tasks: [
    {
      id: 1,
      title: 'Map out onboarding flow',
      project: 'Product design',
      priority: 'High',
      labels: ['UX research', 'Sprint 08'],
      due: 'Nov 14',
      assignees: ['AL', 'MK'],
      comments: 4,
      checklist: '2/5',
    },
    {
      id: 2,
      title: 'Write API integration guide',
      project: 'Documentation',
      priority: 'Medium',
      labels: ['Content'],
      due: 'Nov 16',
      assignees: ['TN'],
      comments: 2,
    },
    {
      id: 3,
      title: 'Review feedback from beta users',
      project: 'Product design',
      priority: 'Low',
      labels: ['Research'],
      due: 'Nov 18',
      assignees: ['MK', 'JD'],
    },
  ] },
  { id: 'progress', title: 'In progress', tone: 'blue', tasks: [
    {
      id: 4,
      title: 'Redesign workspace dashboard',
      project: 'Product design',
      priority: 'High',
      labels: ['UI design', 'Sprint 08'],
      due: 'Nov 12',
      assignees: ['AL', 'JD'],
      comments: 8,
      checklist: '4/6',
    },
    {
      id: 5,
      title: 'Set up real-time notifications',
      project: 'Engineering',
      priority: 'Medium',
      labels: ['Backend'],
      due: 'Nov 13',
      assignees: ['TN', 'MK'],
      comments: 3,
    },
  ] },
  { id: 'review', title: 'In review', tone: 'violet', tasks: [
    {
      id: 6,
      title: 'Create component library',
      project: 'Product design',
      priority: 'Medium',
      labels: ['UI design'],
      due: 'Nov 11',
      assignees: ['JD'],
      comments: 6,
      checklist: '8/8',
    },
    {
      id: 7,
      title: 'Optimize database queries',
      project: 'Engineering',
      priority: 'Low',
      labels: ['Performance'],
      due: 'Nov 12',
      assignees: ['TN', 'AL'],
    },
  ] },
  { id: 'done', title: 'Done', tone: 'green', tasks: [
    {
      id: 8,
      title: 'Define project milestones',
      project: 'Planning',
      priority: 'Low',
      labels: ['Sprint 08'],
      due: 'Nov 08',
      assignees: ['AL', 'MK'],
      comments: 2,
    },
  ] },
]

export const people: Record<string, { name: string; color: string }> = {
  AL: { name: 'Alex Lee', color: 'peach' },
  MK: { name: 'Mia Kim', color: 'lilac' },
  TN: { name: 'Theo Nguyen', color: 'mint' },
  JD: { name: 'Jamie Diaz', color: 'sky' },
}
export const listTones = ['slate', 'blue', 'violet', 'green', 'orange']

export function cardToTask(card: Card, boardName: string): Task {
  const priority = `${card.priority[0]}${card.priority.slice(1).toLowerCase()}` as Task['priority']
  const due = card.dueDate ? new Date(card.dueDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) : 'No due date'
  const assignees = card.assignees.map(user => user.fullName.split(/\s+/).map(part => part[0]).slice(0, 2).join('').toUpperCase())
  return { id: card.id, title: card.title, project: boardName, priority, labels: [], due, assignees, version: card.version }
}
