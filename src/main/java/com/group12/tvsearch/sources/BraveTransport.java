package com.group12.tvsearch.sources;

import java.util.Optional;

/**
 * Supplies a raw Brave Web Search JSON response for a normalized query.
 * The fixture implementation never contacts the network; a live HTTP transport is not installed yet.
 */
@FunctionalInterface
public interface BraveTransport {
    Optional<String> fetch(String normalizedQuery, int count);
}
