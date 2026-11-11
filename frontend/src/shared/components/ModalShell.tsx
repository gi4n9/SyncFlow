import { X } from 'lucide-react'
import type { FormEventHandler, ReactNode } from 'react'

export function ModalShell({ kicker, title, onClose, onSubmit, children, className = '' }: { kicker: string; title: string; onClose: () => void; onSubmit: FormEventHandler<HTMLFormElement>; children: ReactNode; className?: string }) {
  return (
    <div
      className="modal-backdrop"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <form className={`modal ${className}`} onSubmit={onSubmit}>
        <div className="modal-heading">
          <div>
            <span className="modal-kicker">{kicker}</span>
            <h2>{title}</h2>
          </div>
          <button type="button" className="close-button" onClick={onClose} aria-label="Close">
            <X size={18} />
          </button>
        </div>
        {children}
      </form>
    </div>
  )
}
