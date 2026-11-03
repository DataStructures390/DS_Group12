package com.group12.tvsearch.ranking;

import com.group12.tvsearch.model.ScoredSearchResult;
import com.group12.tvsearch.model.SearchResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import org.springframework.stereotype.Component;

/** Initial 45/30/15/10 scoring and PriorityQueue top-K selection. */
@Component
public class RankingAlgorithm {
    public double score(SearchResult r) {
        return .45 * bound(r.keywordRelevance()) + .30 * bound(r.seriesRelevance())
                + .15 * bound(r.sourceRelevance()) + .10 * bound(r.freshness());
    }

    public List<ScoredSearchResult> rank(List<SearchResult> candidates, int k) {
        if (k <= 0) return List.of();
        Comparator<ScoredSearchResult> ascending = Comparator
                .comparingDouble(ScoredSearchResult::score)
                .thenComparing(r -> r.result().url());
        PriorityQueue<ScoredSearchResult> heap = new PriorityQueue<>(ascending);
        for (SearchResult candidate : candidates) {
            heap.offer(new ScoredSearchResult(candidate, score(candidate)));
            if (heap.size() > k) heap.poll();
        }
        List<ScoredSearchResult> result = new ArrayList<>(heap);
        result.sort(ascending.reversed());
        return result;
    }

    private static double bound(double value) {
        return Double.isFinite(value) ? Math.max(0, Math.min(100, value)) : 0;
    }
}
