ALTER TABLE comments ADD COLUMN edited_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_comments_card_created_id ON comments(card_id, created_at DESC, id DESC);

ALTER TABLE notifications ADD COLUMN type VARCHAR(32) NOT NULL DEFAULT 'SYSTEM';
ALTER TABLE notifications ADD COLUMN payload JSONB NOT NULL DEFAULT '{}'::JSONB;
CREATE INDEX idx_notifications_user_unread_created_id
    ON notifications(user_id, is_read, created_at DESC, id DESC);

CREATE TABLE activity_logs (
    id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    board_id BIGINT REFERENCES boards(id) ON DELETE CASCADE,
    card_id BIGINT REFERENCES cards(id) ON DELETE SET NULL,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(48) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_logs_board_created_id ON activity_logs(board_id, created_at DESC, id DESC);
CREATE INDEX idx_activity_logs_card_created_id ON activity_logs(card_id, created_at DESC, id DESC);
CREATE INDEX idx_activity_logs_workspace_created_id ON activity_logs(workspace_id, created_at DESC, id DESC);
