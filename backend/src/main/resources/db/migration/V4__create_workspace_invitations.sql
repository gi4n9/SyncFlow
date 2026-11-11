CREATE INDEX idx_workspace_members_user_id ON workspace_members(user_id);

CREATE TABLE workspace_invitations (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    invited_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('MEMBER', 'VIEWER')),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_pending_workspace_invitation
    ON workspace_invitations(workspace_id, email) WHERE status = 'PENDING';
CREATE INDEX idx_workspace_invitations_expiry ON workspace_invitations(expires_at);
