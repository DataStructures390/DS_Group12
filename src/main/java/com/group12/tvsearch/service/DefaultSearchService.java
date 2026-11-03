package com.group12.tvsearch.service;

import com.group12.tvsearch.model.SearchResult;
import com.group12.tvsearch.query.QueryProcessor;
import com.group12.tvsearch.ranking.RankingAlgorithm;
import com.group12.tvsearch.ranking.ResultMerger;
import com.group12.tvsearch.sources.WebSearchClient;
import com.group12.tvsearch.sources.LocalSearchProvider;
import com.group12.tvsearch.sources.CuratedSearchProvider;
import java.util.Optional;
import com.group12.tvsearch.model.ResultCategory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.springframework.stereotype.Service;

/** Local demonstration first; external NoOp behavior remains explicit. */
@Service
public class DefaultSearchService implements SearchService {
    private final WebSearchClient web;
    private final QueryProcessor processor;
    private final ResultMerger merger;
    private final RankingAlgorithm ranking;
    private final LocalSearchProvider local;
    private final Optional<CuratedSearchProvider> curated;

    public DefaultSearchService(WebSearchClient web, QueryProcessor processor,
                                ResultMerger merger, RankingAlgorithm ranking) {
        this(web, processor, merger, ranking, null, Optional.empty());
    }

    public DefaultSearchService(WebSearchClient web, QueryProcessor processor,
                                ResultMerger merger, RankingAlgorithm ranking,
                                LocalSearchProvider local) {
        this(web, processor, merger, ranking, local, Optional.empty());
    }

    @Autowired
    public DefaultSearchService(WebSearchClient web, QueryProcessor processor,
                                ResultMerger merger, RankingAlgorithm ranking,
                                LocalSearchProvider local, Optional<CuratedSearchProvider> curated) {
        this.web = web;
        this.processor = processor;
        this.merger = merger;
        this.ranking = ranking;
        this.local = local;
        this.curated = curated;
    }

    @Override
    public SearchResponse search(String query, int limit) {
        return search(query, limit, null);
    }

    @Override
    public SearchResponse search(String query, int limit, ResultCategory category) {
        String normalized = processor.normalize(query);
        if (normalized.isBlank()) {
            return new SearchResponse(query, "EMPTY_QUERY", "請輸入影集名稱。", List.of());
        }
        int boundedLimit = Math.max(1, Math.min(50, limit));
        if (curated.isPresent()) return curated.get().search(query, boundedLimit, category);
        List<SearchResult> localCandidates = local == null ? List.of() : local.search(normalized);
        if (!localCandidates.isEmpty()) {
            List<SearchResult> filtered = localCandidates.stream()
                    .filter(r -> category == null || r.category() == category).toList();
            return new SearchResponse(query, filtered.isEmpty() ? "NO_RESULTS" : "LOCAL_DEMO_RESULTS",
                    "本地虛構示範資料；不是即時網頁結果。來源與日期未評分，S/F 為 0。",
                    ranking.rank(merger.deduplicate(filtered), boundedLimit));
        }
        List<SearchResult> candidates = web.search(normalized, boundedLimit);
        if (candidates.isEmpty()) {
            return new SearchResponse(query, "NO_LIVE_SEARCH",
                    "沒有符合的本地文件；尚未連接網頁搜尋 API，沒有即時網頁結果。", List.of());
        }
        List<SearchResult> filtered = candidates.stream()
                .filter(r -> category == null || r.category() == category).toList();
        return new SearchResponse(query, filtered.isEmpty() ? "NO_RESULTS" : "RESULTS_FROM_PROVIDER",
                "結果已經去重與排序；來源可信度仍需驗證。",
                ranking.rank(merger.deduplicate(filtered), boundedLimit));
    }
}
