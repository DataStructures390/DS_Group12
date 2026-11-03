package com.group12.tvsearch.indexing;

import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IndexDifferentialTest {
    @Test void postingListsAgreeWithIndependentBruteForceAfterMixedUpdates() {
        var random = new Random(20261104);
        var index = new InvertedIndex();
        Map<String, Set<String>> reference = new HashMap<>();
        for (int operation = 0; operation < 800; operation++) {
            String id = "doc" + random.nextInt(80);
            if (random.nextInt(4) == 0) {
                index.removeDocument(id); reference.remove(id);
            } else {
                Set<String> words = new HashSet<>();
                for (int n = random.nextInt(10); n > 0; n--) words.add("word" + random.nextInt(25));
                index.addDocument(id, String.join(" ", words)); reference.put(id, Set.copyOf(words));
            }
            String left = "word" + random.nextInt(30);
            String right = "word" + random.nextInt(30);
            Set<String> expected = new HashSet<>();
            reference.forEach((doc, words) -> {
                if (words.contains(left) || words.contains(right)) expected.add(doc);
            });
            assertEquals(expected, index.search(left + " " + right), "operation " + operation);
            assertEquals(reference.size(), index.documentCount());
        }
        assertTrue(index.search("never-indexed-token").isEmpty());
    }
}
