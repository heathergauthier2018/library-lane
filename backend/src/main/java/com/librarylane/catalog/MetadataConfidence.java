package com.librarylane.catalog;

/**
 * Describes how confident Library Lane is about an individual
 * piece of book metadata.
 */
public enum MetadataConfidence {
    VERIFIED,
    STRONG,
    PARTIAL,
    UNKNOWN
}