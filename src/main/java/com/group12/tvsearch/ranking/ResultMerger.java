package com.group12.tvsearch.ranking;

import com.group12.tvsearch.model.SearchResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Exact URL dedup only; canonical URL rules still need implementation. */
@Component
public class ResultMerger {
    public List<SearchResult> deduplicate(List<SearchResult> candidates) {
        Set<String> seen = new HashSet<>();
        List<SearchResult> unique = new ArrayList<>();
        for (SearchResult candidate : candidates) {
            if (seen.add(candidate.url().trim())) unique.add(candidate);
        }
        return unique;
    }
}
