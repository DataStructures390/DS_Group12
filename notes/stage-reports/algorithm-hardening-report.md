# Session Summary — 2026-11-04 — Algorithm/transport hardening

**Task IDs:** P1-02/P1-05 quality, P2-02 boundary validation, P3-04 efficiency groundwork only.
**Branch:** test/search-algorithm-hardening, stacked on PR #5.
**Actual changes:** reject coerced/fractional identity fields, compile tokenizer regex once, independently check index and heap algorithms, controlled concurrent rebuild test, truthful active-collection empty hints/score explanation, Wrapper verify CI, reproducible synthetic benchmark and actual implementation map.
**Tests:** full suite 127 passed, zero failures/errors/skips, 7.211s after production changes. Differential tests cover 800 update steps and 1,000 candidate rows across seven Top-K limits. These counts are test operations, not additional shows/users.
**Benchmark:** original generated tokens only; 100/1000/10000 documents, exact correctness comparison with a brute-force scan, median of 11 x 100 query batches. Raw actual run checked in; no universal speedup/memory/SLA/relevance claim. No external API calls.
**UI issue fixed:** curated no-result state previously retained hard-coded fictional title hints. It now uses /api/collection examples and keeps source-specific scoring explanation.
**Verification still to record:** final packaged verify, browser no-result hint smoke and Java17 Wrapper CI at this commit. Prior PR #5 CI was success (41673884909).
**Limitations:** synthetic microbenchmark is not JMH or independent live relevance evaluation. External rights/keys and teacher rubric still gate planned live/Tree/AI work.

**Final local evidence:** .\mvnw.cmd -B --no-transfer-progress verify succeeded, 127 tests and packaged JAR, 7.683s. Node syntax check and git diff --check passed. Browser curated 芒果 query showed 0 results and only the curated titles 絕命毒師／기묘한 이야기／ダーク as suggestions. Current demo server restarted with this packaged build. New GitHub Wrapper CI will be confirmed on the PR.
