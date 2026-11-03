# Optional TMDB adapter — P2-02 groundwork

HttpTmdbClient implements the existing TmdbClient interface. It is an optional metadata identity resolver, not a web search source. Normal SearchService does not call it yet; merely enabling the bean does not add live results to the UI.

## Credentials and activation boundary

Default TMDB_ENABLED=false creates NoOpTmdbClient. An explicit true value requires TMDB_READ_ACCESS_TOKEN (API Read Access Token, sent only as a Bearer Authorization header). The old TMDB_API_KEY example is a different v3 authentication method and is not consumed by this implementation. .env files are examples, not automatically loaded. Never place real credentials in code, command arguments, commits or logs.

No token was provided/read, no real request was made, no TMDB metadata was persisted, and no paid account was created in this session. Before displaying retrieved data, satisfy TMDB's attribution/logo requirements and applicable accepted terms. Live smoke verification remains blocked until access and those requirements are in place.

## Behavior and budget

- Blank query returns empty without network.
- One TV-only search request, then at most one details request. No pagination, automatic retries or movie endpoint.
- Query is Unicode normalized/URL encoded; language is en-US for response field interpretation, with no origin-country restriction.
- Exact normalized name/original_name match only. Multiple candidate IDs or additional search pages return empty rather than guess. This intentionally favors precision over recall and may miss valid translated/alternative titles. Full verified alias coverage is P2-03.
- Details must confirm the same ID, nonempty original title, Scripted or Miniseries type, nonempty genre metadata, and no Animation/Documentary/Reality/Talk/News genre. Missing metadata does not establish scope. English genre labels/type allowlists are conservative assumptions pending live verification; they are not a complete independent content classifier.
- Response origin_country becomes the series country; query language never sets country. Stable identity uses TMDB TV ID and tmdb:tv:{id} external reference.
- Default connect timeout 3s, request timeout 5s; redirects disabled. There is no retry after 429. Two calls per resolution is a per-resolution bound, not a global account quota manager.
- Authentication, rate-limit, timeout, network, malformed-response, HTTP and interruption failures have typed safe reasons. Interrupted threads are restored. No response body, token, request URL or wrapped transport cause is placed in error messages.
- Response size is validated after download; this adapter is not yet a streaming bounded-body transport. Caller should not expose unbounded resolution loops.

## Verification

Local HttpServer tests use a plainly synthetic token and original JSON responses. They test actual HTTP request encoding/header behavior, five-language input with independent US fixture origin, ambiguous/empty results, malformed data, non-TV scope, miniseries, 401/403/429/500/redirect, timeout, network errors, missing credentials and default NoOp configuration. These are not real metadata or verified translations.

Full suite: **85 tests, zero failures/errors/skips**, 6.315s on Java21/release17. TMDB-specific transport cases: 32; configuration cases: 2. Parent SQLite PR Java17 CI passed (41670280592).

## Official references checked 2026-11-04

- [TV search](https://developer.themoviedb.org/reference/search-tv)
- [TV details](https://developer.themoviedb.org/reference/tv-series-details)
- [Application authentication](https://developer.themoviedb.org/docs/authentication-application)
- [Rate limiting](https://developer.themoviedb.org/docs/rate-limiting)
- [Attribution/noncommercial FAQ](https://developer.themoviedb.org/docs/faq)

Next: live smoke validation after authorization/configuration, verified alias provenance and explicit ambiguous identity behavior. Do not mark real TMDB integration complete from mock results alone.
