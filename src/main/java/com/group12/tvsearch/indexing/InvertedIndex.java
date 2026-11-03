package com.group12.tvsearch.indexing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Real local OR-based Inverted Index; only index content we have permission to store. */
public class InvertedIndex {
    private final Map<String, Set<String>> postings = new HashMap<>();
    private final Map<String, Set<String>> documentTerms = new HashMap<>();

    public synchronized void addDocument(String documentId, String text) {
        Objects.requireNonNull(documentId);
        Objects.requireNonNull(text);
        removeDocument(documentId);
        Set<String> terms = new HashSet<>(Tokenizer.tokens(text));
        documentTerms.put(documentId, terms);
        for (String term : terms) {
            postings.computeIfAbsent(term, key -> new HashSet<>()).add(documentId);
        }
    }

    public synchronized Set<String> search(String query) {
        List<String> terms = Tokenizer.tokens(query);
        Set<String> result = new LinkedHashSet<>();
        for (String term : terms) {
            result.addAll(postings.getOrDefault(term, Set.of()));
        }
        return Set.copyOf(result);
    }

    public synchronized void removeDocument(String documentId) {
        Set<String> existing = documentTerms.remove(documentId);
        if (existing == null) return;
        for (String term : existing) {
            Set<String> docs = postings.get(term);
            if (docs != null) {
                docs.remove(documentId);
                if (docs.isEmpty()) postings.remove(term);
            }
        }
    }

    public synchronized int documentCount() {
        return documentTerms.size();
    }
}
