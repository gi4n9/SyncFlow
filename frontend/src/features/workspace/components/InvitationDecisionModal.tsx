import { UserRoundCheck, UserRoundX } from 'lucide-react'

export function InvitationDecisionModal({ open, workspaceName, onClose, onAccept, onDecline }: { open: boolean; workspaceName: string; onClose: () => void; onAccept: () => void; onDecline: () => void }) {
  if (!open) return null
  return <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}>
    <section className="invitation-decision-modal" role="dialog" aria-modal="true" aria-labelledby="invitation-decision-title">
      <small>WORKSPACE INVITATION · PENDING</small>
      <h2 id="invitation-decision-title">Join “{workspaceName}”?</h2>
      <p>You are not a member unless you accept. You can decline or leave this invitation pending.</p>
      <div className="modal-actions">
        <button className="outline-button" onClick={onClose}>Decide later</button>
        <button className="outline-button" onClick={onDecline}><UserRoundX size={15} /> Decline</button>
        <button className="primary-button" onClick={onAccept}><UserRoundCheck size={15} /> Accept invitation</button>
      </div>
    </section>
  </div>
}
