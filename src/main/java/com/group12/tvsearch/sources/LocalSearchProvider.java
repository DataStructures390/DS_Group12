package com.group12.tvsearch.sources;

import com.group12.tvsearch.indexing.InvertedIndex;
import com.group12.tvsearch.indexing.Tokenizer;
import com.group12.tvsearch.model.LocalDocument;
import com.group12.tvsearch.model.SearchResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** OR retrieval over the actual index. Scores use only available, self-authored fields. */
@Component
public class LocalSearchProvider {
    private final InvertedIndex index = new InvertedIndex();
    private final Map<String, LocalDocument> documents = new HashMap<>();

    public LocalSearchProvider(LocalDocumentRepository repository,
                               @Value("${search.local-demo.enabled:true}") boolean enabled) {
        if (enabled) {
            for (LocalDocument document : repository.findAll()) {
                documents.put(document.id(), document);
                index.addDocument(document.id(), document.indexedText());
            }
        }
    }

    public List<SearchResult> search(String query) {
        List<String> terms = Tokenizer.tokens(query);
        return index.search(query).stream().sorted().map(documents::get)
            .map(d -> new SearchResult(d.url(), d.title(), d.body(), "LOCAL_DEMO", d.category(),
                coverage(terms, d.indexedText()), coverage(terms, d.identityText()), 0, 0))
            .toList();
    }

    private static double coverage(List<String> queryTerms, String text) {
        if (queryTerms.isEmpty()) return 0;
        Set<String> documentTerms = Set.copyOf(Tokenizer.tokens(text));
        long matches = queryTerms.stream().filter(documentTerms::contains).count();
        return 100.0 * matches / queryTerms.size();
    }
}
