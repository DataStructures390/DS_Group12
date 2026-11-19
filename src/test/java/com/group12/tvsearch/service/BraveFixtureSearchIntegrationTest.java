package com.group12.tvsearch.service;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "search.web.provider=brave-fixture")
@AutoConfigureMockMvc
class BraveFixtureSearchIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void fixtureResultsAreDeduplicatedRankedAndLabelled() throws Exception {
        mvc.perform(get("/api/search").param("q", "  BREAKING   Bad "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RESULTS_FROM_PROVIDER"))
            .andExpect(jsonPath("$.message", containsString("假資料")))
            // 8 fixture items: 1 duplicate URL and 1 ftp URL removed
            .andExpect(jsonPath("$.results.length()").value(6))
            .andExpect(jsonPath("$.results[*].result.source", everyItem(is("BRAVE_FIXTURE"))))
            .andExpect(jsonPath("$.results[*].result.url", everyItem(not(startsWith("ftp:")))))
            .andExpect(jsonPath("$.results[0].result.url")
                .value("https://www.ptt.example/bbs/EAseries/M.fixture-001.html"));
    }

    @Test void categoryFilterAppliesToProviderResults() throws Exception {
        mvc.perform(get("/api/search").param("q", "Breaking Bad").param("category", "PROFESSIONAL_REVIEW"))
            .andExpect(jsonPath("$.results.length()").value(1))
            .andExpect(jsonPath("$.results[0].result.category").value("PROFESSIONAL_REVIEW"));
    }

    @Test void smallLimitsStillFilterAndDeduplicateBeforeTopK() throws Exception {
        // The review is the 5th raw fixture item; it must survive limit=1 with a category filter.
        mvc.perform(get("/api/search").param("q", "Breaking Bad").param("limit", "1")
                .param("category", "PROFESSIONAL_REVIEW"))
            .andExpect(jsonPath("$.status").value("RESULTS_FROM_PROVIDER"))
            .andExpect(jsonPath("$.results.length()").value(1))
            .andExpect(jsonPath("$.results[0].result.category").value("PROFESSIONAL_REVIEW"));
        // The first two raw items are the same URL; limit=2 still returns two distinct pages.
        mvc.perform(get("/api/search").param("q", "Breaking Bad").param("limit", "2"))
            .andExpect(jsonPath("$.results.length()").value(2))
            .andExpect(jsonPath("$.results[0].result.url", containsString("fixture-001")))
            .andExpect(jsonPath("$.results[1].result.url", not(containsString("fixture-001"))));
    }

    @Test void localDemoStillWinsAndUnknownQueriesAreHonest() throws Exception {
        mvc.perform(get("/api/search").param("q", "港灣燈火"))
            .andExpect(jsonPath("$.status").value("LOCAL_DEMO_RESULTS"));
        mvc.perform(get("/api/search").param("q", "Some Other Show"))
            .andExpect(jsonPath("$.status").value("NO_LIVE_SEARCH"))
            .andExpect(jsonPath("$.message", containsString("Brave 假資料只收錄示範查詢")))
            .andExpect(jsonPath("$.results.length()").value(0));
    }

    @Test void collectionAdvertisesFixtureSamples() throws Exception {
        mvc.perform(get("/api/collection"))
            .andExpect(jsonPath("$.message", containsString("假資料")))
            .andExpect(jsonPath("$.examples", hasItems("港灣燈火", "Breaking Bad", "絕命毒師")));
    }
}
