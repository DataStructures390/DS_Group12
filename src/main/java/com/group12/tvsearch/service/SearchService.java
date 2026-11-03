package com.group12.tvsearch.service;

public interface SearchService {
    SearchResponse search(String query, int limit);

    default SearchResponse search(String query, int limit,
            com.group12.tvsearch.model.ResultCategory category) {
        if (category == null) return search(query, limit);
        throw new UnsupportedOperationException("Category filtering is not implemented");
    }
}
