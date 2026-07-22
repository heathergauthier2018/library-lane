package com.librarylane.services;

import com.librarylane.catalog.CatalogBookResult;
import com.librarylane.catalog.CatalogSearchRequest;

import java.util.List;
import java.util.Optional;

/**
 * Common contract for every external catalog used by Library Lane.
 *
 * Open Library, Apple Audiobooks, and any future provider will
 * eventually implement this interface.
 */
public interface CatalogProvider {

    String providerName();

    List<CatalogBookResult> search(CatalogSearchRequest request);

    default Optional<CatalogBookResult> getDetails(
            String providerId,
            String format
    ) {
        return Optional.empty();
    }
}