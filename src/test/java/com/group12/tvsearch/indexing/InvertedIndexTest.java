package com.group12.tvsearch.indexing;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InvertedIndexTest {
    @Test
    void handlesEnglishAndChineseIndexing() {
        InvertedIndex index = new InvertedIndex();
        index.addDocument("a", "Breaking Bad 絕命毒師");
        index.addDocument("b", "Stranger Things 怪奇物語");
        assertEquals(Set.of("a"), index.search("breaking"));
        assertEquals(Set.of("a"), index.search("絕命"));
        assertEquals(2, index.documentCount());
    }

    @Test
    void replacementDoesNotLeaveStaleTerms() {
        InvertedIndex index = new InvertedIndex();
        index.addDocument("a", "old title");
        index.addDocument("a", "new title");
        assertTrue(index.search("old").isEmpty());
        assertEquals(Set.of("a"), index.search("new"));
        index.removeDocument("a");
        assertTrue(index.search("new").isEmpty());
    }
}
