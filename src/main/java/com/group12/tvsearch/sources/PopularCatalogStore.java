package com.group12.tvsearch.sources;

import com.group12.tvsearch.model.*;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Dedicated, resumable API cache. Never adds rows to the immutable curated database. */
public final class PopularCatalogStore {
    final SqliteMetadataRepository repository;
    private final Clock clock;
    public record State(int target, int nextPage, int totalPages, int accepted, int examined,
                        int requests, Instant createdAt) {
        public Instant expiresAt() { return createdAt.atZone(ZoneOffset.UTC).plusMonths(6).toInstant(); }
    }

    public PopularCatalogStore(Path path, int target, boolean initialize, Clock clock) throws SQLException, IOException {
        if (target < 1 || target > 500) throw new IllegalArgumentException("Target must be 1..500");
        if (!initialize && !java.nio.file.Files.isRegularFile(path)) throw new IOException("Import the catalog first");
        this.clock = clock;
        repository = new SqliteMetadataRepository(path);
        try (var c = repository.open()) {
            c.setAutoCommit(false);
            try {
                boolean managed;
                try (var s = c.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name='tmdb_import_state'");
                     var rs = s.executeQuery()) { managed = rs.next(); }
                if (!managed) {
                    if (!initialize || scalar(c, "SELECT COUNT(*) FROM series") != 0
                            || scalar(c, "SELECT COUNT(*) FROM document") != 0)
                        throw new SQLException("A dedicated empty catalog database is required");
                    try (var s = c.createStatement()) {
                        s.execute("CREATE TABLE tmdb_import_state (id INTEGER PRIMARY KEY CHECK(id=1), version INTEGER NOT NULL, target INTEGER NOT NULL, next_page INTEGER NOT NULL, total_pages INTEGER NOT NULL, requests INTEGER NOT NULL, created_at TEXT NOT NULL)");
                        s.execute("CREATE TABLE tmdb_candidate (id INTEGER PRIMARY KEY, ordinal INTEGER UNIQUE NOT NULL, status TEXT NOT NULL CHECK(status IN ('PENDING','ACCEPTED','REJECTED')))");
                        s.execute("CREATE TABLE tmdb_basic (series_id INTEGER PRIMARY KEY REFERENCES series(id), display_title TEXT NOT NULL, original_language TEXT NOT NULL, first_air_date TEXT NOT NULL, fetched_at TEXT NOT NULL)");
                    }
                    try (var s = c.prepareStatement("INSERT INTO tmdb_import_state VALUES(1,1,?,1,100,0,?)")) {
                        s.setInt(1, target); s.setString(2, clock.instant().toString()); s.executeUpdate();
                    }
                }
                try (var s = c.prepareStatement("SELECT version,target FROM tmdb_import_state WHERE id=1"); var rs = s.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) != 1 || (initialize && rs.getInt(2) != target))
                        throw new SQLException("Unsupported catalog version or changed import target");
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
        requireFresh();
    }

    private static int scalar(Connection c, String sql) throws SQLException {
        try (var s = c.createStatement(); var rs = s.executeQuery(sql)) { return rs.next() ? rs.getInt(1) : 0; }
    }

    public State state() throws SQLException {
        try (var c = repository.open(); var s = c.prepareStatement("SELECT * FROM tmdb_import_state WHERE id=1"); var rs = s.executeQuery()) {
            if (!rs.next()) throw new SQLException("Missing import state");
            return new State(rs.getInt("target"), rs.getInt("next_page"), rs.getInt("total_pages"),
                scalar(c, "SELECT COUNT(*) FROM tmdb_candidate WHERE status='ACCEPTED'"),
                scalar(c, "SELECT COUNT(*) FROM tmdb_candidate WHERE status<>'PENDING'"),
                rs.getInt("requests"), Instant.parse(rs.getString("created_at")));
        }
    }

    public void requireFresh() throws SQLException {
        if (!clock.instant().isBefore(state().expiresAt()))
            throw new SQLException("Catalog cache expired; refresh into a new approved database before use");
    }

    public OptionalLong pending() throws SQLException {
        try (var c = repository.open(); var s = c.prepareStatement("SELECT id FROM tmdb_candidate WHERE status='PENDING' ORDER BY ordinal LIMIT 1"); var rs = s.executeQuery()) {
            return rs.next() ? OptionalLong.of(rs.getLong(1)) : OptionalLong.empty();
        }
    }

    public void recordRequest() throws SQLException {
        try (var c = repository.open(); var s = c.prepareStatement("UPDATE tmdb_import_state SET requests=requests+1 WHERE id=1")) { s.executeUpdate(); }
    }

    public void addPage(TmdbCatalogSource.Page page) throws SQLException {
        if (page.page() != state().nextPage() || page.page() > 100 || page.totalPages() < page.page() || page.ids().size() > 100)
            throw new IllegalArgumentException("Invalid popular page");
        try (var c = repository.open()) {
            c.setAutoCommit(false);
            try {
                try (var s = c.prepareStatement("INSERT INTO tmdb_candidate VALUES(?,?,'PENDING') ON CONFLICT(id) DO NOTHING")) {
                    int position = (page.page() - 1) * 100;
                    for (long id : page.ids()) {
                        if (id <= 0) throw new IllegalArgumentException("Invalid TV ID");
                        s.setLong(1, id); s.setInt(2, ++position); s.executeUpdate();
                    }
                }
                try (var s = c.prepareStatement("UPDATE tmdb_import_state SET next_page=?,total_pages=? WHERE id=1")) {
                    s.setInt(1, page.page() + 1); s.setInt(2, Math.min(100, page.totalPages())); s.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }

    public void finish(long id, Optional<TmdbCatalogSource.Basic> basic) throws SQLException {
        try (var c = repository.open()) {
            c.setAutoCommit(false);
            try {
                try (var s = c.prepareStatement("SELECT status FROM tmdb_candidate WHERE id=?")) {
                    s.setLong(1, id);
                    try (var rs = s.executeQuery()) {
                        if (!rs.next() || !"PENDING".equals(rs.getString(1))) throw new SQLException("Candidate is not pending");
                    }
                }
                if (basic.isPresent()) {
                    if (scalar(c, "SELECT COUNT(*) FROM tmdb_basic") >= state().target()) throw new SQLException("Catalog target reached");
                    var b = basic.get();
                    var series = b.series();
                    if (series.id() != id || !("tmdb:tv:" + id).equals(series.externalId())
                            || series.originalTitle() == null || series.originalTitle().isBlank()
                            || series.originCountry() == null || !series.originCountry().matches("(?:[A-Z]{2}(?:,[A-Z]{2})*)?")
                            || b.displayTitle() == null || b.displayTitle().isBlank()
                            || b.originalLanguage() == null || !b.originalLanguage().matches("[a-z]{2,3}")
                            || b.firstAirDate() == null)
                        throw new IllegalArgumentException("Invalid basic TV identity");
                    if (!b.firstAirDate().isEmpty()) LocalDate.parse(b.firstAirDate());
                    String source = "https://www.themoviedb.org/tv/" + id;
                    String evidence = "TMDB TV details; " + source + "; checked " + clock.instant();
                    repository.saveSeries(c, series, true);
                    repository.saveAlias(c, new SeriesAlias(id, b.originalLanguage(), series.originalTitle(), true), evidence);
                    repository.saveAlias(c, new SeriesAlias(id, "en", b.displayTitle(), true), evidence);
                    String note = "TMDB 影集基本資料快取；原名：" + series.originalTitle()
                        + "；製作國家／地區：" + (series.originCountry().isBlank() ? "未提供" : series.originCountry())
                        + "；首播日期：" + (b.firstAirDate().isBlank() ? "未提供" : b.firstAirDate())
                        + "。非影評或即時網頁結果。";
                    repository.saveDocument(c, new MetadataDocument("tmdb-tv-" + id, id, source, b.displayTitle(),
                        note, ResultCategory.SERIES_INFORMATION, evidence,
                        "TMDB API cache; account-approved use required; expire within six calendar months"));
                    try (var s = c.prepareStatement("INSERT INTO tmdb_basic VALUES(?,?,?,?,?)")) {
                        s.setLong(1, id); s.setString(2, b.displayTitle()); s.setString(3, b.originalLanguage());
                        s.setString(4, b.firstAirDate()); s.setString(5, clock.instant().toString()); s.executeUpdate();
                    }
                }
                try (var s = c.prepareStatement("UPDATE tmdb_candidate SET status=? WHERE id=?")) {
                    s.setString(1, basic.isPresent() ? "ACCEPTED" : "REJECTED"); s.setLong(2, id); s.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }

    public SqliteMetadataRepository.IndexSnapshot snapshot() throws SQLException {
        requireFresh();
        var snapshot = repository.loadIndexSnapshot();
        if (snapshot.series().size() != state().accepted() || snapshot.documents().size() != state().accepted())
            throw new SQLException("Catalog consistency check failed");
        return snapshot;
    }
}
