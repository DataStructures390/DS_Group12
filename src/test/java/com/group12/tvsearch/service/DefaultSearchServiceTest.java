package com.group12.tvsearch.service;

import static org.junit.jupiter.api.Assertions.*;
import com.group12.tvsearch.query.QueryProcessor;
import com.group12.tvsearch.ranking.RankingAlgorithm;
import com.group12.tvsearch.ranking.ResultMerger;
import com.group12.tvsearch.sources.NoOpWebSearchClient;
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
}
