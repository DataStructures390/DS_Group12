package com.group12.tvsearch.sources;

import static org.junit.jupiter.api.Assertions.*;

import com.group12.tvsearch.model.ResultCategory;
import com.group12.tvsearch.model.SearchResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BraveWebSearchClientTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-16T00:00:00Z"), ZoneOffset.UTC);
    private final BraveResultMapper mapper = new BraveResultMapper(CLOCK);

    private static String response(String... items) {
        return "{\"web\":{\"results\":[" + String.join(",", items) + "]}}";
    }

    private static String item(String title, String url, String description, String pageAge) {
        return "{\"title\":\"" + title + "\",\"url\":\"" + url + "\",\"description\":\"" + description + "\""
            + (pageAge == null ? "" : ",\"page_age\":\"" + pageAge + "\"") + "}";
    }

    @Test void mapsFieldsStripsMarkupAndComputesFeatures() {
        List<SearchResult> results = mapper.map(response(item("<strong>Breaking Bad</strong> 心得 &amp; 討論",
            "https://www.ptt.cc/bbs/x.html", "about <strong>breaking</strong> only", "2026-11-01T00:00:00")),
            "breaking bad", "BRAVE");
        assertEquals(1, results.size());
        SearchResult r = results.get(0);
        assertEquals("Breaking Bad 心得 & 討論", r.title());
        assertEquals("about breaking only", r.snippet());
        assertEquals("BRAVE", r.source());
        assertEquals(ResultCategory.COMMUNITY_DISCUSSION, r.category());
        assertEquals(100, r.keywordRelevance());
        assertEquals(100, r.seriesRelevance());
        assertEquals(70, r.sourceRelevance());
        assertEquals(100, r.freshness());
    }

    @Test void partialCoverageInTitleOnly() {
        SearchResult r = mapper.map(response(item("Breaking news", "https://example.org/a",
            "bad weather", null)), "breaking bad", "BRAVE").get(0);
        assertEquals(100, r.keywordRelevance());
        assertEquals(50, r.seriesRelevance());
        assertEquals(0, r.freshness());
    }

    @Test void classifiesKnownDomainsSubdomainsAndUnknown() {
        assertEquals(ResultCategory.SERIES_INFORMATION, BraveResultMapper.profile("zh.wikipedia.org").category());
        assertEquals(ResultCategory.PROFESSIONAL_REVIEW, BraveResultMapper.profile("www.rottentomatoes.com").category());
        assertEquals(ResultCategory.STREAMING_PLATFORM, BraveResultMapper.profile("WWW.NETFLIX.COM").category());
        assertEquals(ResultCategory.COMMUNITY_DISCUSSION, BraveResultMapper.profile("www.dcard.tw").category());
        assertSame(BraveResultMapper.UNKNOWN, BraveResultMapper.profile("notptt.cc"));
        assertSame(BraveResultMapper.UNKNOWN, BraveResultMapper.profile("ptt.cc.evil.org"));
    }

    @Test void skipsNonHttpRelativeAndUntitledResults() {
        List<SearchResult> results = mapper.map(response(
            item("ftp", "ftp://ptt.cc/a", "", null),
            item("relative", "/local/page", "", null),
            item("script", "javascript:alert(1)", "", null),
            item("", "https://ptt.cc/untitled", "", null),
            item("ok", "http://ptt.cc/ok", "", null)), "ok", "BRAVE");
        assertEquals(List.of("http://ptt.cc/ok"), results.stream().map(SearchResult::url).toList());
    }

    @Test void malformedOrMissingSectionsReturnEmpty() {
        assertTrue(mapper.map("not json", "q", "BRAVE").isEmpty());
        assertTrue(mapper.map("{}", "q", "BRAVE").isEmpty());
        assertTrue(mapper.map("{\"web\":{\"results\":{}}}", "q", "BRAVE").isEmpty());
        assertTrue(mapper.map("", "q", "BRAVE").isEmpty());
    }

    @Test void freshnessDecaysLinearlyAndRejectsFutureOrInvalidDates() {
        assertEquals(100, mapper.freshness("2026-10-17T00:00:00"));      // 30 days
        assertEquals(0, mapper.freshness("2023-11-17T00:00:00Z"));      // 1095 days
        double mid = mapper.freshness("2025-05-17T00:00:00+00:00");     // 548 days
        assertEquals(100.0 * (1095 - 548) / (1095 - 30), mid, 1e-9);
        assertEquals(0, mapper.freshness("2027-01-01T00:00:00"));
        assertEquals(0, mapper.freshness("yesterday"));
        assertEquals(0, mapper.freshness(""));
    }

    @Test void clientBoundsCountAndSurvivesTransportFailure() {
        AtomicInteger requested = new AtomicInteger();
        String many = response(java.util.stream.IntStream.range(0, 30)
            .mapToObj(i -> item("t" + i, "https://ptt.cc/" + i, "", null)).toArray(String[]::new));
        var client = new BraveWebSearchClient((q, count) -> { requested.set(count); return Optional.of(many); },
            mapper, "BRAVE", "notice", List.of());
        assertEquals(20, client.search("t", 50).size());
        assertEquals(20, requested.get());
        assertEquals(1, client.search("t", 0).size());

        var failing = new BraveWebSearchClient((q, c) -> { throw new IllegalStateException("network"); },
            mapper, "BRAVE", "notice", List.of());
        assertTrue(failing.search("t", 5).isEmpty());
        var empty = new BraveWebSearchClient((q, c) -> Optional.empty(), mapper, "BRAVE", "n", List.of());
        assertTrue(empty.search("t", 5).isEmpty());
    }

    @Test void fixtureMatchesNormalizedKeysOnlyAndUsesReservedHosts() {
        FixtureBraveTransport fixture = FixtureBraveTransport.fromClasspath();
        assertTrue(fixture.fetch("breaking bad", 10).isPresent());
        assertTrue(fixture.fetch("絕命毒師", 10).isPresent());
        assertTrue(fixture.fetch("unknown show", 10).isEmpty());
        assertTrue(fixture.fetch(null, 10).isEmpty());
        assertEquals(List.of("Breaking Bad", "絕命毒師", "Squid Game"), fixture.sampleQueries());
        for (String q : List.of("breaking bad", "絕命毒師", "squid game")) {
            for (SearchResult r : mapper.map(fixture.fetch(q, 20).orElseThrow(), q, "BRAVE_FIXTURE")) {
                String host = BraveResultMapper.httpHost(r.url());
                assertTrue(host.toLowerCase().endsWith(".example") || host.equalsIgnoreCase("blog.example"), host);
                assertTrue(r.title().startsWith("[假資料]"), r.title());
            }
        }
    }

    @Test void configurationRejectsUnknownProvider() {
        var config = new WebSearchConfiguration();
        assertInstanceOf(NoOpWebSearchClient.class, config.webSearchClient("none"));
        assertInstanceOf(BraveWebSearchClient.class, config.webSearchClient(" Brave-Fixture "));
        assertThrows(IllegalStateException.class, () -> config.webSearchClient("brave"));
    }
}
