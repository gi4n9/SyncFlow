import { KeyRound, UsersRound } from 'lucide-react'
import type { FormEventHandler } from 'react'
import { ModalShell } from '../../../shared/components/ModalShell'

type Props = {
  open: boolean
  token: string
  error: string
  onToken: (value: string) => void
  onClose: () => void
  onSubmit: FormEventHandler<HTMLFormElement>
}

export function AcceptInvitationModal({ open, token, error, onToken, onClose, onSubmit }: Props) {
  if (!open) return null
  return (
    <ModalShell kicker="WORKSPACE INVITATION" title="Join a workspace" onClose={onClose} onSubmit={onSubmit}>
      <p className="invitation-help"><UsersRound size={15} /> Review this invitation. You will join only after you press “Join workspace”. Sign in with the invited email.</p>
      <label className="field-label" htmlFor="invitation-token">Invitation link</label>
      <input
        id="invitation-token"
        className="text-field"
        value={token}
        onChange={event => onToken(event.target.value)}
        placeholder="Paste the invitation link shared by the workspace Owner"
        required
      />
      {error && <div className="auth-error"><KeyRound size={15} />{error}</div>}
      <div className="modal-actions">
        <button type="button" className="outline-button" onClick={onClose}>Cancel</button>
        <button className="primary-button" type="submit"><UsersRound size={15} /> Join workspace</button>
      </div>
    </ModalShell>
  )
}
