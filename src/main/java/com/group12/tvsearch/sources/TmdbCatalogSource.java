package com.group12.tvsearch.sources;

import com.group12.tvsearch.model.Series;
import java.util.List;
import java.util.Optional;

/** Minimal fields only; no posters, synopses, reviews or translated alias downloads. */
public interface TmdbCatalogSource {
    record Page(int page, int totalPages, List<Long> ids) {
        public Page { ids = List.copyOf(ids); }
    }
    record Basic(Series series, String displayTitle, String originalLanguage, String firstAirDate) {}
    Page popular(int page);
    Optional<Basic> basic(long id);
}
