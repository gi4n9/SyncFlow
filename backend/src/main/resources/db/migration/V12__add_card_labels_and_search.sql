CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE labels (
    id BIGSERIAL PRIMARY KEY,
    board_id BIGINT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    name VARCHAR(64) NOT NULL,
    color VARCHAR(24) NOT NULL DEFAULT 'violet',
    CONSTRAINT uk_labels_board_name UNIQUE (board_id, name)
);

CREATE TABLE card_labels (
    card_id BIGINT NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    label_id BIGINT NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    PRIMARY KEY (card_id, label_id)
);
CREATE INDEX idx_card_labels_label_card ON card_labels(label_id, card_id);
CREATE INDEX idx_labels_board_name ON labels(board_id, name);
CREATE INDEX idx_cards_title_trgm ON cards USING GIN (title gin_trgm_ops);
CREATE INDEX idx_cards_description_trgm ON cards USING GIN (description gin_trgm_ops);
