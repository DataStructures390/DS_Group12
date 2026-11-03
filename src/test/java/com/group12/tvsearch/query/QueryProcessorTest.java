package com.group12.tvsearch.query;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class QueryProcessorTest {
    @Test
    void normalizesFullWidthLettersAndSpaces() {
        QueryProcessor processor = new QueryProcessor();
        assertEquals("breaking bad", processor.normalize("  ＢＲＥＡＫＩＮＧ   BAD "));
        assertEquals("", processor.normalize(null));
    }
}
