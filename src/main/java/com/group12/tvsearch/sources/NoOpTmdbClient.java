package com.group12.tvsearch.sources;

import com.group12.tvsearch.model.Series;
import java.util.Optional;

public class NoOpTmdbClient implements TmdbClient {
    @Override public Optional<Series> resolveSeries(String title) { return Optional.empty(); }
}
