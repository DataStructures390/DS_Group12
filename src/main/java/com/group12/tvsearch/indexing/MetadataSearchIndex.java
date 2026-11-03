package com.group12.tvsearch.indexing;

import com.group12.tvsearch.model.MetadataDocument;
import com.group12.tvsearch.model.ResultCategory;
import com.group12.tvsearch.model.SearchResult;
import com.group12.tvsearch.query.QueryProcessor;
import com.group12.tvsearch.ranking.RankingAlgorithm;
import com.group12.tvsearch.ranking.ResultMerger;
import com.group12.tvsearch.service.SearchResponse;
import com.group12.tvsearch.sources.SqliteMetadataRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Explicit offline SQLite-to-index path; no network, AI or automatic source verification. */
public class MetadataSearchIndex {
    private final SqliteMetadataRepository repository;
    private final QueryProcessor processor = new QueryProcessor();
    private final RankingAlgorithm ranking = new RankingAlgorithm();
    private final ResultMerger merger = new ResultMerger();
    private volatile Snapshot snapshot;

    public MetadataSearchIndex(SqliteMetadataRepository repository) throws SQLException {
        this.repository = repository;
        rebuild();
    }

    /** Publish only a complete successful rebuild; readers keep the previous consistent snapshot. */
    public synchronized void rebuild() throws SQLException {
        var stored = repository.loadIndexSnapshot();
        Map<Long, String> identity = new HashMap<>();
        Map<String, Set<Long>> aliasIds = new HashMap<>();
        for (var series : stored.series()) {
            identity.put(series.id(), series.originalTitle());
            aliasIds.computeIfAbsent(processor.normalize(series.originalTitle()), k -> new HashSet<>()).add(series.id());
        }
        for (var entry : stored.aliases()) {
            var alias = entry.alias();
            identity.merge(alias.seriesId(), alias.title(), (left, right) -> left + " " + right);
            aliasIds.computeIfAbsent(processor.normalize(alias.title()), k -> new HashSet<>()).add(alias.seriesId());
        }
        var index = new InvertedIndex();
        Map<String, MetadataDocument> documents = new HashMap<>();
        Map<String, Set<String>> terms = new HashMap<>();
        Map<Long, Set<String>> identityTerms = new HashMap<>();
        identity.forEach((id, text) -> identityTerms.put(id, Set.copyOf(Tokenizer.tokens(text))));
        for (var document : stored.documents()) {
            String text = identity.getOrDefault(document.seriesId(), "") + " " + document.title() + " " + document.body();
            documents.put(document.id(), document);
            terms.put(document.id(), Set.copyOf(Tokenizer.tokens(text)));
            index.addDocument(document.id(), text);
        }
        Map<String, Set<Long>> immutableAliasIds = new HashMap<>();
        aliasIds.forEach((title, ids) -> immutableAliasIds.put(title, Set.copyOf(ids)));
        snapshot = new Snapshot(index, Map.copyOf(documents), Map.copyOf(terms),
            Map.copyOf(identityTerms), Map.copyOf(immutableAliasIds));
    }

    public SearchResponse search(String query, int limit, ResultCategory category) {
        String normalized = processor.normalize(query);
        if (normalized.isBlank()) return new SearchResponse(query, "EMPTY_QUERY", "請輸入影集或關鍵字。", List.of());
        Snapshot current = snapshot;
        Set<Long> exact = current.aliasIds().getOrDefault(normalized, Set.of());
        if (exact.size() > 1) return new SearchResponse(query, "AMBIGUOUS_SERIES",
            "此名稱對應多個已記錄的影集 ID，請使用更明確的原名；未猜測或呼叫外部服務。", List.of());
        List<String> queryTerms = Tokenizer.tokens(normalized);
        List<SearchResult> candidates = new ArrayList<>();
        for (String id : current.index().search(normalized).stream().sorted().toList()) {
            var d = current.documents().get(id);
            if (category != null && d.category() != category) continue;
            if (!exact.isEmpty() && !exact.contains(d.seriesId())) continue;
            candidates.add(new SearchResult(d.url(), d.title(), d.body(), "PERSISTED_LOCAL", d.category(),
                coverage(queryTerms, current.terms().get(id)),
                coverage(queryTerms, current.identityTerms().getOrDefault(d.seriesId(), Set.of())), 0, 0));
        }
        return new SearchResponse(query, candidates.isEmpty() ? "NO_RESULTS" : "PERSISTED_LOCAL_RESULTS",
            "本地 SQLite 文件與有證據的別名索引；不是即時網頁結果，內容授權依資料庫紀錄。",
            ranking.rank(merger.deduplicate(candidates), Math.max(1, Math.min(50, limit))));
    }

    private static double coverage(List<String> queryTerms, Set<String> terms) {
        if (queryTerms.isEmpty()) return 0;
        return 100.0 * queryTerms.stream().filter(terms::contains).count() / queryTerms.size();
    }

    private record Snapshot(InvertedIndex index, Map<String, MetadataDocument> documents,
                             Map<String, Set<String>> terms, Map<Long, Set<String>> identityTerms,
                             Map<String, Set<Long>> aliasIds) {}
}
