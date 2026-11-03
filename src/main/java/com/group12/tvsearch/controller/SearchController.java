package com.group12.tvsearch.controller;

import com.group12.tvsearch.service.SearchResponse;
import com.group12.tvsearch.service.SearchService;
import com.group12.tvsearch.model.ResultCategory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam(defaultValue = "") String q,
                                 @RequestParam(defaultValue = "10") int limit,
                                 @RequestParam(required = false) ResultCategory category) {
        return searchService.search(q, Math.max(1, Math.min(50, limit)), category);
    }
}
