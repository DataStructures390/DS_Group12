package com.group12.tvsearch.query;

import java.util.List;

/** Planned verified-alias expansion (not yet connected). */
public interface KeywordExpander {
    List<String> expand(String normalizedQuery);
}
