package com.group12.tvsearch.controller;

import static org.junit.jupiter.api.Assertions.*;
import com.group12.tvsearch.model.Series;
import com.group12.tvsearch.sources.TmdbAccessException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SeriesControllerTest {
    @Test void returnsMetadataWithCanonicalLinkThroughHttp() throws Exception {
        var controller = new SeriesController(q -> Optional.of(new Series(123, "Example", "US", "tmdb:tv:123")), true);
        MockMvcBuilders.standaloneSetup(controller).build().perform(get("/api/series").param("q", "Example"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FOUND"))
                .andExpect(jsonPath("$.title").value("Example"))
                .andExpect(jsonPath("$.url").value("https://www.themoviedb.org/tv/123"));
    }

    @Test void disabledAndInvalidQueriesNeverCallProvider() {
        var controller = new SeriesController(q -> { fail("Unexpected network call"); return Optional.empty(); }, false);
        assertEquals("EMPTY_QUERY", controller.resolve("  ").status());
        assertEquals("QUERY_TOO_LONG", controller.resolve("x".repeat(301)).status());
        assertEquals("DISABLED", controller.resolve("Example").status());
    }

    @Test void noMatchIsUnconfirmedRatherThanInventedMetadata() {
        var controller = new SeriesController(q -> Optional.empty(), true);
        assertEquals("UNCONFIRMED", controller.resolve("Example").status());
        assertNull(controller.resolve("Example").url());
    }

    @Test void failuresAreSafeAndReleaseCapacity() {
        int[] count = {0};
        var controller = new SeriesController(q -> {
            if (count[0]++ == 0) throw new TmdbAccessException(TmdbAccessException.Reason.AUTHENTICATION);
            return Optional.empty();
        }, true);
        assertEquals("AUTHENTICATION", controller.resolve("Example").status());
        assertEquals("UNCONFIRMED", controller.resolve("Example").status());
    }

    @Test void simultaneousRequestsDoNotQueueAdditionalProviderCalls() {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var controller = new SeriesController(q -> {
            entered.countDown();
            try { assertTrue(release.await(5, java.util.concurrent.TimeUnit.SECONDS)); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
            return Optional.empty();
        }, true);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> controller.resolve("Example"));
            assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals("BUSY", controller.resolve("Other").status());
            release.countDown();
            assertEquals("UNCONFIRMED", first.get(5, java.util.concurrent.TimeUnit.SECONDS).status());
        } catch (Exception e) { throw new AssertionError(e); }
        finally { release.countDown(); executor.shutdownNow(); }
    }
}
