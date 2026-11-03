package com.group12.tvsearch.model;

import java.util.List;
import java.util.Objects;

/** Self-authored fictional documents; never external search results or verified metadata. */
public record LocalDocument(String id, long seriesId, String seriesTitle,
                            List<String> aliases, String title, String body,
                            ResultCategory category) {
    public LocalDocument {
        Objects.requireNonNull(id);
        Objects.requireNonNull(seriesTitle);
        aliases = List.copyOf(aliases);
        Objects.requireNonNull(title);
        Objects.requireNonNull(body);
        Objects.requireNonNull(category);
        if (!id.matches("[a-z0-9-]+")) throw new IllegalArgumentException("Invalid local document ID");
    }

    public String url() { return "/demo/documents/" + id; }
    public String identityText() { return seriesTitle + " " + String.join(" ", aliases); }
    public String indexedText() { return identityText() + " " + title + " " + body; }
}
