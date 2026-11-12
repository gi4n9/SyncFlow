import { Check, Copy, Link2, LockKeyhole, Send } from 'lucide-react'
import type { FormEventHandler } from 'react'
import { ModalShell } from '../../../shared/components/ModalShell'

type Props = {
  open: boolean
  email: string
  role: 'MEMBER' | 'VIEWER'
  invitationLink: string
  copied: boolean
  error: string
  onEmail: (value: string) => void
  onRole: (value: 'MEMBER' | 'VIEWER') => void
  onClose: () => void
  onSubmit: FormEventHandler<HTMLFormElement>
  onCopy: () => void
}

export function InviteMemberModal(props: Props) {
  const { open, email, role, invitationLink, copied, error, onEmail, onRole, onClose, onSubmit, onCopy } = props
  if (!open) return null

  return (
    <ModalShell
      kicker="WORKSPACE INVITATION"
      title={invitationLink ? 'Invitation link ready' : 'Invite a teammate'}
      onClose={onClose}
      onSubmit={onSubmit}
    >
      {invitationLink ? (
        <>
          <p className="invitation-help"><Link2 size={15} /> Share this link with {email}. It expires in 7 days and can be used once.</p>
          <label className="field-label" htmlFor="invitation-link">Invitation link</label>
          <input id="invitation-link" className="text-field" value={invitationLink} readOnly onFocus={event => event.currentTarget.select()} />
          {error && <div className="auth-error"><LockKeyhole size={15} />{error}</div>}
          <div className="modal-actions">
            <button type="button" className="outline-button" onClick={onClose}>Close</button>
            <button type="button" className="primary-button" onClick={onCopy}>{copied ? <Check size={15} /> : <Copy size={15} />}{copied ? 'Copied' : 'Copy link'}</button>
          </div>
        </>
      ) : (
        <>
          <p className="invitation-help"><Send size={15} /> Create a one-use link and share it with the invited account.</p>
          <label className="field-label" htmlFor="invite-email">Email address</label>
          <input id="invite-email" className="text-field" type="email" value={email} onChange={event => onEmail(event.target.value)} placeholder="teammate@company.com" autoFocus required />
          <label className="field-label" htmlFor="invite-role">Workspace role</label>
          <select id="invite-role" className="text-field" value={role} onChange={event => onRole(event.target.value as 'MEMBER' | 'VIEWER')}>
            <option value="MEMBER">Member — can edit boards and tasks</option>
            <option value="VIEWER">Viewer — read only</option>
          </select>
          {error && <div className="auth-error"><LockKeyhole size={15} />{error}</div>}
          <div className="modal-actions">
            <button type="button" className="outline-button" onClick={onClose}>Cancel</button>
            <button className="primary-button" type="submit"><Send size={15} /> Create invitation link</button>
          </div>
        </>
      )}
    </ModalShell>
  )
}
