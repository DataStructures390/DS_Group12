package com.group12.tvsearch.service;

public interface SearchService {
    SearchResponse search(String query, int limit);
}
