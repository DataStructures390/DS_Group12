package com.group12.tvsearch.sources;

import com.group12.tvsearch.model.SearchResult;
import java.util.List;
import java.util.Optional;

/** Brave Web Search adapter. Brave allows at most 20 results per request. */
public final class BraveWebSearchClient implements WebSearchClient {
    static final int MAX_COUNT = 20;
    private final BraveTransport transport;
    private final BraveResultMapper mapper;
    private final String sourceLabel;
    private final String notice;
    private final List<String> sampleQueries;

    public BraveWebSearchClient(BraveTransport transport, BraveResultMapper mapper,
                                String sourceLabel, String notice, List<String> sampleQueries) {
        this.transport = transport;
        this.mapper = mapper;
        this.sourceLabel = sourceLabel;
        this.notice = notice;
        this.sampleQueries = List.copyOf(sampleQueries);
    }

    @Override
    public List<SearchResult> search(String normalizedQuery, int limit) {
        int count = Math.max(1, Math.min(MAX_COUNT, limit));
        Optional<String> body;
        try {
            body = transport.fetch(normalizedQuery, count);
        } catch (RuntimeException e) {
            return List.of();
        }
        if (body.isEmpty()) return List.of();
        return mapper.map(body.get(), normalizedQuery, sourceLabel).stream().limit(count).toList();
    }

    @Override
    public String resultNotice() {
        return notice;
    }

    @Override
    public String emptyNotice() {
        return sampleQueries.isEmpty() ? "網頁搜尋沒有找到結果。"
            : "Brave 假資料只收錄示範查詢：" + String.join("、", sampleQueries) + "。";
    }

    @Override
    public List<String> sampleQueries() {
        return sampleQueries;
    }
}
