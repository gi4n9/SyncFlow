ALTER TABLE boards ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE board_lists (
    id BIGSERIAL PRIMARY KEY,
    board_id BIGINT NOT NULL REFERENCES boards(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    position DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

WITH existing_statuses AS (
    SELECT DISTINCT board_id, COALESCE(NULLIF(column_status, ''), 'TODO') AS list_name
    FROM cards
), ordered_statuses AS (
    SELECT board_id, list_name,
           ROW_NUMBER() OVER (PARTITION BY board_id ORDER BY list_name) * 1000.0 AS list_position
    FROM existing_statuses
)
INSERT INTO board_lists(board_id, name, position)
SELECT board_id, list_name, list_position FROM ordered_statuses;

INSERT INTO board_lists(board_id, name, position)
SELECT boards.id, defaults.name, defaults.position
FROM boards
CROSS JOIN (VALUES ('To do', 1000.0), ('In progress', 2000.0), ('Done', 3000.0)) AS defaults(name, position)
WHERE NOT EXISTS (SELECT 1 FROM board_lists WHERE board_lists.board_id = boards.id);

ALTER TABLE cards ADD COLUMN list_id BIGINT;
UPDATE cards
SET list_id = board_lists.id
FROM board_lists
WHERE board_lists.board_id = cards.board_id
  AND board_lists.name = COALESCE(NULLIF(cards.column_status, ''), 'TODO');

ALTER TABLE cards ALTER COLUMN list_id SET NOT NULL;
ALTER TABLE cards ALTER COLUMN position TYPE DOUBLE PRECISION USING position::DOUBLE PRECISION;
ALTER TABLE cards ADD CONSTRAINT fk_cards_board_list
    FOREIGN KEY (list_id) REFERENCES board_lists(id) ON DELETE CASCADE;

CREATE INDEX idx_board_lists_board_position ON board_lists(board_id, position);
CREATE INDEX idx_cards_list_position ON cards(list_id, position);
