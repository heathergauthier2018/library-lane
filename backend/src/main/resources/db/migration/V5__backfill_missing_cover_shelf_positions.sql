WITH missing_books AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS sequence_number
    FROM books
    WHERE cover_shelf_position IS NULL
),
available_slots AS (
    SELECT slot, ROW_NUMBER() OVER (ORDER BY slot) AS sequence_number
    FROM generate_series(1, 100) AS slot
    WHERE NOT EXISTS (
        SELECT 1
        FROM books
        WHERE cover_shelf_position = slot
    )
),
assignments AS (
    SELECT missing_books.id, available_slots.slot
    FROM missing_books
    JOIN available_slots USING (sequence_number)
)
UPDATE books
SET cover_shelf_position = assignments.slot
FROM assignments
WHERE books.id = assignments.id;
