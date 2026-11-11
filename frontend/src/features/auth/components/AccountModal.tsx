import { ArrowRight, Command, LockKeyhole, X } from 'lucide-react'
import type { FormEventHandler } from 'react'

type Props = {
  open: boolean
  mode: 'login' | 'register'
  email: string
  password: string
  fullName: string
  error: string
  onEmail: (value: string) => void
  onPassword: (value: string) => void
  onFullName: (value: string) => void
  onMode: (mode: 'login' | 'register') => void
  onClose: () => void
  onSubmit: FormEventHandler<HTMLFormElement>
}

export function AccountModal(props: Props) {
  if (!props.open) return null
  const { mode, email, password, fullName, error, onEmail, onPassword, onFullName, onMode, onClose, onSubmit } = props
  return (
    <div className="modal-backdrop" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}>
      <form className="modal auth-modal" onSubmit={onSubmit}>
        <div className="auth-brand">
          <span className="brand-mark"><Command size={18} /></span>
          syncflow<span className="brand-period">.</span>
        </div>
        <button type="button" className="close-button auth-close" onClick={onClose} aria-label="Close"><X size={18} /></button>
        <h2>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h2>
        <p className="auth-subtitle">
          {mode === 'login' ? 'Sign in to pick up where your team left off.' : 'Start organizing your team’s work today.'}
        </p>

        {mode === 'register' && (
          <>
            <label className="field-label" htmlFor="full-name">Full name</label>
            <input className="text-field" id="full-name" autoComplete="name" value={fullName} onChange={event => onFullName(event.target.value)} placeholder="Alex Lee" required />
          </>
        )}
        <label className="field-label" htmlFor="auth-email">Email address</label>
        <input className="text-field" id="auth-email" type="email" autoComplete="email" value={email} onChange={event => onEmail(event.target.value)} placeholder="you@company.com" required />
        <label className="field-label" htmlFor="auth-password">Password</label>
        <input className="text-field" id="auth-password" type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} value={password} onChange={event => onPassword(event.target.value)} placeholder="At least 8 characters" required />

        {error && <div className="auth-error"><LockKeyhole size={15} />{error}</div>}
        <button className="primary-button auth-submit" type="submit">
          {mode === 'login' ? 'Sign in' : 'Create account'} <ArrowRight size={16} />
        </button>
        <div className="auth-switch">
          {mode === 'login' ? "Don't have an account?" : 'Already have an account?'}
          {' '}<button type="button" onClick={() => onMode(mode === 'login' ? 'register' : 'login')}>
            {mode === 'login' ? 'Sign up' : 'Sign in'}
          </button>
        </div>
      </form>
    </div>
  )
}
