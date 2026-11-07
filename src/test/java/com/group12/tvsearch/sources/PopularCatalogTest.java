package com.group12.tvsearch.sources;

import static org.junit.jupiter.api.Assertions.*;
import com.group12.tvsearch.model.*;
import com.group12.tvsearch.controller.CollectionController;
import com.group12.tvsearch.controller.SeriesController;
import com.group12.tvsearch.service.*;
import com.group12.tvsearch.query.QueryProcessor;
import com.group12.tvsearch.ranking.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.io.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PopularCatalogTest {
    @TempDir Path temporary;
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-11-07T00:00:00Z"), ZoneOffset.UTC);
    private static PrintStream silent() { return new PrintStream(OutputStream.nullOutputStream()); }
    private static TmdbCatalogSource.Basic basic(long id) {
        return new TmdbCatalogSource.Basic(new Series(id, "Synthetic " + id, "DE", "tmdb:tv:" + id),
            "Display " + id, "de", "2020-01-01");
    }
    private PopularCatalogStore store(int target) throws Exception {
        return new PopularCatalogStore(temporary.resolve("catalog.sqlite"), target, true, NOW);
    }
    private static TmdbCatalogSource source() {
        return new TmdbCatalogSource() {
            public Page popular(int page) {
                return new Page(page, 40, java.util.stream.LongStream.rangeClosed((page - 1) * 20L + 1, page * 20L).boxed().toList());
            }
            public Optional<Basic> basic(long id) { return id % 10 == 0 ? Optional.empty() : Optional.of(PopularCatalogTest.basic(id)); }
        };
    }

    @Test void importsExactly500ThenSearchesOfflineWithoutInventingFiveLanguageCoverage() throws Exception {
        var store = store(500);
        int[] calls = {0};
        assertEquals("COMPLETE", new PopularCatalogImporter(source(), store, () -> calls[0]++).run(1000, silent()));
        assertEquals(500, store.state().accepted());
        assertEquals(555, store.state().examined());
        assertEquals(583, calls[0]);
        assertEquals(calls[0], store.state().requests());
        assertEquals(500, store.snapshot().series().size());
        assertEquals(500, store.snapshot().documents().size());
        assertEquals(1000, store.snapshot().aliases().size()); // original German title + en-US display title
        var provider = new PopularSearchProvider(store);
        var results = provider.search("Synthetic 501", 10, null);
        assertEquals("TMDB_CATALOG_RESULTS", results.status());
        assertEquals(1, results.results().size());
        assertEquals("https://www.themoviedb.org/tv/501", results.results().get(0).result().url());
        assertEquals("TMDB_CATALOG", results.results().get(0).result().source());
        assertEquals(0, results.results().get(0).result().freshness());
        assertTrue(provider.search("Synthetic 501", 10, ResultCategory.PROFESSIONAL_REVIEW).results().isEmpty());
        assertEquals("COMPLETE", new PopularCatalogImporter(source(), store, () -> fail("Completed import must not call network")).run(1, silent()));
        assertEquals(500, new PopularCatalogStore(temporary.resolve("catalog.sqlite"), 500, true, NOW).state().accepted());
    }

    @Test void rateLimitStopsWithoutRetryAndResumesTheSameCandidate() throws Exception {
        var store = store(2);
        var source = new TmdbCatalogSource() {
            public Page popular(int page) { return new Page(page, 1, List.of(1L, 1L, 2L)); }
            public Optional<Basic> basic(long id) { throw new TmdbAccessException(TmdbAccessException.Reason.RATE_LIMITED); }
        };
        assertEquals("REQUEST_BUDGET", new PopularCatalogImporter(source, store, () -> {}).run(1, silent()));
        assertEquals(TmdbAccessException.Reason.RATE_LIMITED, assertThrows(TmdbAccessException.class,
            () -> new PopularCatalogImporter(source, store, () -> {}).run(5, silent())).reason());
        assertEquals(2, store.state().requests());
        assertEquals(0, store.state().accepted());
        assertEquals(1, store.pending().orElseThrow());
        var reopened = new PopularCatalogStore(temporary.resolve("catalog.sqlite"), 2, true, NOW);
        assertEquals("COMPLETE", new PopularCatalogImporter(source(), reopened, () -> {}).run(2, silent()));
        assertEquals(2, reopened.state().accepted());
        assertEquals(2, reopened.snapshot().series().size());
    }

    @Test void rejectedAndExhaustedSourceReportActualPartialCount() throws Exception {
        var store = store(3);
        var source = new TmdbCatalogSource() {
            public Page popular(int page) { return new Page(page, 1, List.of(1L)); }
            public Optional<Basic> basic(long id) { return Optional.empty(); }
        };
        assertEquals("SOURCE_EXHAUSTED", new PopularCatalogImporter(source, store, () -> {}).run(10, silent()));
        assertEquals(0, store.state().accepted());
        assertEquals(1, store.state().examined());
        assertTrue(store.snapshot().documents().isEmpty());
        assertThrows(java.sql.SQLException.class, () -> new PopularSearchProvider(store));
    }

    @Test void invalidCandidateRollsBackAndDoesNotAdvanceProgress() throws Exception {
        var store = store(2);
        store.addPage(new TmdbCatalogSource.Page(1, 1, List.of(1L)));
        assertThrows(IllegalArgumentException.class, () -> store.finish(1, Optional.of(basic(999))));
        assertEquals(0, store.state().accepted());
        assertEquals(1, store.pending().orElseThrow());
        assertTrue(store.snapshot().series().isEmpty());
        store.finish(1, Optional.of(basic(1)));
        assertEquals(1, store.snapshot().documents().size());
    }

    @Test void refusesExistingCuratedDatabaseChangedTargetsAndExpiredCache() throws Exception {
        Path curated = temporary.resolve("curated.sqlite");
        new SqliteMetadataRepository(curated).seedCuratedCatalog(CuratedCatalog.load());
        assertThrows(java.sql.SQLException.class, () -> new PopularCatalogStore(curated, 500, true, NOW));
        assertEquals(3, new SqliteMetadataRepository(curated).loadIndexSnapshot().series().size());
        store(2);
        assertThrows(java.sql.SQLException.class, () -> store(3));
        Clock expiry = Clock.fixed(Instant.parse("2027-05-07T00:00:00Z"), ZoneOffset.UTC);
        assertThrows(java.sql.SQLException.class, () -> new PopularCatalogStore(temporary.resolve("catalog.sqlite"), 2, false, expiry));
        assertThrows(IOException.class, () -> new PopularCatalogStore(temporary.resolve("missing.sqlite"), 500, false, NOW));
    }

    @Test void popularModeUsesDatabaseSearchAndLiveMetadataIdWithoutTitleSearch() throws Exception {
        var store = store(2);
        store.addPage(new TmdbCatalogSource.Page(1, 1, List.of(1L, 2L)));
        store.finish(1, Optional.of(basic(1))); store.finish(2, Optional.of(basic(2)));
        var provider = new PopularSearchProvider(store);
        var service = new DefaultSearchService((q, limit) -> { fail("Offline search must not call web"); return List.of(); },
            new QueryProcessor(), new ResultMerger(), new RankingAlgorithm(), null, Optional.empty(), Optional.of(provider));
        assertEquals(1, service.search("Display 2", 10).results().size());
        assertEquals("popular", new CollectionController(Optional.empty(), Optional.of(provider), false).info().collection());
        var resolver = new VerifiedSeriesResolver(new TmdbClient() {
            public Optional<Series> resolveSeries(String q) { fail("Do not title-search a cached identity"); return Optional.empty(); }
            public Optional<Series> resolveSeriesById(long id) { return Optional.of(basic(id).series()); }
        }, Optional.of(provider));
        assertEquals(2, resolver.resolve("Display 2").orElseThrow().id());
    }

    @Test void springPopularModeBuildsFromAnExistingCache() throws Exception {
        var store = store(1);
        store.addPage(new TmdbCatalogSource.Page(1, 1, List.of(1L)));
        store.finish(1, Optional.of(basic(1)));
        new ApplicationContextRunner().withUserConfiguration(PopularSearchProvider.class)
            .withPropertyValues("search.collection=popular",
                "search.popular.database=" + temporary.resolve("catalog.sqlite"))
            .run(context -> {
                assertNull(context.getStartupFailure());
                assertEquals(1, context.getBean(PopularSearchProvider.class).count());
            });
    }
}
