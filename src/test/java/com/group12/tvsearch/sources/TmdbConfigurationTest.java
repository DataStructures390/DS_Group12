package com.group12.tvsearch.sources;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TmdbConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(TmdbConfiguration.class);
    @Test void defaultsToNoOpAndMakesNoExternalRequest() {
        context.run(c -> {
            assertNull(c.getStartupFailure());
            assertInstanceOf(NoOpTmdbClient.class, c.getBean(TmdbClient.class));
            assertTrue(c.getBean(TmdbClient.class).resolveSeries("Harbor").isEmpty());
        });
    }
    @Test void explicitEnableWithMissingTokenFailsClearly() {
        context.withPropertyValues("metadata.tmdb.enabled=true", "TMDB_READ_ACCESS_TOKEN=", "TMDB_API_KEY=")
            .run(c -> assertNotNull(c.getStartupFailure()));
    }
    @Test void apiKeyCanEnableTheClientWithoutAReadToken() {
        context.withPropertyValues("metadata.tmdb.enabled=true", "TMDB_READ_ACCESS_TOKEN=", "TMDB_API_KEY=synthetickey")
            .run(c -> {
                assertNull(c.getStartupFailure());
                assertInstanceOf(HttpTmdbClient.class, c.getBean(TmdbClient.class));
            });
    }
}
