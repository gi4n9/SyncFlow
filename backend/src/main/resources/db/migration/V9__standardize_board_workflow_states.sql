-- Keep custom lists in place after the standard workflow columns.
UPDATE board_lists SET position = position + 1000000000.0;

WITH mapped_lists AS (
    SELECT id, board_id, position,
           CASE LOWER(BTRIM(name))
               WHEN 'draft' THEN 'Draft'
               WHEN 'pending' THEN 'Pending'
               WHEN 'to do' THEN 'Pending'
               WHEN 'todo' THEN 'Pending'
               WHEN 'open' THEN 'Pending'
               WHEN 'in progress' THEN 'In Progress'
               WHEN 'doing' THEN 'In Progress'
               WHEN 'blocked' THEN 'Blocked'
               WHEN 'on hold' THEN 'Blocked'
               WHEN 'in review' THEN 'In Review'
               WHEN 'review' THEN 'In Review'
               WHEN 'waiting for approval' THEN 'In Review'
               WHEN 'reopen' THEN 'Reopen'
               WHEN 'revision required' THEN 'Reopen'
               WHEN 'done' THEN 'Done'
               WHEN 'completed' THEN 'Done'
               WHEN 'cancelled' THEN 'Cancelled'
               WHEN 'canceled' THEN 'Cancelled'
               WHEN 'rejected' THEN 'Cancelled'
               ELSE NULL
           END AS canonical_name
    FROM board_lists
), ranked_lists AS (
    SELECT id, board_id, position, canonical_name,
           ROW_NUMBER() OVER (
               PARTITION BY board_id, canonical_name
               ORDER BY position, id
           ) AS canonical_rank
    FROM mapped_lists
    WHERE canonical_name IS NOT NULL
)
UPDATE board_lists target
SET name = ranked_lists.canonical_name,
    position = CASE ranked_lists.canonical_name
        WHEN 'Draft' THEN 1000.0
        WHEN 'Pending' THEN 2000.0
        WHEN 'In Progress' THEN 3000.0
        WHEN 'Blocked' THEN 4000.0
        WHEN 'In Review' THEN 5000.0
        WHEN 'Reopen' THEN 6000.0
        WHEN 'Done' THEN 7000.0
        WHEN 'Cancelled' THEN 8000.0
    END
FROM ranked_lists
WHERE target.id = ranked_lists.id
  AND ranked_lists.canonical_rank = 1;

INSERT INTO board_lists (board_id, name, position)
SELECT boards.id, workflow.name, workflow.position
FROM boards
CROSS JOIN (VALUES
    ('Draft', 1000.0),
    ('Pending', 2000.0),
    ('In Progress', 3000.0),
    ('Blocked', 4000.0),
    ('In Review', 5000.0),
    ('Reopen', 6000.0),
    ('Done', 7000.0),
    ('Cancelled', 8000.0)
) AS workflow(name, position)
WHERE NOT EXISTS (
    SELECT 1
    FROM board_lists existing
    WHERE existing.board_id = boards.id
      AND existing.name = workflow.name
);
