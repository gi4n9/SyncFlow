-- Cards now belong to a board through board_lists; assignees are stored in card_assignees.
-- These columns were part of the pre-list card schema and are no longer written by the JPA model.
ALTER TABLE cards
    DROP COLUMN board_id,
    DROP COLUMN column_status,
    DROP COLUMN assigned_to;
