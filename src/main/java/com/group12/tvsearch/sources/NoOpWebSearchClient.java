package com.group12.tvsearch.sources;

import com.group12.tvsearch.model.SearchResult;
import java.util.List;

/** No external API is installed yet. Never manufacture real-looking web results. */
public class NoOpWebSearchClient implements WebSearchClient {
    @Override
    public List<SearchResult> search(String normalizedQuery, int limit) {
        return List.of();
    }
}
