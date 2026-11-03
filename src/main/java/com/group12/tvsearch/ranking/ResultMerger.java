package com.group12.tvsearch.ranking;

import com.group12.tvsearch.model.SearchResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.net.URI;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** First occurrence wins. Normalize host/default port/fragments, preserving query semantics. */
@Component
public class ResultMerger {
    public List<SearchResult> deduplicate(List<SearchResult> candidates) {
        Set<String> seen = new HashSet<>();
        List<SearchResult> unique = new ArrayList<>();
        for (SearchResult candidate : candidates) {
            if (seen.add(canonicalUrl(candidate.url()))) unique.add(candidate);
        }
        return unique;
    }

    public static String canonicalUrl(String url) {
        String trimmed = url.trim();
        try {
            URI uri = URI.create(trimmed).normalize();
            if (uri.getHost() == null) return trimmed;
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) return trimmed;
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            String authority = (uri.getRawUserInfo() == null ? "" : uri.getRawUserInfo() + "@") + host;
            if (port != -1 && !(scheme.equals("http") && port == 80)
                    && !(scheme.equals("https") && port == 443)) authority += ":" + port;
            String path = uri.getRawPath();
            if (path == null || path.isEmpty()) path = "/";
            return scheme + "://" + authority + path
                    + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        } catch (IllegalArgumentException e) {
            return trimmed;
        }
    }
}
