package com.group12.tvsearch.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group12.tvsearch.model.Series;
import com.group12.tvsearch.query.QueryProcessor;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/** Conservative exact-title resolver: TV search then scope-checked details, at most two requests. */
public class HttpTmdbClient implements TmdbClient {
    private final HttpClient http;
    private final URI base;
    private final String token;
    private final Duration timeout;
    private final ObjectMapper mapper = new ObjectMapper();
    private final QueryProcessor processor = new QueryProcessor();

    public HttpTmdbClient(String readAccessToken) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER).build(), URI.create("https://api.themoviedb.org/3/"),
            readAccessToken, Duration.ofSeconds(5));
    }

    /** Alternate base is for controlled local transport tests, never a user-supplied URL. */
    public HttpTmdbClient(HttpClient http, URI base, String token, Duration timeout) {
        if (token == null || !token.matches("[A-Za-z0-9._~+/=-]+"))
            throw new IllegalArgumentException("A TMDB read access token is required");
        if (timeout == null || timeout.isNegative() || timeout.isZero())
            throw new IllegalArgumentException("A positive request timeout is required");
        if (base == null || !base.isAbsolute() || !base.toString().endsWith("/")
                || !("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getRawQuery() != null || base.getRawFragment() != null)
            throw new IllegalArgumentException("A fixed absolute API base URL is required");
        this.http = java.util.Objects.requireNonNull(http);
        this.base = base; this.token = token; this.timeout = timeout;
    }

    @Override public Optional<Series> resolveSeries(String title) {
        String query = processor.normalize(title);
        if (query.isBlank()) return Optional.empty();
        JsonNode search = get("search/tv?include_adult=false&page=1&language=en-US&query="
            + URLEncoder.encode(query, StandardCharsets.UTF_8));
        JsonNode rows = search.path("results");
        if (!rows.isArray()) throw failure(TmdbAccessException.Reason.MALFORMED_RESPONSE);
        Set<Long> exactIds = new LinkedHashSet<>();
        for (JsonNode row : rows) {
            if (query.equals(processor.normalize(row.path("name").asText("")))
                    || query.equals(processor.normalize(row.path("original_name").asText("")))) {
                if (!row.path("id").canConvertToLong() || row.path("id").asLong() <= 0)
                    throw failure(TmdbAccessException.Reason.MALFORMED_RESPONSE);
                exactIds.add(row.path("id").asLong());
            }
        }
        // A remake/title collision or truncated result set is not a verified identity.
        if (exactIds.size() != 1 || search.path("total_pages").asInt(1) > 1) return Optional.empty();
        long id = exactIds.iterator().next();
        JsonNode details = get("tv/" + id + "?language=en-US");
        if (details.path("id").asLong(-1) != id)
            throw failure(TmdbAccessException.Reason.MALFORMED_RESPONSE);
        if (!Set.of("Scripted", "Miniseries").contains(details.path("type").asText())) return Optional.empty();
        JsonNode genres = details.path("genres");
        if (!genres.isArray() || genres.isEmpty()) return Optional.empty();
        for (JsonNode genre : genres) {
            String name = genre.path("name").asText("");
            if (name.isBlank()) return Optional.empty();
            if (name.equalsIgnoreCase("Animation") || name.equalsIgnoreCase("Documentary")
                    || name.equalsIgnoreCase("Reality") || name.equalsIgnoreCase("Talk")
                    || name.equalsIgnoreCase("News")) return Optional.empty();
        }
        String originalTitle = details.path("original_name").asText("");
        if (originalTitle.isBlank()) throw failure(TmdbAccessException.Reason.MALFORMED_RESPONSE);
        JsonNode countries = details.path("origin_country");
        if (!countries.isArray()) throw failure(TmdbAccessException.Reason.MALFORMED_RESPONSE);
        Set<String> origins = new LinkedHashSet<>();
        for (JsonNode country : countries) {
            if (!country.isTextual()) throw failure(TmdbAccessException.Reason.MALFORMED_RESPONSE);
            origins.add(country.asText());
        }
        return Optional.of(new Series(id, originalTitle, String.join(",", origins), "tmdb:tv:" + id));
    }

    private JsonNode get(String relativeUrl) {
        HttpRequest request = HttpRequest.newBuilder(base.resolve(relativeUrl)).timeout(timeout)
            .header("Authorization", "Bearer " + token).header("Accept", "application/json").GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 401 || response.statusCode() == 403)
                throw failure(TmdbAccessException.Reason.AUTHENTICATION);
            if (response.statusCode() == 429) throw failure(TmdbAccessException.Reason.RATE_LIMITED);
            if (response.statusCode() != 200) throw failure(TmdbAccessException.Reason.HTTP_ERROR);
            if (response.body().length() > 1_000_000) throw failure(TmdbAccessException.Reason.MALFORMED_RESPONSE);
            JsonNode json = mapper.readTree(response.body());
            if (json == null || !json.isObject()) throw failure(TmdbAccessException.Reason.MALFORMED_RESPONSE);
            return json;
        } catch (HttpTimeoutException e) {
            throw failure(TmdbAccessException.Reason.TIMEOUT);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw failure(TmdbAccessException.Reason.MALFORMED_RESPONSE);
        } catch (IOException e) {
            throw failure(TmdbAccessException.Reason.NETWORK);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure(TmdbAccessException.Reason.INTERRUPTED);
        }
    }

    private static TmdbAccessException failure(TmdbAccessException.Reason reason) {
        return new TmdbAccessException(reason);
    }
}
