import { LogOut, ShieldAlert } from 'lucide-react'

export function LeaveWorkspaceModal({ open, workspaceName, isOwner, busy = false, onClose, onConfirm }: { open: boolean; workspaceName: string; isOwner: boolean; busy?: boolean; onClose: () => void; onConfirm: () => void }) {
  if (!open) return null
  return <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}>
    <section className="invitation-decision-modal" role="dialog" aria-modal="true" aria-labelledby="leave-workspace-title">
      <small>WORKSPACE MEMBERSHIP</small>
      <h2 id="leave-workspace-title">Leave “{workspaceName}”?</h2>
      {isOwner ? <p className="leave-owner-notice"><ShieldAlert size={16} /> You are the Owner. Transfer ownership to another member before leaving this workspace.</p>
        : <p>You will lose access to this workspace and its boards. An Owner can invite you again later.</p>}
      <div className="modal-actions">
        <button className="outline-button" onClick={onClose}>Cancel</button>
        {!isOwner && <button className="primary-button danger-button" disabled={busy} onClick={onConfirm}><LogOut size={15} /> {busy ? 'Leaving…' : 'Leave workspace'}</button>}
      </div>
    </section>
  </div>
}
