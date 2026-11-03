package com.group12.tvsearch.indexing;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokenizerTest {
    @Test void normalizesDeduplicatesAndHandlesEmptyInput() {
        assertEquals(List.of("harbor", "lights"), Tokenizer.tokens("ＨＡＲＢＯＲ harbor LIGHTS!"));
        assertTrue(Tokenizer.tokens(null).isEmpty());
        assertTrue(Tokenizer.tokens(" ,! ").isEmpty());
        assertTrue(Tokenizer.tokens("").isEmpty());
    }
    @Test void indexesCjkBigramsWithoutBreakingSupplementaryCharacters() {
        assertTrue(Tokenizer.tokens("港灣燈火").containsAll(List.of("港灣", "灣燈", "燈火")));
        assertTrue(Tokenizer.tokens("ミッドナイト").contains("ミッ"));
        assertTrue(Tokenizer.tokens("미드나이트").contains("미드"));
        assertTrue(Tokenizer.tokens("𠀀山列車").contains("𠀀山"));
    }
    @Test void separatesAdjacentLatinAndCjkRuns() {
        var terms = Tokenizer.tokens("Harbor港灣燈火2026");
        assertTrue(terms.containsAll(List.of("harbor", "港灣燈火", "港灣", "2026")));
        assertFalse(terms.contains("r港"));
        assertFalse(terms.contains("火2"));
    }
    @Test void indexHasDocumentedOrSemanticsAndSafeReplacement() {
        InvertedIndex index = new InvertedIndex();
        index.addDocument("a", "harbor harbor");
        index.addDocument("b", "garden");
        assertEquals(java.util.Set.of("a", "b"), index.search("harbor garden"));
        assertTrue(index.search(null).isEmpty());
        index.addDocument("a", "");
        assertTrue(index.search("harbor").isEmpty());
        index.removeDocument("missing");
        assertThrows(NullPointerException.class, () -> index.addDocument(null, "x"));
        assertThrows(NullPointerException.class, () -> index.addDocument("x", null));
    }
}
