package com.group12.tvsearch.sources;

/** Safe diagnostic categories; messages never include credentials, URLs or response bodies. */
public class TmdbAccessException extends RuntimeException {
    public enum Reason { AUTHENTICATION, RATE_LIMITED, TIMEOUT, NETWORK, MALFORMED_RESPONSE, HTTP_ERROR, INTERRUPTED }
    private final Reason reason;
    public TmdbAccessException(Reason reason) {
        super("TMDB request failed: " + reason.name());
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
