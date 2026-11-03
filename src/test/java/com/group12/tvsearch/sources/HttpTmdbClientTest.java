package com.group12.tvsearch.sources;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class HttpTmdbClientTest {
    private HttpServer server;
    private URI base;
    private int status = 200;
    private String searchBody;
    private String detailBody;
    private long delayMillis;
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicReference<String> lastQuery = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String TEST_TOKEN = "synthetic-test-token-not-a-real-key";

    @BeforeEach void start() throws Exception {
        detailBody = """
            {"id":123,"original_name":"Fictional original","type":"Scripted",
             "genres":[{"id":18,"name":"Drama"}],"origin_country":["US"]}
            """;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/3/", exchange -> {
            requests.incrementAndGet();
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastQuery.set(exchange.getRequestURI().getRawQuery());
            String body;
            if (searchBody != null) body = searchBody;
            else {
                String query = exchange.getRequestURI().getRawQuery();
                String title = query == null ? "" : URLDecoder.decode(query.substring(query.indexOf("query=") + 6), StandardCharsets.UTF_8);
                body = mapper.writeValueAsString(Map.of("results", List.of(Map.of("id",123,"name",title)), "total_pages",1));
            }
            if (exchange.getRequestURI().getPath().startsWith("/3/tv/")) body = detailBody;
            if (delayMillis > 0) {
                try { Thread.sleep(delayMillis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            try {
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } finally { exchange.close(); }
        });
        server.start();
        base = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/3/");
    }
    @AfterEach void stop() { server.stop(0); }
    private HttpTmdbClient client(Duration timeout) {
        return new HttpTmdbClient(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(), base, TEST_TOKEN, timeout);
    }
    private HttpTmdbClient client() { return client(Duration.ofSeconds(2)); }

    @ParameterizedTest
    @ValueSource(strings = {"Harbor Lights", "港灣燈火", "港湾灯火", "ハーバーライト", "항구의 빛"})
    void languageDoesNotRestrictOriginCountry(String title) {
        var series = client().resolveSeries(title).orElseThrow();
        assertEquals(123, series.id());
        assertEquals("tmdb:tv:123", series.externalId());
        assertEquals("US", series.originCountry());
        assertEquals("Fictional original", series.originalTitle());
        assertEquals(2, requests.get());
        assertEquals("Bearer " + TEST_TOKEN, authorization.get());
        assertFalse(lastQuery.get().contains(TEST_TOKEN));
    }

    @Test void emptyQueryAndNoOpDoNotUseNetwork() {
        assertTrue(client().resolveSeries(" ").isEmpty());
        assertTrue(client().resolveSeries(null).isEmpty());
        assertTrue(new NoOpTmdbClient().resolveSeries("any").isEmpty());
        assertEquals(0, requests.get());
    }

    @Test void queryIsEncodedAndTokenNeverAppearsInUrl() {
        // Search returns an exact match for the decoded query even with query syntax characters.
        assertTrue(client().resolveSeries("港灣 & garden? a=1").isPresent());
        assertEquals(2, requests.get());
        assertEquals("language=en-US", lastQuery.get());
    }

    @ParameterizedTest
    @CsvSource({"401,AUTHENTICATION", "403,AUTHENTICATION", "429,RATE_LIMITED", "500,HTTP_ERROR", "302,HTTP_ERROR"})
    void mapsHttpErrorsWithoutRetryOrLeakingResponse(int httpStatus, String reason) {
        status = httpStatus; searchBody = "Credentials: " + TEST_TOKEN;
        var error = assertThrows(TmdbAccessException.class, () -> client().resolveSeries("Harbor"));
        assertEquals(TmdbAccessException.Reason.valueOf(reason), error.reason());
        assertFalse(error.toString().contains(TEST_TOKEN));
        assertNull(error.getCause());
        assertEquals(1, requests.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not json", "null", "[]", "{}", "{\"results\":null}", "{\"results\":[{\"name\":\"Harbor\",\"id\":-1}]}",
        "{\"results\":[{\"name\":\"Harbor\",\"id\":123.5}]}", "{\"results\":[null]}", "{\"results\":[{\"name\":123,\"id\":1}]}"})
    void rejectsMalformedResponses(String body) {
        searchBody = body;
        var error = assertThrows(TmdbAccessException.class, () -> client().resolveSeries("Harbor"));
        assertEquals(TmdbAccessException.Reason.MALFORMED_RESPONSE, error.reason());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"results\":[]}", "{\"results\":[{\"id\":1,\"name\":\"Different title\"}]}",
        "{\"results\":[{\"id\":1,\"name\":\"Harbor\"},{\"id\":2,\"name\":\"Harbor\"}]}",
        "{\"results\":[{\"id\":1,\"name\":\"Harbor\"}],\"total_pages\":2}"})
    void noMatchOrAmbiguityDoesNotGuessIdentity(String body) {
        searchBody = body;
        assertTrue(client().resolveSeries("Harbor").isEmpty());
        assertEquals(1, requests.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Animation", "Documentary", "Reality", "Talk", "News"})
    void rejectsOutOfScopeGenres(String genre) {
        detailBody = detailBody.replace("Drama", genre);
        assertTrue(client().resolveSeries("Harbor").isEmpty());
    }

    @Test void rejectsNonScriptedMissingGenresAndWrongIdentity() {
        String original = detailBody;
        detailBody = original.replace("Scripted", "Reality");
        assertTrue(client().resolveSeries("Harbor").isEmpty());
        detailBody = original.replace("[{\"id\":18,\"name\":\"Drama\"}]", "[]");
        assertTrue(client().resolveSeries("Harbor").isEmpty());
        detailBody = original.replace("123", "456");
        assertEquals(TmdbAccessException.Reason.MALFORMED_RESPONSE,
            assertThrows(TmdbAccessException.class, () -> client().resolveSeries("Harbor")).reason());
    }

    @Test void timesOutWithinBoundAndDoesNotRetry() {
        delayMillis = 800;
        assertEquals(TmdbAccessException.Reason.TIMEOUT,
            assertThrows(TmdbAccessException.class, () -> client(Duration.ofMillis(100)).resolveSeries("Harbor")).reason());
        assertEquals(1, requests.get());
    }

    @Test void connectionFailureHasSafeTypedError() {
        server.stop(0);
        assertEquals(TmdbAccessException.Reason.NETWORK,
            assertThrows(TmdbAccessException.class, () -> client().resolveSeries("Harbor")).reason());
    }

    @Test void rejectsMissingTokenAndInvalidTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new HttpTmdbClient(""));
        assertThrows(IllegalArgumentException.class, () -> new HttpTmdbClient("bad\nheader"));
        assertThrows(IllegalArgumentException.class, () -> new HttpTmdbClient("secret\u0000control"));
        assertThrows(IllegalArgumentException.class, () -> new HttpTmdbClient(HttpClient.newHttpClient(), base, TEST_TOKEN, Duration.ZERO));
    }
    @Test void acceptsScriptedMiniseries() {
        detailBody = detailBody.replace("Scripted", "Miniseries");
        assertTrue(client().resolveSeries("Harbor").isPresent());
    }
    @ParameterizedTest
    @ValueSource(strings = {"fractional-id", "numeric-title", "invalid-country"})
    void rejectsMalformedDetailIdentityFields(String malformed) {
        if (malformed.equals("fractional-id")) detailBody = detailBody.replace("\"id\":123", "\"id\":123.5");
        if (malformed.equals("numeric-title")) detailBody = detailBody.replace("\"Fictional original\"", "123");
        if (malformed.equals("invalid-country")) detailBody = detailBody.replace("\"US\"", "\"not-a-country\"");
        assertEquals(TmdbAccessException.Reason.MALFORMED_RESPONSE,
            assertThrows(TmdbAccessException.class, () -> client().resolveSeries("Harbor")).reason());
    }
}
