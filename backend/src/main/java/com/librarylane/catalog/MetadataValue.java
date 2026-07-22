package com.librarylane.catalog;

import java.util.List;

/**
 * Wraps one piece of metadata with information about where it
 * came from and whether the reader should confirm it.
 *
 * @param value             resolved metadata value
 * @param confidence        Library Lane's confidence in the value
 * @param sources           providers that contributed to the value
 * @param needsConfirmation whether the reader should confirm the value
 * @param <T>               type of value being stored
 */
public record MetadataValue<T>(
        T value,
        MetadataConfidence confidence,
        List<String> sources,
        boolean needsConfirmation
) {

    public MetadataValue {
        confidence = confidence == null
                ? MetadataConfidence.UNKNOWN
                : confidence;

        sources = sources == null
                ? List.of()
                : List.copyOf(sources);
    }

    public static <T> MetadataValue<T> unknown() {
        return new MetadataValue<>(
                null,
                MetadataConfidence.UNKNOWN,
                List.of(),
                true
        );
    }

    public static <T> MetadataValue<T> verified(
            T value,
            List<String> sources
    ) {
        return new MetadataValue<>(
                value,
                MetadataConfidence.VERIFIED,
                sources,
                false
        );
    }

    public static <T> MetadataValue<T> strong(
            T value,
            String source
    ) {
        return new MetadataValue<>(
                value,
                MetadataConfidence.STRONG,
                source == null || source.isBlank()
                        ? List.of()
                        : List.of(source),
                false
        );
    }

    public static <T> MetadataValue<T> partial(
            T value,
            List<String> sources
    ) {
        return new MetadataValue<>(
                value,
                MetadataConfidence.PARTIAL,
                sources,
                true
        );
    }
}