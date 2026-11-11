ALTER TABLE cards ADD COLUMN priority VARCHAR(16) NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE cards ADD CONSTRAINT chk_cards_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'));
ALTER TABLE cards ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE cards ADD COLUMN created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;

CREATE TABLE card_assignees (
    card_id BIGINT NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, user_id)
);

INSERT INTO card_assignees(card_id, user_id)
SELECT id, assigned_to FROM cards WHERE assigned_to IS NOT NULL
ON CONFLICT DO NOTHING;

CREATE INDEX idx_card_assignees_user_id ON card_assignees(user_id);
