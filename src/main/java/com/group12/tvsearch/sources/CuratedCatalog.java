package com.group12.tvsearch.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import com.group12.tvsearch.model.Series;
import com.group12.tvsearch.model.SeriesAlias;
import com.group12.tvsearch.model.AliasEvidence;
import com.group12.tvsearch.model.MetadataDocument;
import com.group12.tvsearch.model.ResultCategory;

/** Manually checked title facts/links and project-authored summaries, not copied article bodies. */
public record CuratedCatalog(String checkedOn, List<Entry> series) {
    public CuratedCatalog {
        LocalDate.parse(checkedOn);
        series = List.copyOf(series);
        Set<Long> ids = new HashSet<>();
        Set<String> urls = new HashSet<>();
        for (Entry entry : series) {
            if (entry.id() <= 0 || !ids.add(entry.id()) || entry.originalTitle().isBlank()
                    || !entry.country().matches("[A-Z]{2}") || entry.summary().isBlank())
                throw new IllegalArgumentException("Invalid curated series identity");
            Set<String> locales = new HashSet<>();
            for (Alias alias : entry.aliases()) {
                URI url = URI.create(alias.url());
                if (alias.title().isBlank() || !locales.add(alias.locale()) || !urls.add(alias.url())
                        || !"https".equals(url.getScheme()) || url.getUserInfo() != null
                        || !Set.of("en.wikipedia.org", "zh.wikipedia.org", "ja.wikipedia.org", "ko.wikipedia.org").contains(url.getHost()))
                    throw new IllegalArgumentException("Invalid curated title evidence URL");
            }
            if (!locales.equals(Set.of("en", "zh-Hant", "zh-Hans", "ja", "ko")))
                throw new IllegalArgumentException("Five evidenced title locales are required");
        }
    }
    public record Entry(long id, String originalTitle, String country, String summary, List<Alias> aliases) {
        public Entry { aliases = List.copyOf(aliases); }
    }
    public record Alias(String locale, String title, String url) {}

    public SqliteMetadataRepository.IndexSnapshot snapshot() {
        var identities = new ArrayList<Series>();
        var aliases = new ArrayList<AliasEvidence>();
        var documents = new ArrayList<MetadataDocument>();
        for (var entry : series) {
            identities.add(new Series(entry.id(), entry.originalTitle(), entry.country(), null));
            for (var alias : entry.aliases()) {
                String evidence = alias.url() + " | manually checked " + checkedOn;
                aliases.add(new AliasEvidence(new SeriesAlias(entry.id(), alias.locale(), alias.title(), true), evidence));
                documents.add(new MetadataDocument("curated-" + entry.id() + "-" + alias.locale(), entry.id(), alias.url(),
                    alias.title() + " — Wikipedia (" + alias.locale() + ")",
                    entry.summary() + " 連結與名稱核對日期：" + checkedOn + "；不是即時搜尋。",
                    ResultCategory.SERIES_INFORMATION, evidence,
                    "Project-authored summary/title facts; linked Wikipedia article retains its own license"));
            }
        }
        identities.sort(Comparator.comparingLong(Series::id));
        aliases.sort(Comparator.comparingLong((AliasEvidence a) -> a.alias().seriesId()).thenComparing(a -> a.alias().locale()));
        documents.sort(Comparator.comparing(MetadataDocument::id));
        return new SqliteMetadataRepository.IndexSnapshot(List.copyOf(identities), List.copyOf(aliases), List.copyOf(documents));
    }

    public static CuratedCatalog load() throws IOException {
        try (var stream = CuratedCatalog.class.getResourceAsStream("/data/curated-series.json")) {
            if (stream == null) throw new IOException("Missing curated catalog");
            return new ObjectMapper().readValue(stream, CuratedCatalog.class);
        }
    }
}
