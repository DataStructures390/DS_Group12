package com.group12.tvsearch.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group12.tvsearch.model.Series;
import com.group12.tvsearch.query.QueryProcessor;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Exact evidenced aliases select a checked TV ID; live details must still confirm scope and identity. */
@Service
public class VerifiedSeriesResolver {
    public record Identity(long localSeriesId, long tmdbId, String source) {}
    public record IdentityCatalog(String checkedOn, List<Identity> identities) {}
    private final TmdbClient client;
    private final QueryProcessor processor = new QueryProcessor();
    private final Map<String, Set<Long>> aliases = new HashMap<>();
    private final Map<Long, CuratedCatalog.Entry> entries = new HashMap<>();
    private final Map<Long, Long> tmdbIds = new HashMap<>();

    @Autowired
    public VerifiedSeriesResolver(TmdbClient client) throws IOException {
        this(client, CuratedCatalog.load(), loadIdentities());
    }

    VerifiedSeriesResolver(TmdbClient client, CuratedCatalog catalog, IdentityCatalog identities) {
        this.client = Objects.requireNonNull(client);
        LocalDate.parse(identities.checkedOn());
        var available = new HashMap<Long, CuratedCatalog.Entry>();
        for (var entry : catalog.series()) available.put(entry.id(), entry);
        var usedTmdbIds = new HashSet<Long>();
        for (var identity : identities.identities()) {
            var entry = available.get(identity.localSeriesId());
            if (entry == null || identity.tmdbId() <= 0 || tmdbIds.containsKey(entry.id())
                    || !usedTmdbIds.add(identity.tmdbId()) || identity.source() == null
                    || !identity.source().matches("https://www\\.themoviedb\\.org/tv/" + identity.tmdbId() + "(?:-[a-z0-9-]+)?"))
                throw new IllegalArgumentException("Invalid verified TV identity catalog");
            entries.put(entry.id(), entry);
            tmdbIds.put(entry.id(), identity.tmdbId());
            addAlias(entry.originalTitle(), entry.id());
            for (var alias : entry.aliases()) addAlias(alias.title(), entry.id());
        }
    }

    private static IdentityCatalog loadIdentities() throws IOException {
        try (var stream = VerifiedSeriesResolver.class.getResourceAsStream("/data/curated-tmdb-identities.json")) {
            if (stream == null) throw new IOException("Missing verified TV identity catalog");
            return new ObjectMapper().readValue(stream, IdentityCatalog.class);
        }
    }

    private void addAlias(String title, long localId) {
        aliases.computeIfAbsent(processor.normalize(title), ignored -> new HashSet<>()).add(localId);
    }

    public Optional<Series> resolve(String query) {
        String normalized = processor.normalize(query);
        if (normalized.isBlank()) return Optional.empty();
        var candidates = aliases.get(normalized);
        if (candidates == null) return client.resolveSeries(query);
        if (candidates.size() != 1) return Optional.empty();
        long localId = candidates.iterator().next();
        var expected = entries.get(localId);
        long tmdbId = tmdbIds.get(localId);
        return client.resolveSeriesById(tmdbId).filter(series ->
                series.id() == tmdbId
                && processor.normalize(expected.originalTitle()).equals(processor.normalize(series.originalTitle()))
                && Arrays.asList(series.originCountry().split(",")).contains(expected.country()));
    }
}
