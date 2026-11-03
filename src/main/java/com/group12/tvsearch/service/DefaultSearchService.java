package com.group12.tvsearch.service;

import com.group12.tvsearch.model.SearchResult;
import com.group12.tvsearch.query.QueryProcessor;
import com.group12.tvsearch.ranking.RankingAlgorithm;
import com.group12.tvsearch.ranking.ResultMerger;
import com.group12.tvsearch.sources.WebSearchClient;
import java.util.List;
import org.springframework.stereotype.Service;

/** Stage 0 wiring. Local index and TMDB are not connected yet. */
@Service
public class DefaultSearchService implements SearchService {
    private final WebSearchClient web;
    private final QueryProcessor processor;
    private final ResultMerger merger;
    private final RankingAlgorithm ranking;

    public DefaultSearchService(WebSearchClient web, QueryProcessor processor,
                                ResultMerger merger, RankingAlgorithm ranking) {
        this.web = web;
        this.processor = processor;
        this.merger = merger;
        this.ranking = ranking;
    }

    @Override
    public SearchResponse search(String query, int limit) {
        String normalized = processor.normalize(query);
        if (normalized.isBlank()) {
            return new SearchResponse(query, "EMPTY_QUERY", "請輸入影集名稱。", List.of());
        }
        List<SearchResult> candidates = web.search(normalized, Math.max(1, limit));
        if (candidates.isEmpty()) {
            return new SearchResponse(query, "NO_LIVE_SEARCH",
                    "Stage 0：尚未連接網頁搜尋 API，沒有即時網頁結果。", List.of());
        }
        return new SearchResponse(query, "RESULTS_FROM_PROVIDER",
                "結果已經去重與排序；來源可信度仍需驗證。",
                ranking.rank(merger.deduplicate(candidates), limit));
    }
}
