package com.librarylane.catalog;

import java.util.List;

public record CatalogBookResult(
        String provider,
        String providerId,
        String title,
        String subtitle,
        List<String> authors,
        List<String> genres,
        String description,
        String publisher,
        String publicationDate,
        Integer pageCount,
        Integer audiobookLengthSeconds,
        List<String> narrators,
        String coverImageUrl,
        String language,
        String isbn10,
        String isbn13,
        String seriesName,
        Double seriesNumber,
        String editionFormat,
        String format
) {
}
