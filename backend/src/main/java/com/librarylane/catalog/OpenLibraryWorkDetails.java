package com.librarylane.catalog;

import java.util.List;

/**
 * Richer metadata retrieved from an individual Open Library work.
 *
 * A work represents the underlying story rather than one particular
 * physical, electronic, or audio edition.
 */
public record OpenLibraryWorkDetails(
        String workId,
        String title,
        String subtitle,
        String description,
        List<String> subjects,
        List<String> seriesNames,
        List<String> authorKeys,
        String firstPublishDate
) {

    public OpenLibraryWorkDetails {
        workId = cleanText(workId);
        title = cleanText(title);
        subtitle = cleanText(subtitle);
        description = cleanText(description);
        firstPublishDate = cleanText(firstPublishDate);

        subjects = immutableList(subjects);
        seriesNames = immutableList(seriesNames);
        authorKeys = immutableList(authorKeys);
    }

    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    public boolean hasSeriesInformation() {
        return !seriesNames.isEmpty();
    }

    public boolean hasSubjects() {
        return !subjects.isEmpty();
    }

    private static String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? null : cleaned;
    }

    private static List<String> immutableList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}