package com.group12.tvsearch.sources;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CuratedCatalogTest {
    @TempDir Path temp;
    @Test void catalogHasFifteenEvidenceLinksAndOriginalSummaries() throws Exception {
        var catalog = CuratedCatalog.load();
        assertEquals("2026-11-04", catalog.checkedOn());
        assertEquals(3, catalog.snapshot().series().size());
        assertEquals(15, catalog.snapshot().aliases().size());
        assertEquals(15, catalog.snapshot().documents().size());
        assertTrue(catalog.snapshot().documents().stream().allMatch(d -> d.license().contains("Project-authored")));
    }
    @Test void existingDatabaseMustMatchCatalogWithoutSilentOverwrite() throws Exception {
        Path database = temp.resolve("curated.sqlite");
        var provider = new CuratedSearchProvider(database.toString());
        assertEquals(5, provider.search("絕命毒師", 10, null).results().size());
        assertEquals(5, new CuratedSearchProvider(database.toString()).search("絕命毒師", 10, null).results().size());
        var repo = new SqliteMetadataRepository(database);
        assertThrows(java.sql.SQLException.class, () -> repo.seedCuratedCatalog(CuratedCatalog.load()));
        repo.saveAlias(new com.group12.tvsearch.model.SeriesAlias(100001, "en", "unreviewed-change", true), "Unreviewed source");
        assertThrows(java.io.IOException.class, () -> new CuratedSearchProvider(database.toString()));
        assertEquals(1, repo.findVerifiedAliasCandidates("unreviewed-change").size(), "Contributor changes must survive refusal");
    }
    @Test void fictionalDatabaseCannotBeRelabeledAsRealCuratedSources() throws Exception {
        Path database = temp.resolve("fictional.sqlite");
        new SqliteMetadataRepository(database).seedLocalDemo(new LocalDocumentRepository());
        assertThrows(java.io.IOException.class, () -> new CuratedSearchProvider(database.toString()));
    }
    @Test void rejectsUnapprovedEvidenceDomainsAndIncompleteLocales() throws Exception {
        var original = CuratedCatalog.load().series().get(0);
        var bad = new CuratedCatalog.Alias("en", "Bad", "https://example.org/not-verified");
        var entry = new CuratedCatalog.Entry(100004, "Bad", "US", "Original summary", List.of(bad));
        assertThrows(IllegalArgumentException.class, () -> new CuratedCatalog("2026-11-04", List.of(entry)));
        var incomplete = new CuratedCatalog.Entry(100004, "Other", "US", "Original", List.of(original.aliases().get(0)));
        assertThrows(IllegalArgumentException.class, () -> new CuratedCatalog("2026-11-04", List.of(incomplete)));
    }
}
