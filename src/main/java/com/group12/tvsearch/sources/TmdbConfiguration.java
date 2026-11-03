package com.group12.tvsearch.sources;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TmdbConfiguration {
    @Bean
    @ConditionalOnProperty(name = "metadata.tmdb.enabled", havingValue = "true")
    public TmdbClient httpTmdbClient(@Value("${TMDB_READ_ACCESS_TOKEN:}") String token) {
        return new HttpTmdbClient(token);
    }

    @Bean
    @ConditionalOnProperty(name = "metadata.tmdb.enabled", havingValue = "false", matchIfMissing = true)
    public TmdbClient noOpTmdbClient() { return new NoOpTmdbClient(); }
}
