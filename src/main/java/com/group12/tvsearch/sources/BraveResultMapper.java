package com.group12.tvsearch.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group12.tvsearch.indexing.Tokenizer;
import com.group12.tvsearch.model.ResultCategory;
import com.group12.tvsearch.model.SearchResult;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts a Brave Web Search response (web.results[]) into ranking features.
 * K = query-term coverage in title + description; M = coverage in title;
 * S = initial per-domain source weight; F = linear decay of page_age from 30 days to 3 years.
 * The S table is an initial design to be calibrated, not a measured credibility rating.
 */
public final class BraveResultMapper {
    record SourceProfile(ResultCategory category, double relevance) {}

    static final SourceProfile UNKNOWN = new SourceProfile(ResultCategory.SERIES_INFORMATION, 20);
    private static final Map<String, SourceProfile> DOMAINS = new LinkedHashMap<>();
    static {
        DOMAINS.put("ptt.cc", new SourceProfile(ResultCategory.COMMUNITY_DISCUSSION, 70));
        DOMAINS.put("dcard.tw", new SourceProfile(ResultCategory.COMMUNITY_DISCUSSION, 70));
        DOMAINS.put("reddit.com", new SourceProfile(ResultCategory.COMMUNITY_DISCUSSION, 60));
        DOMAINS.put("wikipedia.org", new SourceProfile(ResultCategory.SERIES_INFORMATION, 80));
        DOMAINS.put("themoviedb.org", new SourceProfile(ResultCategory.SERIES_INFORMATION, 80));
        DOMAINS.put("imdb.com", new SourceProfile(ResultCategory.SERIES_INFORMATION, 75));
        DOMAINS.put("rottentomatoes.com", new SourceProfile(ResultCategory.PROFESSIONAL_REVIEW, 75));
        DOMAINS.put("metacritic.com", new SourceProfile(ResultCategory.PROFESSIONAL_REVIEW, 75));
        DOMAINS.put("netflix.com", new SourceProfile(ResultCategory.STREAMING_PLATFORM, 70));
        DOMAINS.put("disneyplus.com", new SourceProfile(ResultCategory.STREAMING_PLATFORM, 70));
        DOMAINS.put("primevideo.com", new SourceProfile(ResultCategory.STREAMING_PLATFORM, 70));
        // Reserved RFC 2606 hosts used only by the offline fixture.
        DOMAINS.put("ptt.example", new SourceProfile(ResultCategory.COMMUNITY_DISCUSSION, 70));
        DOMAINS.put("dcard.example", new SourceProfile(ResultCategory.COMMUNITY_DISCUSSION, 70));
        DOMAINS.put("wiki.example", new SourceProfile(ResultCategory.SERIES_INFORMATION, 80));
        DOMAINS.put("review.example", new SourceProfile(ResultCategory.PROFESSIONAL_REVIEW, 75));
        DOMAINS.put("stream.example", new SourceProfile(ResultCategory.STREAMING_PLATFORM, 70));
    }

    private static final long FRESH_DAYS = 30;
    private static final long STALE_DAYS = 3 * 365;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Clock clock;

    public BraveResultMapper(Clock clock) {
        this.clock = clock;
    }

    /** Malformed JSON or a response without web.results yields an empty list, never an exception. */
    public List<SearchResult> map(String json, String normalizedQuery, String sourceLabel) {
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
        if (root == null) return List.of();
        JsonNode items = root.path("web").path("results");
        if (!items.isArray()) return List.of();
        List<String> terms = Tokenizer.tokens(normalizedQuery);
        List<SearchResult> mapped = new ArrayList<>();
        for (JsonNode item : items) {
            String url = item.path("url").asText("").trim();
            String title = plainText(item.path("title").asText(""));
            String host = httpHost(url);
            if (host == null || title.isBlank()) continue;
            String description = plainText(item.path("description").asText(""));
            SourceProfile profile = profile(host);
            mapped.add(new SearchResult(url, title, description, sourceLabel, profile.category(),
                coverage(terms, title + " " + description), coverage(terms, title),
                profile.relevance(), freshness(item.path("page_age").asText(""))));
        }
        return mapped;
    }

    static SourceProfile profile(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        for (var entry : DOMAINS.entrySet()) {
            String domain = entry.getKey();
            if (h.equals(domain) || h.endsWith("." + domain)) return entry.getValue();
        }
        return UNKNOWN;
    }

    static String httpHost(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) return null;
            scheme = scheme.toLowerCase(Locale.ROOT);
            return scheme.equals("http") || scheme.equals("https") ? uri.getHost() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Brave wraps matched words in tags such as strong; keep only text. */
    static String plainText(String html) {
        return html.replaceAll("<[^>]*>", "")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&amp;", "&").replaceAll("\\s+", " ").trim();
    }

    double freshness(String pageAge) {
        Instant published = parse(pageAge);
        if (published == null) return 0;
        long days = ChronoUnit.DAYS.between(published, clock.instant());
        if (days < 0) return 0; // future dates are not trusted
        if (days <= FRESH_DAYS) return 100;
        if (days >= STALE_DAYS) return 0;
        return 100.0 * (STALE_DAYS - days) / (STALE_DAYS - FRESH_DAYS);
    }

    private static Instant parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }

    private static double coverage(List<String> queryTerms, String text) {
        if (queryTerms.isEmpty()) return 0;
        Set<String> textTerms = Set.copyOf(Tokenizer.tokens(text));
        long matches = queryTerms.stream().filter(textTerms::contains).count();
        return 100.0 * matches / queryTerms.size();
    }
}
