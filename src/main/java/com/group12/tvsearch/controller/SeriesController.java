package com.group12.tvsearch.controller;

import com.group12.tvsearch.sources.TmdbAccessException;
import com.group12.tvsearch.sources.TmdbClient;
import com.group12.tvsearch.sources.VerifiedSeriesResolver;
import com.group12.tvsearch.model.Series;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.concurrent.Semaphore;

/** Metadata is separate from ranked webpage results and is never persisted here. */
@RestController
public class SeriesController {
    private final java.util.function.Function<String, java.util.Optional<Series>> resolver;
    private final boolean enabled;
    private final Semaphore inFlight = new Semaphore(1);

    @Autowired
    public SeriesController(VerifiedSeriesResolver resolver,
            @Value("${metadata.tmdb.enabled:false}") boolean enabled) {
        this.resolver = resolver::resolve;
        this.enabled = enabled;
    }

    /** Standalone endpoint tests can still supply a controlled client. */
    public SeriesController(TmdbClient client, boolean enabled) {
        this.resolver = client::resolveSeries;
        this.enabled = enabled;
    }

    public record SeriesResponse(String status, String title, String originCountry, String url) {}

    @GetMapping("/api/series")
    public SeriesResponse resolve(@RequestParam(defaultValue = "") String q) {
        if (q.isBlank()) return state("EMPTY_QUERY");
        if (q.length() > 300) return state("QUERY_TOO_LONG");
        if (!enabled) return state("DISABLED");
        if (!inFlight.tryAcquire()) return state("BUSY");
        try {
            return resolver.apply(q).map(series -> new SeriesResponse("FOUND",
                    series.originalTitle(), series.originCountry(),
                    "https://www.themoviedb.org/tv/" + series.id()))
                    .orElseGet(() -> state("UNCONFIRMED"));
        } catch (TmdbAccessException exception) {
            return state(exception.reason().name());
        } finally {
            inFlight.release();
        }
    }

    private static SeriesResponse state(String status) {
        return new SeriesResponse(status, null, null, null);
    }
}
