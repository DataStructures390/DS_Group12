# Execution backlog

Baseline: main at 2b98cec, inspected 2026-11-03. Original working tree clean; origin = https://github.com/DataStructures390/DS_Group12.git. Java 21.0.12.1, Maven initially absent; isolated Maven 3.9.11 installed under the parent work/tools directory, then generated project Wrapper 3.3.4 pinned to Maven 3.9.11. Baseline: 6 tests pass. No AGENTS.md found in the checkout. Read root/spec/roadmap and every module TODO, src/main, src/test and Java CI.

All owners remain **unassigned**; Codex implemented changes without assigning team members. Done means local evidence below, not merged/CI verified. Unlisted broad TODO checkboxes stay open until their whole scope is satisfied.

| ID | Priority | Owner | Dependencies | Affected files / deliverables | Acceptance and tests | Status |
|---|---|---|---|---|---|---|
| P0-01 | P0 | unassigned | — | pom.xml, src, java-ci.yml; stage report | Confirm remote/status; run actual Java/Maven tests | Done: baseline 6 pass |
| P0-02 | P0 | unassigned | P0-01 | this file; all TODO inventories | Tasks carry owner, dependency, affected files, acceptance, test, status | Done |
| P0-03 | P0 | unassigned | teacher | PROJECT_SPEC, ROADMAP | Actual rubric clarifies Tree/BFS/HTML parsing/LLM requirements | Blocked: teacher rubric unavailable |
| P0-04 | P0 | unassigned | — | SOURCE_POLICY.md | Official-source matrix; selected plan rights, costs, quotas and coverage | Partial: matrix done; plan rights/key/20 live cases pending |
| P0-05 | P0 | unassigned | P0-01 | LOCAL_SEARCH_CONTRACT.md; model/service/controller | Minimal local I/O, scoring and status contract; integration tests | Done for Stage 1; real metadata contract pending P2 |
| P1-01 | P1 | unassigned | P0-01 | LocalDocument, LocalDocumentRepository | >=8 self-authored docs with IDs, category, local source/URL | Done: 12 documents; all 12 URLs tested |
| P1-02 | P1 | unassigned | P1-01 | Tokenizer, InvertedIndex, TokenizerTest | English/CJK, Unicode, duplicates, removal/replacement, null/empty, documented OR | Done; no claim of full five-language segmentation |
| P1-03 | P1 | unassigned | P0-05, P1-01 | LocalSearchProvider, DefaultSearchService | Query passes through actual index without a network API | Done; enabled/disabled integration tests |
| P1-04 | P1 | unassigned | P1-03 | LocalSearchProvider; contract | K/M computed from indexed fields; disclose missing S/F | Done for demo; scoring assertions and rank change |
| P1-05 | P1 | unassigned | P1-04 | RankingAlgorithm, ResultMerger, ranking tests | Deterministic tie, conservative canonical URLs, clamp, Top-K, nonfinite features | Done; query-identity/encoding collision tests |
| P1-06 | P1 | unassigned | P1-03, P1-05 | controllers, static UI, Wrapper; stage report | App JSON/UI returns labeled local results; tests pass; linked docs load | Done locally; browser checks recorded |
| P1-07 | P1 | unassigned | P0-05, P1-06 | SearchService/SearchController, app.js | All + four categories, filter before Top-K, invalid value HTTP400 | Done; integration and browser verification |
| P2-01 | P2 | unassigned | P0-05 | SqliteMetadataRepository, MetadataDocument, db/V1__metadata.sql | Persistent Series/Alias/Document, repeatable migration/seeding tests | Done: 51-test suite; explicit offline seed/reseed; search integration remains separate |
| P2-02 | P2 | unassigned | P0-04, P2-01 | HttpTmdbClient, TmdbConfiguration, TMDB_ADAPTER.md | Env-only token, TV scope filtering, timeout/invalid-key tests | Partial: adapter/mock transport complete, 85 tests pass; live smoke and attribution/display pending |
| P2-03 | P2 | unassigned | P2-02 | SeriesAlias, future SeriesResolver | Verified five-language aliases resolve same canonical ID; ambiguity tests | Planned; demo aliases are fictional fixtures only |
| P2-04 | P2 | unassigned | P0-04 | WebSearchClient adapter | Permitted live URLs, metadata, rate limits, mock error tests | Blocked: provider selection/rights/key |
| P2-05 | P2 | unassigned | P1-06, P2-04 | service/merger/model provenance | Merge permitted live/local candidates preserving provenance and rights; mock tests | Planned |
| P2-06 | P2 | unassigned | P2-03, P2-05 | future domain filter | Reject non-TV/animation/unrelated pages; keep credible discussions | Planned; local demo corpus is scope-limited, no general live filter |
| P2-07 | P2 | unassigned | P0-03, P0-04, P1-06 | future permitted tree/BFS | Only if rubric requires; depth bound and permitted pages; tests | Conditional, blocked on rubric/rights |
| P3-01 | P3 | unassigned | P2-03, P2-05 | future fallback state machine | Bounded aliases/source retries and honest final no-result; state tests | Planned |
| P3-02 | P3 | unassigned | P3-01 | AiFallbackService adapter | Optional candidate terms only, request/token budget, validation and timeout tests | Planned |
| P3-03 | P3 | unassigned | P2-03 | evaluation cases | Reviewed cross-country five-language canonical-ID cases | Planned; local regression is not this evaluation |
| P3-04 | P3 | unassigned | P2-05 | evaluation/report | Human-labeled live ground truth, Precision@5/hit/no-result metrics | Planned; no fabricated metric |
| P3-05 | P3 | unassigned | P1-07 | static UI | Loading/error/empty, responsive and keyboard; actual browser checks | Partial: basic states/mobile/keyboard completed; full accessibility audit pending |
| P3-06 | P3 | unassigned | P0-03, P3-04 | future LLM comparison | Same query set, disclosed method and measured comparison | Conditional |
| P3-07 | P3 | unassigned | P3-04 | final report/demo | Reproducible runbook, actual screenshots, limitations, costs | Stage 1 report/runbook done; final project report planned |

## Next commands

Windows: `.\mvnw.cmd test`, then `.\mvnw.cmd spring-boot:run`.
Unix: `./mvnw test`, then `./mvnw spring-boot:run`.

P2-01 now has a separate stacked feature branch; see SQLITE_METADATA.md. Next implementation: P2-02 optional TMDB adapter with mock transport tests, then P2-03 verified real aliases. Do not mix unapproved live search into this local PR. In parallel the team can supply the actual teacher rubric and choose an authorized search plan. No teammate names or assignments are assumed.
