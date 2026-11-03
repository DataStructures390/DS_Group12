package com.group12.tvsearch.sources;

import com.group12.tvsearch.model.MetadataDocument;
import com.group12.tvsearch.model.AliasEvidence;
import com.group12.tvsearch.model.ResultCategory;
import com.group12.tvsearch.model.Series;
import com.group12.tvsearch.model.SeriesAlias;
import com.group12.tvsearch.query.QueryProcessor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Explicitly constructed persistence layer, not an automatic source or Spring ingestion job. */
public class SqliteMetadataRepository {
    private final String jdbcUrl;
    private final QueryProcessor processor = new QueryProcessor();

    public SqliteMetadataRepository(Path database) throws SQLException, IOException {
        Path absolute = database.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        jdbcUrl = "jdbc:sqlite:" + absolute;
        migrate();
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 3000");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    private void migrate() throws SQLException, IOException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                int version;
                try (ResultSet rs = statement.executeQuery("PRAGMA user_version")) {
                    if (!rs.next()) throw new SQLException("Unable to read schema version");
                    version = rs.getInt(1);
                }
                if (version > 1) throw new SQLException("Database schema is newer than this application");
                if (version == 0) {
                    try (var stream = SqliteMetadataRepository.class.getResourceAsStream("/db/V1__metadata.sql")) {
                        if (stream == null) throw new IOException("Missing V1 migration");
                        String migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                        // V1 is our own fixed DDL, without procedural SQL or semicolons in literals.
                        for (String sql : migration.split(";")) {
                            if (!sql.isBlank()) statement.execute(sql);
                        }
                    }
                    statement.execute("PRAGMA user_version = 1");
                }
                connection.commit();
            } catch (SQLException | IOException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public void saveSeries(Series series, boolean confirmedLiveActionScripted) throws SQLException {
        try (Connection connection = open()) {
            saveSeries(connection, series, confirmedLiveActionScripted);
        }
    }

    private void saveSeries(Connection connection, Series series, boolean confirmedLiveActionScripted) throws SQLException {
        Objects.requireNonNull(series);
        if (!confirmedLiveActionScripted || series.id() <= 0 || series.originalTitle() == null
                || series.originalTitle().isBlank() || series.originCountry() == null) {
            throw new IllegalArgumentException("Confirmed live-action scripted series identity required");
        }
        try (var ps = connection.prepareStatement("""
                INSERT INTO series(id, original_title, origin_country, external_id, scope)
                VALUES (?, ?, ?, ?, 'LIVE_ACTION_SCRIPTED')
                ON CONFLICT(id) DO UPDATE SET original_title=excluded.original_title,
                  origin_country=excluded.origin_country, external_id=excluded.external_id
                """)) {
            ps.setLong(1, series.id()); ps.setString(2, series.originalTitle());
            ps.setString(3, series.originCountry()); ps.setString(4, series.externalId());
            ps.executeUpdate();
        }
    }

    public Optional<Series> findSeries(long id) throws SQLException {
        try (Connection connection = open(); var ps = connection.prepareStatement("SELECT * FROM series WHERE id=?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(readSeries(rs)) : Optional.empty();
            }
        }
    }

    public void saveAlias(SeriesAlias alias, String evidence) throws SQLException {
        try (Connection connection = open()) {
            saveAlias(connection, alias, evidence);
        }
    }

    private void saveAlias(Connection connection, SeriesAlias alias, String evidence) throws SQLException {
        Objects.requireNonNull(alias);
        String normalized = processor.normalize(alias.title());
        if (normalized.isBlank() || alias.locale() == null || alias.locale().isBlank()
                || (alias.verified() && (evidence == null || evidence.isBlank()))) {
            throw new IllegalArgumentException("Alias title, locale and verification evidence required");
        }
        try (var ps = connection.prepareStatement("""
                INSERT INTO series_alias(series_id, locale, title, normalized_title, verified, evidence)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(series_id, locale, normalized_title) DO UPDATE SET
                  title=excluded.title, verified=excluded.verified, evidence=excluded.evidence
                """)) {
            ps.setLong(1, alias.seriesId()); ps.setString(2, alias.locale());
            ps.setString(3, alias.title()); ps.setString(4, normalized);
            ps.setBoolean(5, alias.verified()); ps.setString(6, evidence == null ? "" : evidence);
            ps.executeUpdate();
        }
    }

    /** Return ALL matching identities; never silently choose one on a title collision. */
    public List<Series> findVerifiedAliasCandidates(String title) throws SQLException {
        try (Connection connection = open(); var ps = connection.prepareStatement("""
                SELECT DISTINCT s.* FROM series s JOIN series_alias a ON a.series_id=s.id
                WHERE a.normalized_title=? AND a.verified=1 ORDER BY s.id
                """)) {
            ps.setString(1, processor.normalize(title));
            List<Series> matches = new ArrayList<>();
            try (var rs = ps.executeQuery()) {
                while (rs.next()) matches.add(readSeries(rs));
            }
            return List.copyOf(matches);
        }
    }

    public void saveDocument(MetadataDocument document) throws SQLException {
        try (Connection connection = open()) {
            saveDocument(connection, document);
        }
    }

    private void saveDocument(Connection connection, MetadataDocument document) throws SQLException {
        Objects.requireNonNull(document);
        try (var ps = connection.prepareStatement("""
                INSERT INTO document(id,series_id,url,title,body,category,provenance,license)
                VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET
                  series_id=excluded.series_id,url=excluded.url,title=excluded.title,
                  body=excluded.body,category=excluded.category,provenance=excluded.provenance,license=excluded.license
                """)) {
            ps.setString(1, document.id()); ps.setLong(2, document.seriesId());
            ps.setString(3, document.url()); ps.setString(4, document.title());
            ps.setString(5, document.body()); ps.setString(6, document.category().name());
            ps.setString(7, document.provenance()); ps.setString(8, document.license());
            ps.executeUpdate();
        }
    }

    public List<MetadataDocument> findDocuments() throws SQLException {
        try (Connection connection = open(); var ps = connection.prepareStatement("SELECT * FROM document ORDER BY id");
                var rs = ps.executeQuery()) {
            List<MetadataDocument> documents = new ArrayList<>();
            while (rs.next()) documents.add(new MetadataDocument(rs.getString("id"), rs.getLong("series_id"),
                rs.getString("url"), rs.getString("title"), rs.getString("body"),
                ResultCategory.valueOf(rs.getString("category")), rs.getString("provenance"), rs.getString("license")));
            return List.copyOf(documents);
        }
    }

    /** Consistent read transaction for rebuilding the in-memory index. */
    public IndexSnapshot loadIndexSnapshot() throws SQLException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                List<Series> series = new ArrayList<>();
                try (var ps = connection.prepareStatement("SELECT * FROM series ORDER BY id"); var rs = ps.executeQuery()) {
                    while (rs.next()) series.add(readSeries(rs));
                }
                List<AliasEvidence> aliases = new ArrayList<>();
                try (var ps = connection.prepareStatement("SELECT * FROM series_alias WHERE verified=1 ORDER BY series_id,locale,title");
                        var rs = ps.executeQuery()) {
                    while (rs.next()) aliases.add(new AliasEvidence(new SeriesAlias(rs.getLong("series_id"),
                        rs.getString("locale"), rs.getString("title"), true), rs.getString("evidence")));
                }
                List<MetadataDocument> documents = new ArrayList<>();
                try (var ps = connection.prepareStatement("SELECT * FROM document ORDER BY id"); var rs = ps.executeQuery()) {
                    while (rs.next()) documents.add(new MetadataDocument(rs.getString("id"), rs.getLong("series_id"),
                        rs.getString("url"), rs.getString("title"), rs.getString("body"),
                        ResultCategory.valueOf(rs.getString("category")), rs.getString("provenance"), rs.getString("license")));
                }
                connection.commit();
                return new IndexSnapshot(List.copyOf(series), List.copyOf(aliases), List.copyOf(documents));
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public record IndexSnapshot(List<Series> series, List<AliasEvidence> aliases,
                                List<MetadataDocument> documents) {}

    /** One-time catalog import into an EMPTY database; never overwrite existing contributor data. */
    public void seedCuratedCatalog(CuratedCatalog catalog) throws SQLException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                try (var ps = connection.prepareStatement("SELECT COUNT(*) FROM series"); var rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) != 0) throw new SQLException("Curated import requires an empty database");
                }
                var snapshot = catalog.snapshot();
                for (var series : snapshot.series()) saveSeries(connection, series, true);
                for (var alias : snapshot.aliases()) saveAlias(connection, alias.alias(), alias.evidence());
                for (var document : snapshot.documents()) saveDocument(connection, document);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /** Repeatable self-authored seed, isolated from all real-world metadata. */
    public void seedLocalDemo(LocalDocumentRepository fixtures) throws SQLException {
        try (Connection connection = open()) {
            connection.setAutoCommit(false);
            try {
                // Seeding is deliberately restricted to a dedicated demo database.
                try (var ps = connection.prepareStatement("SELECT COUNT(*) FROM series WHERE origin_country <> 'DEMO'");
                        var rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) throw new SQLException("Refusing to seed demo into a non-demo database");
                }
                for (var d : fixtures.findAll()) {
                    saveSeries(connection, new Series(d.seriesId(), d.seriesTitle(), "DEMO", null), true);
                    for (String title : d.aliases()) {
                        saveAlias(connection, new SeriesAlias(d.seriesId(), "demo", title, true),
                                "Self-authored fictional fixture: " + d.url());
                    }
                    saveDocument(connection, new MetadataDocument(d.id(), d.seriesId(), d.url(), d.title(), d.body(),
                        d.category(), "Self-authored fictional fixture", "Project-owned original demo content"));
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static Series readSeries(ResultSet rs) throws SQLException {
        return new Series(rs.getLong("id"), rs.getString("original_title"),
                rs.getString("origin_country"), rs.getString("external_id"));
    }
}
