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

    @Test void canonicalizesConservativelyAndPreservesQueryIdentity() {
        ResultMerger merger = new ResultMerger();
        assertEquals(1, merger.deduplicate(List.of(
            result("HTTPS://EXAMPLE.ORG:443/a#one", 0, 0, 0, 0),
            result("https://example.org/a#two", 0, 0, 0, 0))).size());
        assertEquals(2, merger.deduplicate(List.of(
            result("https://example.org/a?id=1", 0, 0, 0, 0),
            result("https://example.org/a?id=2", 0, 0, 0, 0))).size());
        assertEquals("https://example.org/a%2Fb?q=%20", ResultMerger.canonicalUrl("https://EXAMPLE.org:443/a%2Fb?q=%20#x"));
        assertEquals("/demo/documents/a", ResultMerger.canonicalUrl("/demo/documents/a"));
        assertEquals("bad url", ResultMerger.canonicalUrl("bad url"));
    }

    @Test void boundsFeaturesAndUsesStableUrlTieBreakForTopK() {
        RankingAlgorithm ranker = new RankingAlgorithm();
        assertEquals(45, ranker.score(result("x", 200, -1, Double.NaN, Double.POSITIVE_INFINITY)), .001);
        var a = result("https://example.org/a", 100, 0, 0, 0);
        var b = result("https://example.org/b", 100, 0, 0, 0);
        assertEquals(a, ranker.rank(List.of(b, a), 1).get(0).result());
        assertEquals(List.of(a, b), ranker.rank(List.of(b, a), 100).stream().map(r -> r.result()).toList());
        assertTrue(ranker.rank(List.of(a), 0).isEmpty());
        assertTrue(ranker.rank(List.of(), 10).isEmpty());
    }
}
