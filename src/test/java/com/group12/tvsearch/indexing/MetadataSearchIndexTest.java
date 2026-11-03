package com.group12.tvsearch.indexing;

import static org.junit.jupiter.api.Assertions.*;
import com.group12.tvsearch.model.MetadataDocument;
import com.group12.tvsearch.model.ResultCategory;
import com.group12.tvsearch.model.Series;
import com.group12.tvsearch.model.SeriesAlias;
import com.group12.tvsearch.sources.LocalDocumentRepository;
import com.group12.tvsearch.sources.SqliteMetadataRepository;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MetadataSearchIndexTest {
    @TempDir Path temp;

    private SqliteMetadataRepository seeded() throws Exception {
        var repository = new SqliteMetadataRepository(temp.resolve("metadata.sqlite"));
        repository.seedLocalDemo(new LocalDocumentRepository());
        return repository;
    }

    @ParameterizedTest
    @ValueSource(strings = {"Harbor Lights", "港灣燈火", "港湾灯火", "ハーバーライト", "항구의 빛"})
    void storedVerifiedFixtureAliasesFindSameDocumentsAcrossLanguages(String query) throws Exception {
        var repo = seeded();
        for (String title : List.of("港湾灯火", "ハーバーライト", "항구의 빛")) {
            repo.saveAlias(new SeriesAlias(1, "synthetic-demo", title, true), "Self-authored fictional test mapping; not a real translation claim");
        }
        var index = new MetadataSearchIndex(new SqliteMetadataRepository(temp.resolve("metadata.sqlite")));
        var response = index.search(query, 10, null);
        assertEquals("PERSISTED_LOCAL_RESULTS", response.status());
        assertEquals(4, response.results().size());
        assertTrue(response.results().stream().allMatch(r -> r.result().url().contains("harbor-")));
        assertEquals(75, response.results().get(0).score(), .001);
    }

    @Test void unverifiedAliasesDoNotEnterTheIndex() throws Exception {
        var repo = seeded();
        repo.saveAlias(new SeriesAlias(1, "en", "unverified-unique-guess", false), "");
        var response = new MetadataSearchIndex(repo).search("unverified-unique-guess", 10, null);
        assertEquals("NO_RESULTS", response.status());
        assertTrue(response.results().isEmpty());
    }

    @Test void titleCollisionReturnsAmbiguousInsteadOfPickingFirstId() throws Exception {
        var repo = seeded();
        repo.saveAlias(new SeriesAlias(1, "demo", "shared-title", true), "Original fixture one");
        repo.saveAlias(new SeriesAlias(2, "demo", "shared-title", true), "Original fixture two");
        var response = new MetadataSearchIndex(repo).search("SHARED-TITLE", 10, null);
        assertEquals("AMBIGUOUS_SERIES", response.status());
        assertTrue(response.results().isEmpty());
    }

    @Test void exactIdentityPreventsPartialKeywordCollisionsFromOtherSeries() throws Exception {
        var repo = seeded();
        repo.saveSeries(new Series(4, "Harbor Other", "DEMO", null), true);
        repo.saveDocument(new MetadataDocument("other", 4, "/demo/documents/other", "Harbor Other",
            "Self-authored unrelated story", ResultCategory.SERIES_INFORMATION, "Original", "Project-owned"));
        var response = new MetadataSearchIndex(repo).search("Harbor Lights", 50, null);
        assertEquals(4, response.results().size());
        assertFalse(response.results().stream().anyMatch(r -> r.result().url().endsWith("other")));
    }

    @Test void filtersBeforeTopKAndHandlesMissingEmptyAndClampedLimits() throws Exception {
        var index = new MetadataSearchIndex(seeded());
        var response = index.search("港灣燈火", 1, ResultCategory.STREAMING_PLATFORM);
        assertEquals(1, response.results().size());
        assertEquals(ResultCategory.STREAMING_PLATFORM, response.results().get(0).result().category());
        assertEquals("EMPTY_QUERY", index.search(null, 10, null).status());
        assertEquals("NO_RESULTS", index.search("攝影", 10, ResultCategory.STREAMING_PLATFORM).status());
        assertEquals(1, index.search("港灣燈火", 0, null).results().size());
        assertEquals(4, index.search("港灣燈火", 500, null).results().size());
    }

    @Test void rebuildPublishesUpdatesOnlyAfterSuccessfulCompletion() throws Exception {
        var repo = seeded();
        var failing = new SqliteMetadataRepository(temp.resolve("metadata.sqlite")) {
            boolean fail;
            @Override public IndexSnapshot loadIndexSnapshot() throws SQLException {
                if (fail) throw new SQLException("Controlled rebuild failure");
                return super.loadIndexSnapshot();
            }
        };
        var index = new MetadataSearchIndex(failing);
        repo.saveAlias(new SeriesAlias(1, "en", "newalias", true), "Original fixture mapping");
        assertEquals("NO_RESULTS", index.search("newalias", 10, null).status());
        failing.fail = true;
        assertThrows(SQLException.class, index::rebuild);
        assertEquals(4, index.search("港灣燈火", 10, null).results().size());
        failing.fail = false;
        index.rebuild();
        assertEquals(4, index.search("newalias", 10, null).results().size());
    }

    @Test void canonicalDuplicatesHaveDeterministicFirstDocumentSelection() throws Exception {
        var repo = seeded();
        repo.saveDocument(new MetadataDocument("duplicate-a", 1, "https://EXAMPLE.org:443/test#one", "Original first",
            "Original", ResultCategory.SERIES_INFORMATION, "Self-authored synthetic URL fixture", "Project-owned"));
        repo.saveDocument(new MetadataDocument("duplicate-b", 1, "https://example.org/test#two", "Original second",
            "Original", ResultCategory.SERIES_INFORMATION, "Self-authored synthetic URL fixture", "Project-owned"));
        var index = new MetadataSearchIndex(repo);
        var response = index.search("Harbor Lights", 50, null);
        assertEquals(5, response.results().size());
        assertTrue(response.results().stream().anyMatch(r -> r.result().title().equals("Original first")));
        assertFalse(response.results().stream().anyMatch(r -> r.result().title().equals("Original second")));
        assertEquals(response, index.search("Harbor Lights", 50, null));
    }

    @Test void emptyDatabaseIsAnHonestNoResult() throws Exception {
        var index = new MetadataSearchIndex(new SqliteMetadataRepository(temp.resolve("empty.sqlite")));
        assertEquals("NO_RESULTS", index.search("Harbor", 10, null).status());
    }
}
