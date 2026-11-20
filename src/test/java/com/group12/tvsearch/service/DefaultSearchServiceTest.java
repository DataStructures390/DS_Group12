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
    void overlongQueryIsRejectedBeforeAnyProviderRequest() {
        int[] calls = {0};
        SearchService service = new DefaultSearchService((query, limit) -> {
            calls[0]++;
            return List.of();
        }, new QueryProcessor(), new ResultMerger(), new RankingAlgorithm());

        SearchResponse tooLong = service.search("a".repeat(DefaultSearchService.MAX_QUERY_LENGTH + 1), 10);
        assertEquals("QUERY_TOO_LONG", tooLong.status());
        assertTrue(tooLong.results().isEmpty());
        assertEquals(0, calls[0]);

        SearchResponse atLimit = service.search("a".repeat(DefaultSearchService.MAX_QUERY_LENGTH), 10);
        assertEquals("NO_LIVE_SEARCH", atLimit.status());
        assertEquals(1, calls[0]);
    }

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

    @Test
    void providerCategoryFilterAndDeduplicationHappenBeforeTopK() {
        SearchResult discussion = new SearchResult("https://forum.example/a", "Series talk", "", "forum.example",
                ResultCategory.COMMUNITY_DISCUSSION, 100, 100, 0, 0);
        SearchResult duplicate = new SearchResult("https://FORUM.example:443/a#c", "Series talk", "", "forum.example",
                ResultCategory.COMMUNITY_DISCUSSION, 100, 100, 0, 0);
        SearchResult review = new SearchResult("https://review.example/b", "Series review", "", "review.example",
                ResultCategory.PROFESSIONAL_REVIEW, 50, 50, 0, 0);
        List<SearchResult> raw = List.of(discussion, duplicate, review);
        int[] requested = {0};
        SearchService service = new DefaultSearchService((query, limit) -> {
            requested[0] = limit;
            return raw.subList(0, Math.min(limit, raw.size()));
        }, new QueryProcessor(), new ResultMerger(), new RankingAlgorithm());

        SearchResponse filtered = service.search("series", 1, ResultCategory.PROFESSIONAL_REVIEW);
        assertEquals("RESULTS_FROM_PROVIDER", filtered.status());
        assertEquals(List.of(review), filtered.results().stream().map(result -> result.result()).toList());
        assertEquals(DefaultSearchService.MAX_LIMIT, requested[0]);

        SearchResponse topTwo = service.search("series", 2);
        assertEquals(List.of(discussion, review), topTwo.results().stream().map(result -> result.result()).toList());
    }
}
