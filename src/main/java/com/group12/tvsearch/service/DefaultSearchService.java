package com.group12.tvsearch.service;

import com.group12.tvsearch.model.SearchResult;
import com.group12.tvsearch.query.QueryProcessor;
import com.group12.tvsearch.ranking.RankingAlgorithm;
import com.group12.tvsearch.ranking.ResultMerger;
import com.group12.tvsearch.sources.WebSearchClient;
import com.group12.tvsearch.sources.LocalSearchProvider;
import com.group12.tvsearch.sources.CuratedSearchProvider;
import com.group12.tvsearch.sources.PopularSearchProvider;
import java.util.Optional;
import com.group12.tvsearch.model.ResultCategory;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.springframework.stereotype.Service;

/** Local demonstration first; external NoOp behavior remains explicit. */
@Service
public class DefaultSearchService implements SearchService {
    /** Upper bound for returned results; also the candidate pool requested from web search. */
    static final int MAX_LIMIT = 50;
    private final WebSearchClient web;
    private final QueryProcessor processor;
    private final ResultMerger merger;
    private final RankingAlgorithm ranking;
    private final LocalSearchProvider local;
    private final Optional<CuratedSearchProvider> curated;
    private final Optional<PopularSearchProvider> popular;

    public DefaultSearchService(WebSearchClient web, QueryProcessor processor,
                                ResultMerger merger, RankingAlgorithm ranking) {
        this(web, processor, merger, ranking, null, Optional.empty());
    }

    public DefaultSearchService(WebSearchClient web, QueryProcessor processor,
                                ResultMerger merger, RankingAlgorithm ranking,
                                LocalSearchProvider local) {
        this(web, processor, merger, ranking, local, Optional.empty());
    }

    public DefaultSearchService(WebSearchClient web, QueryProcessor processor,
                                ResultMerger merger, RankingAlgorithm ranking,
                                LocalSearchProvider local, Optional<CuratedSearchProvider> curated) {
        this(web, processor, merger, ranking, local, curated, Optional.empty());
    }

    @Autowired
    public DefaultSearchService(WebSearchClient web, QueryProcessor processor,
                                ResultMerger merger, RankingAlgorithm ranking,
                                LocalSearchProvider local, Optional<CuratedSearchProvider> curated,
                                Optional<PopularSearchProvider> popular) {
        this.web = web;
        this.processor = processor;
        this.merger = merger;
        this.ranking = ranking;
        this.local = local;
        this.curated = curated;
        this.popular = popular;
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
        int boundedLimit = Math.max(1, Math.min(MAX_LIMIT, limit));
        if (popular.isPresent()) return popular.get().search(query, boundedLimit, category);
        if (curated.isPresent()) return curated.get().search(query, boundedLimit, category);
        List<SearchResult> localCandidates = local == null ? List.of() : local.search(normalized);
        if (!localCandidates.isEmpty()) {
            List<SearchResult> filtered = localCandidates.stream()
                    .filter(r -> category == null || r.category() == category).toList();
            return new SearchResponse(query, filtered.isEmpty() ? "NO_RESULTS" : "LOCAL_DEMO_RESULTS",
                    "本地虛構示範資料；不是即時網頁結果。來源與日期未評分，S/F 為 0。",
                    ranking.rank(merger.deduplicate(filtered), boundedLimit));
        }
        // Ask for the widest candidate pool so category filtering and URL de-duplication
        // happen before Top-K; the web client clamps this to its own per-request maximum.
        List<SearchResult> candidates = web.search(normalized, MAX_LIMIT);
        if (candidates.isEmpty()) {
            return new SearchResponse(query, "NO_LIVE_SEARCH",
                    "沒有符合的本地文件；" + web.emptyNotice(), List.of());
        }
        List<SearchResult> filtered = candidates.stream()
                .filter(r -> category == null || r.category() == category).toList();
        return new SearchResponse(query, filtered.isEmpty() ? "NO_RESULTS" : "RESULTS_FROM_PROVIDER",
                web.resultNotice(),
                ranking.rank(merger.deduplicate(filtered), boundedLimit));
    }
}
