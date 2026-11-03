package com.group12.tvsearch.query;

import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Basic Unicode normalization; full multi-language resolution is future work. */
@Component
public class QueryProcessor {
    public String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
