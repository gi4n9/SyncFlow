import { MoreHorizontal, Plus, Users } from 'lucide-react'
import type { AppUser, AppWorkspaceMember } from '../../../shared/types'

type Props = { title: string; description: string; isBoardLoaded: boolean; profile: AppUser | null; members: AppWorkspaceMember[]; canAddTask: boolean; onInvite: () => void; onAddTask: () => void; onCreateBoard: () => void }

export function ProjectHeader({ title, description, isBoardLoaded, profile, members, canAddTask, onInvite, onAddTask, onCreateBoard }: Props) {
  const status = isBoardLoaded ? 'ACTIVE BOARD' : profile ? 'WORKSPACE DATA' : 'PREVIEW BOARD'
  const source = isBoardLoaded ? 'SAVED TO DATABASE' : profile ? '' : 'SAMPLE DATA'

  return (
    <div className="project-heading">
      <div>
        <div className="eyebrow">
          <span className="eyebrow-dot" /> {status} <span className="eyebrow-sep">·</span> {source}
        </div>
        <h1>
          {title}
          <button aria-label="Create board" onClick={onCreateBoard}><MoreHorizontal size={22} /></button>
        </h1>
        <p className="project-description">{description}</p>
      </div>
      <div className="heading-buttons">
        <div className="avatar-stack">
          {members.length ? members.slice(0, 4).map((member, index) => (
            <span className={`avatar ${['peach', 'lilac', 'mint', 'sky'][index]}`} title={`${member.fullName} · ${member.role}`} key={member.userId}>
              {member.fullName.split(/\s+/).map(part => part[0]).slice(0, 2).join('').toUpperCase()}
            </span>
          )) : !profile && (
            <>
              <span className="avatar peach">AL</span>
              <span className="avatar lilac">MK</span>
              <span className="avatar mint">TN</span>
              <span className="avatar sky">JD</span>
            </>
          )}
          {members.length > 4 && <button className="avatar more-people">+{members.length - 4}</button>}
        </div>
        <button className="outline-button invite-button" onClick={onInvite}><Users size={15} /> Invite</button>
        <button className="primary-button" disabled={!canAddTask} onClick={onAddTask}><Plus size={17} /> Add task</button>
      </div>
    </div>
  )
}
