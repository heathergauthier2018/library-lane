ALTER TABLE books
    ADD COLUMN IF NOT EXISTS cover_shelf_position INTEGER;

UPDATE books
SET cover_shelf_position = shelf_position
WHERE cover_shelf_position IS NULL
  AND shelf_position BETWEEN 1 AND 100;

ALTER TABLE books
    DROP CONSTRAINT IF EXISTS chk_books_cover_shelf_position;

ALTER TABLE books
    ADD CONSTRAINT chk_books_cover_shelf_position
        CHECK (cover_shelf_position IS NULL OR cover_shelf_position BETWEEN 1 AND 100);

CREATE UNIQUE INDEX IF NOT EXISTS ux_books_cover_shelf_position
    ON books (cover_shelf_position)
    WHERE cover_shelf_position IS NOT NULL;
