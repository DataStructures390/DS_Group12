package com.group12.tvsearch.sources;

import com.group12.tvsearch.indexing.MetadataSearchIndex;
import com.group12.tvsearch.model.*;
import com.group12.tvsearch.query.QueryProcessor;
import com.group12.tvsearch.service.SearchResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Explicit offline collection; no imports or provider calls are triggered by application startup. */
@Component
@ConditionalOnProperty(name = "search.collection", havingValue = "popular")
public class PopularSearchProvider {
    private final PopularCatalogStore store;
    private final MetadataSearchIndex index;
    private final List<String> examples;
    private final Map<String, Set<Series>> aliases = new HashMap<>();
    private final QueryProcessor processor = new QueryProcessor();
    private final int count;

    @org.springframework.beans.factory.annotation.Autowired
    public PopularSearchProvider(@Value("${search.popular.database:data/tmdb-popular.sqlite}") String database)
            throws SQLException, IOException {
        this(new PopularCatalogStore(Path.of(database), 500, false, Clock.systemUTC()));
    }

    public PopularSearchProvider(PopularCatalogStore store) throws SQLException {
        this.store = store;
        var snapshot = store.snapshot();
        count = snapshot.series().size();
        if (count == 0) throw new SQLException("Catalog is empty; run the approved import first");
        index = new MetadataSearchIndex(store.repository);
        var identities = new HashMap<Long, Series>();
        snapshot.series().forEach(series -> {
            identities.put(series.id(), series);
            aliases.computeIfAbsent(processor.normalize(series.originalTitle()), ignored -> new HashSet<>()).add(series);
        });
        snapshot.aliases().forEach(entry -> aliases.computeIfAbsent(
            processor.normalize(entry.alias().title()), ignored -> new HashSet<>()).add(identities.get(entry.alias().seriesId())));
        examples = snapshot.series().stream().limit(3).map(Series::originalTitle).toList();
    }

    public int count() { return count; }
    public List<String> examples() { return examples; }
    public boolean fresh() {
        try { store.requireFresh(); return true; }
        catch (SQLException e) { return false; }
    }
    public Set<Series> candidates(String query) {
        return fresh() ? Set.copyOf(aliases.getOrDefault(processor.normalize(query), Set.of())) : Set.of();
    }
    public String description() {
        return "TMDB 熱門候選中已通過範圍檢查的 " + count
            + " 部影集基本資料；本地快取，只有影集資訊分類，尚未補齊五語別名或即時網頁搜尋。";
    }
    public SearchResponse search(String query, int limit, ResultCategory category) {
        if (!fresh()) return new SearchResponse(query, "CATALOG_EXPIRED",
                "影集資料快取已過期或無法讀取，請更新資料庫後重新啟動。", List.of());
        var response = index.search(query, limit, category);
        var rows = response.results().stream().map(row -> {
            var s = row.result();
            return new ScoredSearchResult(new SearchResult(s.url(), s.title(), s.snippet(), "TMDB_CATALOG", s.category(),
                s.keywordRelevance(), s.seriesRelevance(), s.sourceRelevance(), s.freshness()), row.score());
        }).toList();
        return new SearchResponse(query, rows.isEmpty() ? response.status() : "TMDB_CATALOG_RESULTS", description(), rows);
    }
}
