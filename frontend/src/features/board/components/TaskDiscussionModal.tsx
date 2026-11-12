import { useEffect, useState, type FormEvent } from 'react'
import { Activity, MessageSquare, Pencil, Send, Trash2, X } from 'lucide-react'
import { createComment, deleteComment, listBoardActivity, listCardActivity, listComments, updateComment } from '../../../api/client'
import type { ActivityItem, CommentItem, PageResponse } from '../../../api/client'
import type { WorkspaceMember } from '../../../api/client'

type Props = { cardId?: number; boardId?: number; taskTitle?: string; currentUserId: number; isOwner: boolean; members: WorkspaceMember[]; onClose: () => void; onNotice: (message: string) => void }
export function TaskDiscussionModal({ cardId, boardId, taskTitle = 'Board activity', currentUserId, isOwner, members, onClose, onNotice }: Props) {
  const [comments, setComments] = useState<CommentItem[]>([])
  const [activities, setActivities] = useState<ActivityItem[]>([])
  const [commentPage, setCommentPage] = useState(0)
  const [activityPage, setActivityPage] = useState(0)
  const [commentPages, setCommentPages] = useState<PageResponse<CommentItem> | null>(null)
  const [activityPages, setActivityPages] = useState<PageResponse<ActivityItem> | null>(null)
  const [content, setContent] = useState('')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editingContent, setEditingContent] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let active = true
    const commentsRequest = cardId ? listComments(cardId, commentPage) : Promise.resolve(null)
    const activityRequest = cardId ? listCardActivity(cardId, activityPage) : boardId ? listBoardActivity(boardId, activityPage) : Promise.resolve(null)
    Promise.all([commentsRequest, activityRequest]).then(([commentResult, activityResult]) => {
      if (!active) return
      setCommentPages(commentResult)
      setComments(commentResult?.content ?? [])
      setActivityPages(activityResult)
      setActivities(activityResult?.content ?? [])
    }).catch(reason => { if (active) setError(reason instanceof Error ? reason.message : 'Could not load discussion.') })
    return () => { active = false }
  }, [cardId, boardId, commentPage, activityPage])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!cardId || !content.trim()) return
    setBusy(true); setError('')
    try {
      await createComment(cardId, content.trim())
      setContent('')
      setCommentPage(0)
      const result = await listComments(cardId, 0)
      setCommentPages(result)
      setComments(result.content)
      onNotice('Comment added.')
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Could not add comment.') }
    finally { setBusy(false) }
  }
  async function saveEdit(id: number) {
    try {
      await updateComment(id, editingContent.trim())
      const result = await listComments(cardId!, commentPage)
      setComments(result.content); setCommentPages(result); setEditingId(null)
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Could not update comment.') }
  }
  async function removeComment(id: number) {
    try {
      await deleteComment(id)
      const result = await listComments(cardId!, commentPage)
      setComments(result.content); setCommentPages(result)
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Could not delete comment.') }
  }

  return <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}>
    <section className="discussion-modal" role="dialog" aria-modal="true" aria-label={taskTitle}>
      <header className="discussion-header"><div><small>{cardId ? 'TASK DISCUSSION' : 'BOARD TIMELINE'}</small><h2>{taskTitle}</h2></div><button className="icon-button" onClick={onClose} aria-label="Close"><X size={18} /></button></header>
      {error && <p className="form-error">{error}</p>}
      {cardId && <section className="discussion-section"><h3><MessageSquare size={16} /> Comments <span>{commentPages?.totalElements ?? comments.length}</span></h3>
        <div className="discussion-items">{comments.map(comment => <article className="comment-item" key={comment.id}>
          <div className="comment-heading"><b>{comment.authorName}</b><time>{new Date(comment.createdAt).toLocaleString()}{comment.editedAt ? ' · edited' : ''}</time>
            {(comment.authorId === currentUserId || isOwner) && <div className="comment-actions">{comment.authorId === currentUserId && <button aria-label="Edit comment" onClick={() => { setEditingId(comment.id); setEditingContent(comment.content) }}><Pencil size={13} /></button>}<button aria-label="Delete comment" onClick={() => void removeComment(comment.id)}><Trash2 size={13} /></button></div>}
          </div>
          {editingId === comment.id ? <div className="comment-edit"><textarea value={editingContent} onChange={event => setEditingContent(event.target.value)} /><button onClick={() => void saveEdit(comment.id)}>Save</button><button onClick={() => setEditingId(null)}>Cancel</button></div> : <p>{comment.content}</p>}
        </article>)}{comments.length === 0 && <p className="discussion-empty">No comments yet.</p>}</div>
        <div className="discussion-pagination"><button disabled={!commentPages || commentPage === 0} onClick={() => setCommentPage(page => page - 1)}>Previous</button><span>Page {commentPage + 1}</span><button disabled={!commentPages?.hasNext} onClick={() => setCommentPage(page => page + 1)}>Next</button></div>
        <form className="comment-form" onSubmit={submit}><textarea value={content} onChange={event => setContent(event.target.value)} placeholder="Write a comment… Use @email to mention a workspace member" maxLength={10000} /><div><small>{members.length} workspace members · mention using @email</small><button className="primary-button" disabled={busy || !content.trim()}><Send size={14} /> Send</button></div></form>
      </section>}
      <section className="discussion-section activity-section"><h3><Activity size={16} /> Activity <span>{activityPages?.totalElements ?? activities.length}</span></h3>
        <div className="activity-items">{activities.map(item => <article className="activity-item" key={item.id}><i /><div><b>{item.actorName ?? 'Someone'}</b> {item.action.toLowerCase().replaceAll('_', ' ')}<time>{new Date(item.createdAt).toLocaleString()}</time></div></article>)}{activities.length === 0 && <p className="discussion-empty">No activity recorded yet.</p>}</div>
        <div className="discussion-pagination"><button disabled={!activityPages || activityPage === 0} onClick={() => setActivityPage(page => page - 1)}>Previous</button><span>Page {activityPage + 1}</span><button disabled={!activityPages?.hasNext} onClick={() => setActivityPage(page => page + 1)}>Next</button></div>
      </section>
    </section>
  </div>
}
