package com.group12.tvsearch.sources;

import com.group12.tvsearch.model.Series;
import java.util.Optional;

/** Planned metadata resolver; implementation requires valid TMDB access. */
public interface TmdbClient {
    Optional<Series> resolveSeries(String title);
}
