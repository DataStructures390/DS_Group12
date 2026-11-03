package com.group12.tvsearch.ranking;

import static org.junit.jupiter.api.Assertions.*;
import com.group12.tvsearch.model.ResultCategory;
import com.group12.tvsearch.model.SearchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class RankingAlgorithmTest {
    private SearchResult result(String url, double k, double m, double s, double f) {
        return new SearchResult(url, url, "", "example.org",
                ResultCategory.SERIES_INFORMATION, k, m, s, f);
    }

    @Test
    void computesWeightsAndFindsBestResult() {
        RankingAlgorithm ranker = new RankingAlgorithm();
        SearchResult medium = result("https://example.org/a", 80, 40, 60, 20);
        SearchResult best = result("https://example.org/b", 100, 100, 100, 100);
        assertEquals(59.0, ranker.score(medium), 0.0001);
        assertEquals("https://example.org/b", ranker.rank(List.of(medium, best), 1)
                .get(0).result().url());
    }

    @Test
    void removesDuplicateExactUrls() {
        ResultMerger merger = new ResultMerger();
        SearchResult r = result("https://example.org/a", 50, 50, 50, 50);
        assertEquals(1, merger.deduplicate(List.of(r, r)).size());
    }
}
