package com.group12.tvsearch.sources;

import static org.junit.jupiter.api.Assertions.*;
import com.group12.tvsearch.model.MetadataDocument;
import com.group12.tvsearch.model.LocalDocument;
import com.group12.tvsearch.model.ResultCategory;
import com.group12.tvsearch.model.Series;
import com.group12.tvsearch.model.SeriesAlias;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteMetadataRepositoryTest {
    @TempDir Path temp;
    private final Series sample = new Series(101, "Harbor Lights", "DEMO", "fixture:101");
    private MetadataDocument doc(String id, long seriesId, String url) {
        return new MetadataDocument(id, seriesId, url, "Original sample", "自寫 fictional body",
            ResultCategory.SERIES_INFORMATION, "Self-authored fixture", "Project-owned original");
    }

    @Test void migratesPersistsAndReopensWithoutDataLoss() throws Exception {
        Path file = temp.resolve("nested/metadata.sqlite");
        var repo = new SqliteMetadataRepository(file);
        repo.saveSeries(sample, true);
        repo.saveAlias(new SeriesAlias(101, "zh-Hant", "港灣燈火", true), "Self-authored fictional mapping");
        var document = doc("one", 101, "/demo/documents/one");
        repo.saveDocument(document);
        var reopened = new SqliteMetadataRepository(file);
        assertEquals(sample, reopened.findSeries(101).orElseThrow());
        assertEquals(List.of(sample), reopened.findVerifiedAliasCandidates("港灣燈火"));
        assertEquals(List.of(document), reopened.findDocuments());
        assertTrue(reopened.findSeries(999).isEmpty());
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var s = c.createStatement();
                var rs = s.executeQuery("PRAGMA user_version")) {
            assertTrue(rs.next()); assertEquals(1, rs.getInt(1));
        }
    }

    @Test void rejectsOutOfScopeOrUnverifiedEvidenceAndOrphans() throws Exception {
        var repo = new SqliteMetadataRepository(temp.resolve("constraints.sqlite"));
        assertThrows(IllegalArgumentException.class, () -> repo.saveSeries(sample, false));
        assertThrows(IllegalArgumentException.class, () -> repo.saveAlias(
            new SeriesAlias(101, "en", "Harbor", true), ""));
        assertThrows(SQLException.class, () -> repo.saveAlias(
            new SeriesAlias(101, "en", "Harbor", true), "Self-authored"));
        assertThrows(SQLException.class, () -> repo.saveDocument(doc("orphan", 101, "/demo/documents/orphan")));
        assertThrows(IllegalArgumentException.class, () -> new MetadataDocument("x", 1, "/x", "t", "b",
            ResultCategory.SERIES_INFORMATION, "", ""));
        repo.saveSeries(sample, true);
        repo.saveAlias(new SeriesAlias(101, "en", "AI guess", false), "");
        assertTrue(repo.findVerifiedAliasCandidates("AI guess").isEmpty());
    }

    @Test void aliasCollisionsReturnAllCandidatesWithoutGuessing() throws Exception {
        var repo = new SqliteMetadataRepository(temp.resolve("alias.sqlite"));
        var other = new Series(102, "Another Harbor", "DEMO", null);
        repo.saveSeries(sample, true); repo.saveSeries(other, true);
        repo.saveAlias(new SeriesAlias(101, "en", "Harbor", true), "Fictional mapping 1");
        repo.saveAlias(new SeriesAlias(102, "en", "HARBOR", true), "Fictional mapping 2");
        assertEquals(List.of(sample, other), repo.findVerifiedAliasCandidates(" ＨＡＲＢＯＲ "));
        assertTrue(repo.findVerifiedAliasCandidates("' OR 1=1 --").isEmpty());
        assertTrue(repo.findVerifiedAliasCandidates(null).isEmpty());
    }

    @Test void uniqueExternalIdsAndUrlsCannotSilentlyMergeDifferentRecords() throws Exception {
        var repo = new SqliteMetadataRepository(temp.resolve("unique.sqlite"));
        repo.saveSeries(sample, true);
        assertThrows(SQLException.class, () -> repo.saveSeries(new Series(102, "Other", "DEMO", "fixture:101"), true));
        repo.saveDocument(doc("one", 101, "/demo/documents/one"));
        assertThrows(SQLException.class, () -> repo.saveDocument(doc("two", 101, "/demo/documents/one")));
        assertEquals(1, repo.findDocuments().size());
        repo.saveSeries(new Series(101, "New title", "DEMO", "fixture:101"), true);
        assertEquals(1, repo.findDocuments().size(), "UPSERT must not delete children");
    }

    @Test void demoSeedIsRepeatableAndDoesNotOverwriteRealMetadata() throws Exception {
        var repo = new SqliteMetadataRepository(temp.resolve("demo.sqlite"));
        repo.seedLocalDemo(new LocalDocumentRepository());
        repo.seedLocalDemo(new LocalDocumentRepository());
        assertEquals(12, repo.findDocuments().size());
        assertEquals(1, repo.findVerifiedAliasCandidates("港灣燈火").size());
        var realRepo = new SqliteMetadataRepository(temp.resolve("real.sqlite"));
        var existing = new Series(1, "Preserve me", "US", null);
        realRepo.saveSeries(existing, true);
        assertThrows(SQLException.class, () -> realRepo.seedLocalDemo(new LocalDocumentRepository()));
        assertEquals(existing, realRepo.findSeries(1).orElseThrow());
        assertTrue(realRepo.findDocuments().isEmpty());
    }

    @Test void refusesNewerSchemaRatherThanDowngrading() throws Exception {
        Path file = temp.resolve("future.sqlite");
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file); var s = c.createStatement()) {
            s.execute("PRAGMA user_version = 2");
        }
        assertThrows(SQLException.class, () -> new SqliteMetadataRepository(file));
    }

    @Test void failedSeedRollsBackTheEntireBatch() throws Exception {
        var repo = new SqliteMetadataRepository(temp.resolve("rollback.sqlite"));
        var invalidFixture = new LocalDocument("invalid", -1, "Invalid", List.of(),
            "Invalid series", "Original", ResultCategory.SERIES_INFORMATION);
        var fixtures = new LocalDocumentRepository() {
            @Override public List<LocalDocument> findAll() {
                return List.of(super.findAll().get(0), invalidFixture);
            }
        };
        assertThrows(IllegalArgumentException.class, () -> repo.seedLocalDemo(fixtures));
        assertTrue(repo.findSeries(1).isEmpty());
        assertTrue(repo.findDocuments().isEmpty());
    }
}
