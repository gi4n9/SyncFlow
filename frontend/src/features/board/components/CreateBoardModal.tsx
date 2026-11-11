import { LockKeyhole, Plus } from 'lucide-react'
import type { FormEventHandler } from 'react'
import { ModalShell } from '../../../shared/components/ModalShell'

type Props = { open: boolean; workspaceName?: string; name: string; error: string; onName: (value: string) => void; onClose: () => void; onSubmit: FormEventHandler<HTMLFormElement> }

export function CreateBoardModal(props: Props) {
  if (!props.open) return null
  const { workspaceName, name, error, onName, onClose, onSubmit } = props
  return (
    <ModalShell kicker={workspaceName ?? 'WORKSPACE'} title="Create a board" onClose={onClose} onSubmit={onSubmit}>
      <label className="field-label" htmlFor="board-name">Board name</label>
      <input
        autoFocus
        id="board-name"
        className="text-field"
        placeholder="e.g. Product design"
        value={name}
        onChange={event => onName(event.target.value)}
        required
        maxLength={255}
      />
      {error && <div className="auth-error"><LockKeyhole size={15} />{error}</div>}
      <div className="modal-actions">
        <button type="button" className="outline-button" onClick={onClose}>Cancel</button>
        <button className="primary-button" type="submit"><Plus size={16} /> Create board</button>
      </div>
    </ModalShell>
  )
}
