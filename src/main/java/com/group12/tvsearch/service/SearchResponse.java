package com.group12.tvsearch.service;

import com.group12.tvsearch.model.ScoredSearchResult;
import java.util.List;

public record SearchResponse(
        String query, String status, String message, List<ScoredSearchResult> results) {}
