package com.group12.tvsearch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
