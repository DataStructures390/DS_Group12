package com.group12.tvsearch.model;

import java.util.Objects;

/** A permitted document for persistent indexing, carrying the caller's usage evidence. */
public record MetadataDocument(String id, long seriesId, String url, String title,
                               String body, ResultCategory category, String provenance,
                               String license) {
    public MetadataDocument {
        Objects.requireNonNull(category);
        Objects.requireNonNull(body);
        for (String field : new String[]{id, url, title, provenance, license}) {
            if (field == null || field.isBlank()) throw new IllegalArgumentException("Document fields/evidence required");
        }
        if (seriesId <= 0) throw new IllegalArgumentException("Positive series ID required");
    }
}
