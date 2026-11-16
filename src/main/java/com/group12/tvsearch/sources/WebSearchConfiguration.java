package com.group12.tvsearch.sources;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * search.web.provider selects the web search adapter:
 * none (default) returns nothing; brave-fixture returns offline fake data in Brave's format.
 * There is deliberately no live Brave option yet: it requires an approved plan and key.
 */
@Configuration
public class WebSearchConfiguration {
    public static final String FIXTURE_SOURCE = "BRAVE_FIXTURE";
    public static final String FIXTURE_NOTICE =
        "Brave 搜尋介面測試：以下全部是假資料，網址無法開啟，不是真實網頁。";

    @Bean
    WebSearchClient webSearchClient(@Value("${search.web.provider:none}") String provider) {
        return switch (provider.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "none" -> new NoOpWebSearchClient();
            case "brave-fixture" -> {
                FixtureBraveTransport transport = FixtureBraveTransport.fromClasspath();
                yield new BraveWebSearchClient(transport, new BraveResultMapper(Clock.systemUTC()),
                    FIXTURE_SOURCE, FIXTURE_NOTICE, transport.sampleQueries());
            }
            default -> throw new IllegalStateException(
                "Unknown search.web.provider; use none or brave-fixture");
        };
    }
}
