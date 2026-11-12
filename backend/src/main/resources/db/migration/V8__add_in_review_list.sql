INSERT INTO board_lists (board_id, name, position)
SELECT boards.id,
       'In review',
       COALESCE(
           (
               SELECT (COALESCE(MAX(previous_list.position), done_list.position - 1000.0)
                       + done_list.position) / 2.0
               FROM board_lists done_list
               LEFT JOIN board_lists previous_list
                   ON previous_list.board_id = done_list.board_id
                  AND previous_list.position < done_list.position
               WHERE done_list.board_id = boards.id
                 AND LOWER(done_list.name) = 'done'
               GROUP BY done_list.id, done_list.position
               ORDER BY done_list.position
               LIMIT 1
           ),
           (SELECT COALESCE(MAX(existing_list.position), 0.0) + 1000.0
            FROM board_lists existing_list
            WHERE existing_list.board_id = boards.id)
       )
FROM boards
WHERE NOT EXISTS (
    SELECT 1
    FROM board_lists existing_list
    WHERE existing_list.board_id = boards.id
      AND LOWER(existing_list.name) IN ('in review', 'in reviewing')
);
