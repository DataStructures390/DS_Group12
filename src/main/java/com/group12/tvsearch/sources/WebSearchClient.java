package com.group12.tvsearch.sources;

import com.group12.tvsearch.model.SearchResult;
import java.util.List;

/** Interface for an authorized external web search provider. */
public interface WebSearchClient {
    List<SearchResult> search(String normalizedQuery, int limit);
}
