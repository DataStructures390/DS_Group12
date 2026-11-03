package com.group12.tvsearch.sources;

import com.group12.tvsearch.indexing.MetadataSearchIndex;
import com.group12.tvsearch.model.ResultCategory;
import com.group12.tvsearch.model.ScoredSearchResult;
import com.group12.tvsearch.model.SearchResult;
import com.group12.tvsearch.service.SearchResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Explicit opt-in collection; all retrieval is local and its real links were manually checked. */
@Component
@ConditionalOnProperty(name = "search.collection", havingValue = "curated")
public class CuratedSearchProvider {
    private final MetadataSearchIndex index;
    public CuratedSearchProvider(@Value("${search.curated.database:data/curated.sqlite}") String database)
            throws SQLException, IOException {
        Path path = Path.of(database).toAbsolutePath().normalize();
        boolean exists = Files.exists(path);
        var catalog = CuratedCatalog.load();
        var repository = new SqliteMetadataRepository(path);
        if (!exists) repository.seedCuratedCatalog(catalog);
        if (!repository.loadIndexSnapshot().equals(catalog.snapshot()))
            throw new IOException("Selected database does not match the checked curated catalog; use a dedicated new database path");
        index = new MetadataSearchIndex(repository);
    }
    public SearchResponse search(String query, int limit, ResultCategory category) {
        var response = index.search(query, limit, category);
        var results = response.results().stream().map(row -> {
            var r = row.result();
            return new ScoredSearchResult(new SearchResult(r.url(), r.title(), r.snippet(), "CURATED_LOCAL", r.category(),
                r.keywordRelevance(), r.seriesRelevance(), r.sourceRelevance(), r.freshness()), row.score());
        }).toList();
        return new SearchResponse(query, results.isEmpty() ? response.status() : "CURATED_LOCAL_RESULTS",
            "人工整理的本地資料集：真實影集與已核對 Wikipedia 連結；不是即時網頁搜尋。只有影集資訊分類。", results);
    }
}
