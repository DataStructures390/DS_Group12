# Stage 1 local-search contract

## Minimal reversible decisions

- Reuse SearchResult, SearchResponse, QueryProcessor, InvertedIndex, ResultMerger and RankingAlgorithm.
- Preserve search(query, limit); add optional ResultCategory filtering, applied before Top-K.
- GET /api/search?q=...&limit=10&category=PROFESSIONAL_REVIEW. Omit category for All. Unknown categories/noninteger limits return HTTP 400. Service and controller clamp limits to 1–50.
- Local demo is enabled by default; LOCAL_DEMO_ENABLED=false disables both retrieval and document routes. NoOpWebSearchClient remains unchanged and does not call the network.
- Local-first is a temporary Stage 1 policy: if local candidates exist, return only local candidates, even when the selected category is empty. Mixing live results is P2-05 and is not implemented.
- Empty input: EMPTY_QUERY. Local hits: LOCAL_DEMO_RESULTS. Category has no matching hits: NO_RESULTS. No local matches and NoOp provider: NO_LIVE_SEARCH. A nonempty authorized provider can still return RESULTS_FROM_PROVIDER.
- Local URLs are relative /demo/documents/{id}, served as UTF-8 plain text. Each document explicitly states that it is fictional. External demo URLs are unnecessary.
- SearchResult.source = LOCAL_DEMO identifies these fixtures. Category describes fixture purpose, not professional accreditation or actual streaming availability.

## Scoring

Index original title, self-authored demo alias, heading and body. OR retrieval finds any shared token. NFKC/case normalization, Latin/CJK boundaries, CJK full runs and bigrams are supported. This is not a production Japanese/Korean segmenter or automatic Traditional/Simplified conversion.

K = 100 * unique query tokens present in indexed fields / unique query tokens.
M = 100 * unique query tokens present in demo series title/alias / unique query tokens.
S = F = 0 because no source quality or reliable publication date is available.

Score = .45K + .30M + .15S + .10F. A complete fixture title match scores 75, not 100. M is lexical evidence over known fictional fixtures, not verified real-world identity resolution. Initial weights are not calibrated.

Results sort score descending, then original URL ascending. PriorityQueue retains Top-K in O(n log k), sorting the retained heap in O(k log k). Each feature clamps finite values to 0–100; nonfinite values contribute zero.

URL dedup keeps the first candidate. HTTP(S) canonicalization lowercases scheme/host, removes default port and fragment, normalizes literal dot segments and root empty path. It preserves path case, percent encoding, queries/query order and trailing slashes. Relative demo URLs use exact identity. No guessed tracking-parameter removal or HTTP-to-HTTPS equivalence.

## Coverage and limits

Fixtures: 12 original records, 3 fictional series, 4 categories each. No externally copied article bodies or API secrets. The corpus itself only contains fictional live-action series; this does not establish a general movie/anime filter for live search. Unknown real titles honestly return no results.

Tests: LocalSearchIntegrationTest, LocalDemoDisabledTest, TokenizerTest, InvertedIndexTest, RankingAlgorithmTest, DefaultSearchServiceTest. Source/metadata provenance, persistent storage, verified aliases and bounded live retries remain later tasks.
