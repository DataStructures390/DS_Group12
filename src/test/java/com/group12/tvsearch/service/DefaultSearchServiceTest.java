package com.group12.tvsearch.service;

import static org.junit.jupiter.api.Assertions.*;
import com.group12.tvsearch.model.ResultCategory;
import com.group12.tvsearch.model.SearchResult;
import com.group12.tvsearch.query.QueryProcessor;
import com.group12.tvsearch.ranking.RankingAlgorithm;
import com.group12.tvsearch.ranking.ResultMerger;
import com.group12.tvsearch.sources.NoOpWebSearchClient;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultSearchServiceTest {
    @Test
    void noWebProviderMeansNoFabricatedResults() {
        SearchService service = new DefaultSearchService(
                new NoOpWebSearchClient(), new QueryProcessor(),
                new ResultMerger(), new RankingAlgorithm());
        SearchResponse response = service.search("絕命毒師", 10);
        assertEquals("NO_LIVE_SEARCH", response.status());
        assertTrue(response.results().isEmpty());
    }

    @Test
    void ranksProviderResultsWithSchemeRelativeUrls() {
        SearchResult relative = new SearchResult("//example.org/series", "Series guide", "", "example.org",
                ResultCategory.SERIES_INFORMATION, 100, 100, 0, 0);
        SearchResult absolute = new SearchResult("https://example.org/review", "Series review", "", "example.org",
                ResultCategory.PROFESSIONAL_REVIEW, 50, 100, 0, 0);
        SearchService service = new DefaultSearchService(
                (query, limit) -> List.of(absolute, relative, relative), new QueryProcessor(),
                new ResultMerger(), new RankingAlgorithm());

        SearchResponse response = service.search("series", 10);

        assertEquals("RESULTS_FROM_PROVIDER", response.status());
        assertEquals(List.of(relative, absolute), response.results().stream().map(result -> result.result()).toList());
    }
}
