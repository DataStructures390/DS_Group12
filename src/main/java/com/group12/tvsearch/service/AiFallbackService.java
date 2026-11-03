package com.group12.tvsearch.service;

import java.util.List;

/** AI suggestions only; never generates asserted search result URLs. */
public interface AiFallbackService {
    List<String> suggestQueries(String normalizedQuery);
}
