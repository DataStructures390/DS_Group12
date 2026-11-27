package com.group12.tvsearch.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.group12.tvsearch.model.ResultCategory;
import com.group12.tvsearch.service.SearchResponse;
import com.group12.tvsearch.service.SearchService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP parameter handling of /api/search, checked against a recording service (no index, no network). */
class SearchControllerTest {
    private record Call(String query, int limit, ResultCategory category) {}

    private final List<Call> calls = new ArrayList<>();
    private MockMvc mvc;

    @BeforeEach void setUp() {
        SearchService recording = new SearchService() {
            @Override public SearchResponse search(String query, int limit) {
                return search(query, limit, null);
            }
            @Override public SearchResponse search(String query, int limit, ResultCategory category) {
                calls.add(new Call(query, limit, category));
                return new SearchResponse(query, "NO_RESULTS", "recorded", List.of());
            }
        };
        mvc = MockMvcBuilders.standaloneSetup(new SearchController(recording)).build();
    }

    @Test void omittedParametersUseEmptyQueryLimitTenAndAllCategories() throws Exception {
        mvc.perform(get("/api/search")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NO_RESULTS"));
        assertEquals(List.of(new Call("", 10, null)), calls);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "50,50", "0,1", "-5,1", "51,50", "2147483647,50", "-2147483648,1"})
    void controllerClampsLimitToOneThroughFifty(String requested, int passed) throws Exception {
        mvc.perform(get("/api/search").param("q", "Harbor Lights").param("limit", requested))
            .andExpect(status().isOk());
        assertEquals(List.of(new Call("Harbor Lights", passed, null)), calls);
    }

    @Test void emptyCategoryMeansAllCategories() throws Exception {
        mvc.perform(get("/api/search").param("q", "x").param("category", ""))
            .andExpect(status().isOk());
        assertEquals(List.of(new Call("x", 10, null)), calls);
    }

    @Test void exactCategoryNameIsPassedThrough() throws Exception {
        mvc.perform(get("/api/search").param("q", "x").param("category", "PROFESSIONAL_REVIEW"))
            .andExpect(status().isOk());
        assertEquals(List.of(new Call("x", 10, ResultCategory.PROFESSIONAL_REVIEW)), calls);
    }

    @ParameterizedTest
    @CsvSource({"limit,abc", "limit,2147483648", "limit,1.5", "category,professional_review", "category,UNKNOWN"})
    void malformedParametersAreRejectedBeforeTheService(String name, String value) throws Exception {
        mvc.perform(get("/api/search").param("q", "x").param(name, value))
            .andExpect(status().isBadRequest());
        assertTrue(calls.isEmpty(), "service must not run for a rejected request");
    }
}
