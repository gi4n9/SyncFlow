import { LockKeyhole, Plus } from 'lucide-react'
import type { FormEventHandler } from 'react'
import { ModalShell } from '../../../shared/components/ModalShell'

type Props = { open: boolean; name: string; description: string; error: string; onName: (value: string) => void; onDescription: (value: string) => void; onClose: () => void; onSubmit: FormEventHandler<HTMLFormElement> }

export function CreateWorkspaceModal(props: Props) {
  if (!props.open) return null
  const { name, description, error, onName, onDescription, onClose, onSubmit } = props
  return (
    <ModalShell kicker="YOUR TEAM" title="Create a workspace" onClose={onClose} onSubmit={onSubmit}>
      <label className="field-label" htmlFor="workspace-name">Workspace name</label>
      <input
        autoFocus
        id="workspace-name"
        className="text-field"
        placeholder="e.g. Studio North"
        value={name}
        onChange={event => onName(event.target.value)}
        required
        maxLength={255}
      />
      <label className="field-label" htmlFor="workspace-description">Description</label>
      <input
        id="workspace-description"
        className="text-field"
        placeholder="What is this workspace for?"
        value={description}
        onChange={event => onDescription(event.target.value)}
        maxLength={2000}
      />
      {error && <div className="auth-error"><LockKeyhole size={15} />{error}</div>}
      <div className="modal-actions">
        <button type="button" className="outline-button" onClick={onClose}>Cancel</button>
        <button className="primary-button" type="submit"><Plus size={16} /> Create workspace</button>
      </div>
    </ModalShell>
  )
}
