package com.group12.tvsearch.indexing;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** NFKC words and CJK bigrams. Script transitions do not create mixed-script bigrams. */
public final class Tokenizer {
    private Tokenizer() {}

    public static List<String> tokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        Set<String> terms = new LinkedHashSet<>();
        String separated = normalized.replaceAll(
                "(?<=[\\p{IsLatin}\\p{N}])(?=[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}])"
                + "|(?<=[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsHangul}])(?=[\\p{IsLatin}\\p{N}])", " ");
        for (String word : separated.split("[^\\p{L}\\p{N}]+")) {
            if (word.isEmpty()) continue;
            terms.add(word);
            int[] cp = word.codePoints().toArray();
            if (cp.length > 2 && containsCjk(cp)) {
                for (int i = 0; i < cp.length - 1; i++) {
                    terms.add(new String(cp, i, 2));
                }
            }
        }
        return new ArrayList<>(terms);
    }

    private static boolean containsCjk(int[] codepoints) {
        for (int ch : codepoints) {
            Character.UnicodeScript s = Character.UnicodeScript.of(ch);
            if (s == Character.UnicodeScript.HAN
                    || s == Character.UnicodeScript.HIRAGANA
                    || s == Character.UnicodeScript.KATAKANA
                    || s == Character.UnicodeScript.HANGUL) return true;
        }
        return false;
    }
}
