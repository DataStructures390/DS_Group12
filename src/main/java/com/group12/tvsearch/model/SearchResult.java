package com.group12.tvsearch.model;

import java.util.Objects;

/** Source scores are placeholders until permitted providers and features are integrated. */
public record SearchResult(
        String url, String title, String snippet, String source, ResultCategory category,
        double keywordRelevance, double seriesRelevance,
        double sourceRelevance, double freshness) {
    public SearchResult {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(category, "category");
    }
}
