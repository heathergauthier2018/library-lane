package com.librarylane.catalog;

import java.util.List;

/**
 * Represents a catalog book after Library Lane has compared and
 * combined compatible metadata from its available sources.
 */
public record ResolvedCatalogBook(
        CatalogBookResult book,
        MetadataValue<String> title,
        MetadataValue<List<String>> authors,
        MetadataValue<List<String>> genres,
        MetadataValue<String> description,
        MetadataValue<String> publisher,
        MetadataValue<String> publicationDate,
        MetadataValue<Integer> pageCount,
        MetadataValue<Integer> audiobookLengthSeconds,
        MetadataValue<List<String>> narrators,
        MetadataValue<String> coverImageUrl,
        MetadataValue<String> seriesName,
        MetadataValue<Double> seriesNumber,
        MetadataValue<Boolean> isSeries,
        List<String> warnings
) {

    public ResolvedCatalogBook {
        warnings = warnings == null
                ? List.of()
                : List.copyOf(warnings);
    }
}