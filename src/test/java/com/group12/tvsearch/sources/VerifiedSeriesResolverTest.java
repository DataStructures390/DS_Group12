package com.group12.tvsearch.sources;

import static org.junit.jupiter.api.Assertions.*;
import com.group12.tvsearch.model.Series;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.group12.tvsearch.controller.SeriesController;

class VerifiedSeriesResolverTest {
    private static final Map<Long, Series> LIVE = Map.of(
            1396L, new Series(1396, "Breaking Bad", "US", "tmdb:tv:1396"),
            66732L, new Series(66732, "Stranger Things", "US", "tmdb:tv:66732"),
            70523L, new Series(70523, "Dark", "DE", "tmdb:tv:70523"));

    @ParameterizedTest
    @CsvSource({
        "Breaking Bad,1396", "絕命毒師,1396", "绝命毒师,1396", "ブレイキング・バッド,1396", "브레이킹 배드,1396",
        "Stranger Things,66732", "怪奇物語,66732", "怪奇物语,66732",
        "ストレンジャー・シングス 未知の世界,66732", "기묘한 이야기,66732",
        "Dark,70523", "闇,70523", "暗黑,70523", "ダーク,70523", "다크,70523",
        "ＢＲＥＡＫＩＮＧ　ＢＡＤ,1396"
    })
    void evidencedLanguagesResolveOneTvIdWithoutTitleSearch(String query, long expected) throws Exception {
        var calls = new AtomicInteger();
        var client = new TmdbClient() {
            public Optional<Series> resolveSeries(String q) { fail("Known alias must not trigger title search"); return Optional.empty(); }
            public Optional<Series> resolveSeriesById(long id) {
                calls.incrementAndGet();
                assertEquals(expected, id);
                return Optional.of(LIVE.get(id));
            }
        };
        var result = new VerifiedSeriesResolver(client).resolve(query).orElseThrow();
        assertEquals(expected, result.id());
        assertEquals(expected == 70523 ? "DE" : "US", result.originCountry());
        assertEquals(1, calls.get());
    }

    @Test void unknownTitleUsesExistingResolverAndBlankDoesNotCallIt() throws Exception {
        var calls = new AtomicInteger();
        var resolver = new VerifiedSeriesResolver(q -> {
            calls.incrementAndGet();
            assertEquals("Unknown title", q);
            return Optional.empty();
        });
        assertTrue(resolver.resolve(" ").isEmpty());
        assertTrue(resolver.resolve("Unknown title").isEmpty());
        assertEquals(1, calls.get());
    }

    @Test void liveDetailsMustMatchIdOriginalTitleAndCountry() throws Exception {
        for (var wrong : List.of(new Series(99, "Breaking Bad", "US", null),
                new Series(1396, "Different show", "US", null),
                new Series(1396, "Breaking Bad", "DE", null))) {
            var resolver = new VerifiedSeriesResolver(new TmdbClient() {
                public Optional<Series> resolveSeries(String q) { fail("Do not silently fallback"); return Optional.empty(); }
                public Optional<Series> resolveSeriesById(long id) { return Optional.of(wrong); }
            });
            assertTrue(resolver.resolve("絕命毒師").isEmpty());
        }
    }

    private CuratedCatalog collisionCatalog() {
        return new CuratedCatalog("2026-11-06", List.of(
            new CuratedCatalog.Entry(1, "One", "US", "Original note", collisionAliases("One")),
            new CuratedCatalog.Entry(2, "Two", "DE", "Original note", collisionAliases("Two"))));
    }

    private List<CuratedCatalog.Alias> collisionAliases(String page) {
        return List.of("en", "zh-Hant", "zh-Hans", "ja", "ko").stream()
                .map(locale -> new CuratedCatalog.Alias(locale, "Shared",
                        "https://" + (locale.startsWith("zh") ? "zh" : locale) + ".wikipedia.org/wiki/" + page + "_" + locale))
                .toList();
    }

    @Test void collisionDoesNotChooseAnIdentityOrCallNetwork() {
        var identities = new VerifiedSeriesResolver.IdentityCatalog("2026-11-06", List.of(
            new VerifiedSeriesResolver.Identity(1, 11, "https://www.themoviedb.org/tv/11"),
            new VerifiedSeriesResolver.Identity(2, 22, "https://www.themoviedb.org/tv/22")));
        var client = new TmdbClient() {
            public Optional<Series> resolveSeries(String q) { fail("No title fallback on collision"); return Optional.empty(); }
            public Optional<Series> resolveSeriesById(long id) { fail("No arbitrary winner"); return Optional.empty(); }
        };
        assertTrue(new VerifiedSeriesResolver(client, collisionCatalog(), identities).resolve("Shared").isEmpty());
    }

    @Test void invalidOrDuplicateMappingsAreRejected() {
        for (var mapping : List.of(
            List.of(new VerifiedSeriesResolver.Identity(999, 11, "https://www.themoviedb.org/tv/11")),
            List.of(new VerifiedSeriesResolver.Identity(1, 11, "https://example.org/tv/11")),
            List.of(new VerifiedSeriesResolver.Identity(1, 11, "https://www.themoviedb.org/tv/11"),
                    new VerifiedSeriesResolver.Identity(2, 11, "https://www.themoviedb.org/tv/11")))) {
            assertThrows(IllegalArgumentException.class, () -> new VerifiedSeriesResolver(q -> Optional.empty(),
                    collisionCatalog(), new VerifiedSeriesResolver.IdentityCatalog("2026-11-06", mapping)));
        }
    }

    @Test void springControllerUsesTheVerifiedAliasResolver() {
        new ApplicationContextRunner().withUserConfiguration(VerifiedSeriesResolver.class, SeriesController.class)
            .withPropertyValues("metadata.tmdb.enabled=true")
            .withBean(TmdbClient.class, () -> new TmdbClient() {
                public Optional<Series> resolveSeries(String q) { fail("Wrong resolver path"); return Optional.empty(); }
                public Optional<Series> resolveSeriesById(long id) { return Optional.of(LIVE.get(id)); }
            }).run(context -> {
                assertNull(context.getStartupFailure());
                var response = context.getBean(SeriesController.class).resolve("絕命毒師");
                assertEquals("FOUND", response.status());
                assertEquals("https://www.themoviedb.org/tv/1396", response.url());
            });
    }
}
