-- Preserve the existing newest-first shelf as each library's initial personal
-- arrangement. Readers can subsequently reorder these values through the
-- Books page without changing the underlying book records.
ALTER TABLE public.books
    ADD COLUMN shelf_position integer;

WITH ranked_books AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY library_id
            ORDER BY date_added DESC NULLS LAST,
                     created_at DESC NULLS LAST,
                     id DESC
        ) - 1 AS initial_position
    FROM public.books
)
UPDATE public.books AS book
SET shelf_position = ranked_books.initial_position
FROM ranked_books
WHERE book.id = ranked_books.id;

