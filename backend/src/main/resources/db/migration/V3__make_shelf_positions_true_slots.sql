-- V2 introduced zero-based compact ordering. Preserve every existing reader
-- arrangement while promoting that value to a physical, one-based shelf slot.
UPDATE books
SET shelf_position = shelf_position + 1
WHERE shelf_position BETWEEN 0 AND 206;

ALTER TABLE books
    ADD CONSTRAINT books_shelf_position_range
    CHECK (shelf_position IS NULL OR shelf_position BETWEEN 1 AND 207);

CREATE UNIQUE INDEX books_library_shelf_position_unique
    ON books (library_id, shelf_position)
    WHERE shelf_position IS NOT NULL;
