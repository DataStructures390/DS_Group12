package com.group12.tvsearch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "search.local-demo.enabled=false")
@AutoConfigureMockMvc
class LocalDemoDisabledTest {
    @Autowired SearchService service;
    @Autowired MockMvc mvc;
    @Test void disablingDemoPreservesNoOpAndHidesDocuments() throws Exception {
        var response = service.search("Harbor Lights", 10);
        assertEquals("NO_LIVE_SEARCH", response.status());
        assertTrue(response.results().isEmpty());
        mvc.perform(get("/demo/documents/harbor-info")).andExpect(status().isNotFound());
    }

    @Test void collectionDoesNotAdvertiseDemoTitlesThatNoLongerSearch() throws Exception {
        // With the demo off and no web provider, suggesting "港灣燈火" would only lead to NO_LIVE_SEARCH.
        mvc.perform(get("/api/collection"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.collection").value("disabled"))
            .andExpect(jsonPath("$.message").value("本地示範已關閉。尚未接上即時網頁搜尋。"))
            .andExpect(jsonPath("$.examples.length()").value(0));
    }
}
