package com.group12.tvsearch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.group12.tvsearch.sources.SqliteMetadataRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"search.collection=curated", "search.local-demo.enabled=false"})
@AutoConfigureMockMvc
class CuratedSearchIntegrationTest {
    private static Path database;
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) throws Exception {
        database = Files.createTempDirectory("tvsearch-curated-").resolve("curated.sqlite");
        registry.add("search.curated.database", () -> database.toString());
    }
    @AfterAll static void cleanup() throws Exception {
        Files.deleteIfExists(database); Files.deleteIfExists(database.getParent());
    }
    @Autowired SearchService service;
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @CsvSource({"Breaking Bad,100001", "絕命毒師,100001", "绝命毒师,100001", "ブレイキング・バッド,100001", "브레이킹 배드,100001",
        "Stranger Things,100002", "怪奇物語,100002", "怪奇物语,100002", "ストレンジャー・シングス 未知の世界,100002", "기묘한 이야기,100002",
        "Dark,100003", "闇,100003", "暗黑,100003", "ダーク,100003", "다크,100003"})
    void fifteenEvidencedTitleQueriesReachTheSameCanonicalSeries(String query, long id) throws Exception {
        var response = service.search(query, 10);
        assertEquals("CURATED_LOCAL_RESULTS", response.status());
        assertEquals(5, response.results().size());
        var repo = new SqliteMetadataRepository(database);
        assertEquals(id, repo.findVerifiedAliasCandidates(query).get(0).id());
        var expectedUrls = repo.findDocuments().stream().filter(d -> d.seriesId() == id).map(d -> d.url()).collect(java.util.stream.Collectors.toSet());
        assertEquals(expectedUrls, response.results().stream().map(r -> r.result().url()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(response.results().stream().allMatch(r -> r.result().source().equals("CURATED_LOCAL")));
        assertEquals(75, response.results().get(0).score(), .001);
        mvc.perform(get("/api/search").param("q", query)).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CURATED_LOCAL_RESULTS"));
    }

    @Test void originAndLanguageStayIndependentAndFictionalDataIsExcluded() throws Exception {
        var repo = new SqliteMetadataRepository(database);
        assertEquals("US", repo.findVerifiedAliasCandidates("브레이킹 배드").get(0).originCountry());
        assertEquals("DE", repo.findVerifiedAliasCandidates("ダーク").get(0).originCountry());
        assertEquals("NO_RESULTS", service.search("港灣燈火", 10).status());
        assertEquals("NO_RESULTS", service.search("芒果", 10).status());
        assertEquals("NO_RESULTS", service.search("動畫電影", 10).status());
    }

    @Test void collectionBannerAndCategoryStatesAreTruthful() throws Exception {
        mvc.perform(get("/api/collection")).andExpect(jsonPath("$.collection").value("curated"))
            .andExpect(jsonPath("$.examples[0]").value("絕命毒師"));
        mvc.perform(get("/api/search").param("q", "絕命毒師").param("category", "PROFESSIONAL_REVIEW"))
            .andExpect(jsonPath("$.status").value("NO_RESULTS")).andExpect(jsonPath("$.results.length()").value(0));
        mvc.perform(get("/demo/documents/harbor-info")).andExpect(status().isNotFound());
    }
}
