package com.group12.tvsearch.sources;

import com.group12.tvsearch.model.Series;
import java.util.Optional;

/** Optional exact identity resolver. Empty means unconfirmed; transport errors are typed. */
public interface TmdbClient {
    Optional<Series> resolveSeries(String title);
}
