package com.group12.tvsearch.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Offline stand-in for the Brave Web Search API. Responses use the Brave JSON shape, but every
 * result is project-authored fake data on reserved .example hosts. No network request is made.
 */
public final class FixtureBraveTransport implements BraveTransport {
    public static final String RESOURCE = "data/brave-fixture.json";
    private final Map<String, String> responses;
    private final java.util.List<String> sampleQueries;

    public FixtureBraveTransport(String json) {
        try {
            JsonNode root = new ObjectMapper().readTree(json);
            JsonNode queries = root.path("queries");
            if (!queries.isObject()) throw new IllegalArgumentException("Fixture needs a queries object");
            Map<String, String> loaded = new HashMap<>();
            var it = queries.fields();
            while (it.hasNext()) {
                var entry = it.next();
                loaded.put(entry.getKey(), entry.getValue().toString());
            }
            responses = Map.copyOf(loaded);
            java.util.List<String> samples = new java.util.ArrayList<>();
            root.path("sampleQueries").forEach(n -> samples.add(n.asText()));
            sampleQueries = java.util.List.copyOf(samples);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid Brave fixture JSON", e);
        }
    }

    public static FixtureBraveTransport fromClasspath() {
        try (InputStream in = FixtureBraveTransport.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("Missing " + RESOURCE);
            return new FixtureBraveTransport(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Keys are matched against the already normalized (NFKC, lower-case, single-space) query. */
    @Override
    public Optional<String> fetch(String normalizedQuery, int count) {
        if (normalizedQuery == null) return Optional.empty();
        return Optional.ofNullable(responses.get(normalizedQuery));
    }

    public java.util.List<String> sampleQueries() {
        return sampleQueries;
    }
}
