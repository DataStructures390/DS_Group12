package com.group12.tvsearch.controller;

import com.group12.tvsearch.sources.LocalDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocalDocumentController {
    private final LocalDocumentRepository repository;
    private final boolean enabled;

    public LocalDocumentController(LocalDocumentRepository repository,
                                   @Value("${search.local-demo.enabled:true}") boolean enabled) {
        this.repository = repository;
        this.enabled = enabled;
    }

    @GetMapping(value = "/demo/documents/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> document(@PathVariable String id) {
        if (!enabled) return ResponseEntity.notFound().build();
        return repository.findById(id).map(d -> ResponseEntity.ok()
            .contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
            .body("LOCAL DEMO / 本地虛構示範文件\n非真實影集資訊、影評或播放服務。\n\n"
                + d.title() + "\nSeries ID: demo-" + d.seriesId() + "\nCategory: "
                + d.category() + "\n\n" + d.body()))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
