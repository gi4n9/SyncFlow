import { Check, X } from 'lucide-react'

export function Toast({ message, onDismiss }: { message: string; onDismiss: () => void }) {
  if (!message) return null
  return (
    <div className="toast">
      <span className="toast-check"><Check size={14} /></span>
      {message}
      <button aria-label="Dismiss" onClick={onDismiss}>
        <X size={15} />
      </button>
    </div>
  )
}
