package com.librarylane.catalog;

/**
 * Represents one search request made to an external catalog provider.
 *
 * Format currently uses:
 * PHYSICAL, EBOOK, AUDIOBOOK, or null for every format.
 *
 * SearchBy currently uses:
 * TITLE, AUTHOR, or SERIES.
 */
public record CatalogSearchRequest(
        String query,
        String format,
        String searchBy,
        int limit
) {

    public CatalogSearchRequest {
        query = query == null
                ? ""
                : query.trim().replaceAll("\\s+", " ");

        format = normalizeFormat(format);
        searchBy = normalizeSearchField(searchBy);

        if (limit <= 0) {
            limit = 40;
        }

        if (limit > 100) {
            limit = 100;
        }
    }

    public CatalogSearchRequest(
            String query,
            String format,
            String searchBy
    ) {
        this(query, format, searchBy, 40);
    }

    private static String normalizeFormat(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase();

        return switch (normalized) {
            case "PHYSICAL", "EBOOK", "AUDIOBOOK" -> normalized;
            default -> null;
        };
    }

    private static String normalizeSearchField(String value) {
        if (value == null || value.isBlank()) {
            return "TITLE";
        }

        String normalized = value.trim().toUpperCase();

        return switch (normalized) {
            case "AUTHOR", "SERIES" -> normalized;
            default -> "TITLE";
        };
    }
}