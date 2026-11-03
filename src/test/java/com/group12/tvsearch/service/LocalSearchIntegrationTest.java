package com.group12.tvsearch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.group12.tvsearch.sources.LocalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LocalSearchIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SearchService service;
    @Autowired LocalDocumentRepository repository;

    @ParameterizedTest
    @ValueSource(strings = {"Harbor Lights", "港灣燈火", "ＨＡＲＢＯＲ lights", "午夜花園", "Mountain Train"})
    void returnsFourCategorizedLocalDocuments(String query) throws Exception {
        mvc.perform(get("/api/search").param("q", query))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("LOCAL_DEMO_RESULTS"))
            .andExpect(jsonPath("$.results.length()").value(4))
            .andExpect(jsonPath("$.results[0].result.source").value("LOCAL_DEMO"));
    }

    @Test void categoryFilteringHappensBeforeTopK() throws Exception {
        mvc.perform(get("/api/search").param("q", "港灣燈火").param("limit", "1")
                .param("category", "STREAMING_PLATFORM"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.results.length()").value(1))
            .andExpect(jsonPath("$.results[0].result.category").value("STREAMING_PLATFORM"));
    }

    @Test void rejectsInvalidCategoryAndLimit() throws Exception {
        mvc.perform(get("/api/search").param("category", "UNKNOWN")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/search").param("limit", "abc")).andExpect(status().isBadRequest());
    }

    @Test void handlesEmptyUnknownAndOutOfScope() throws Exception {
        mvc.perform(get("/api/search").param("q", "  ")).andExpect(jsonPath("$.status").value("EMPTY_QUERY"));
        for (String query : new String[]{"zzzzzz", "動畫電影", "芒果", "Breaking Bad"}) {
            SearchResponse response = service.search(query, 10);
            assertEquals("NO_LIVE_SEARCH", response.status());
            assertTrue(response.results().isEmpty());
        }
    }

    @Test void localLinksResolveAndDoNotPretendToBeRealWebsites() throws Exception {
        assertEquals(12, repository.findAll().size());
        for (var doc : repository.findAll()) {
            mvc.perform(get(doc.url())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("本地虛構示範文件")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(doc.title())));
        }
        mvc.perform(get("/demo/documents/missing")).andExpect(status().isNotFound());
    }

    @Test void computesFeaturesFromFieldsAndClampsLimits() {
        var full = service.search("Harbor Lights", 100).results();
        assertEquals(4, full.size());
        assertEquals(75, full.get(0).score(), 0.001);
        assertEquals(100, full.get(0).result().keywordRelevance());
        assertEquals(100, full.get(0).result().seriesRelevance());
        assertEquals(0, full.get(0).result().sourceRelevance());
        assertEquals(0, full.get(0).result().freshness());
        var keyword = service.search("攝影", 10).results();
        assertEquals(1, keyword.size());
        assertEquals(45, keyword.get(0).score(), 0.001);
        assertEquals(1, service.search("Harbor Lights", -1).results().size());
        assertEquals(full, service.search("Harbor Lights", 100).results());
    }

    @ParameterizedTest
    @CsvSource({
        "Harbor Lights,harbor,4", "harbor lights,harbor,4", "HARBOR LIGHTS,harbor,4",
        "ＨＡＲＢＯＲ ＬＩＧＨＴＳ,harbor,4", "港灣燈火,harbor,4", "港灣,harbor,4",
        "Midnight Garden,garden,4", "MIDNIGHT GARDEN,garden,4", "午夜花園,garden,4",
        "午夜,garden,4", "Mountain Train,train,4", "MOUNTAIN TRAIN,train,4",
        "山間列車,train,4", "山間,train,4", "攝影,harbor,1", "配樂,garden,1",
        "zzzzzz,none,0", "動畫電影,none,0", "芒果,none,0", "Breaking Bad,none,0"
    })
    void twentyCaseLocalRegression(String query, String prefix, int count) {
        var response = service.search(query, 10);
        assertEquals(count, response.results().size());
        for (var r : response.results()) {
            assertTrue(r.result().url().startsWith("/demo/documents/" + prefix + "-"));
        }
    }

    @Test void extraRelevantKeywordChangesRankingAndEmptyCategoryIsHonest() {
        assertEquals("/demo/documents/harbor-review",
            service.search("港灣燈火 攝影", 1).results().get(0).result().url());
        var response = service.search("攝影", 10,
            com.group12.tvsearch.model.ResultCategory.STREAMING_PLATFORM);
        assertEquals("NO_RESULTS", response.status());
        assertTrue(response.results().isEmpty());
    }
}
