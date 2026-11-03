# Session Summary — 2026-11-03

**Task IDs:** P0-01, P0-02, P0-05, P1-01 through P1-07; P0-04 partial.
**Branch:** feat/stage1-local-search (base main 2b98cec).

## Verified baseline

Fresh clone from https://github.com/DataStructures390/DS_Group12.git. Branch main clean before edits. Read specification, roadmap, root and all per-module TODOs, Java source/tests and CI. No checkout AGENTS.md. Java 21.0.12.1; Maven absent from PATH. Downloaded official Maven 3.9.11 into parent work/tools. Original `mvn test` equivalent: 6 tests, 0 failures/errors/skips. No live integrations or SQLite existed.

## Actual changes

- Twelve original fictional documents across three demo series and four categories, with canonical demo IDs and local text routes. No copied third-party bodies or invented external URLs.
- Actual InvertedIndex connected through LocalSearchProvider and DefaultSearchService; original NoOp web client unchanged. Demo can be disabled via LOCAL_DEMO_ENABLED=false; document routes then return 404.
- K/M calculated from token coverage in actual fields; missing S/F explicitly zero. Score explanation, keyword-sensitive ordering, deterministic score/URL ties, bounded Top-K.
- Conservative HTTP(S) URL dedup; preserves query identity and percent encoding. Expanded Unicode/tokenization/invalid-input/limit/ranking tests.
- API category filter before Top-K; explicit EMPTY_QUERY/NO_RESULTS/LOCAL_DEMO_RESULTS/NO_LIVE_SEARCH. Live/local merging deferred.
- UI displays result cards and real local links, score explanations, four filters, loading/error/empty states, aborts stale requests, uses textContent and HTTP(S)/local link checks.
- Maven Wrapper 3.3.4 pins Maven 3.9.11 with distribution SHA256. Supplied VS Code workspace has test/run tasks; Java extension pack is recommended, not auto-installed.
- Execution backlog, minimal contract, source feasibility matrix, local runbook and local evaluation description; only narrow evidenced TODO milestones checked.

## Files touched

model/LocalDocument.java, model/SearchResult.java; sources/LocalDocumentRepository.java and LocalSearchProvider.java; service/DefaultSearchService.java and SearchService.java; controller/SearchController.java and LocalDocumentController.java; indexing/Tokenizer.java; ranking/RankingAlgorithm.java and ResultMerger.java; application.properties and static HTML/CSS/JS; test indexing/ranking/service; Maven Wrapper; .env.example; README/TODO/module TODOs; docs and evaluation/LOCAL_REGRESSION.md. See git diff/PR for the exact reviewable list.

## Commands and outcomes

- Isolated Maven `test`: original 6 pass.
- Isolated Maven `package`: initial local implementation packaged, 22 tests pass.
- Wrapper generation: first PowerShell invocation split an unquoted -Dmaven=3.9.11 argument; reran with quoted property and succeeded. No build failure is hidden.
- `.\mvnw.cmd -B --no-transfer-progress test`: expanded suite **44 tests, 0 failures, 0 errors, 0 skipped**, 9.531s on this host. Includes 20 functional fixture query cases.
- `node --check src/main/resources/static/app.js`: passed.
- `git diff --check`: no whitespace errors (Git reports expected Windows LF/CRLF conversion notices).
- Final packaging/current-HEAD CI evidence is recorded in the delivery report after the final check; never infer it from older runs.

## Browser checks

Ran packaged app at localhost:8080. Chinese 港灣燈火 showed 4 local records; Professional Reviews filter showed 1. Score disclosure displayed K/M=100 and S/F=0. Local info link opened UTF-8 text with an explicit fictional-data notice. Unknown 芒果 showed zero results and suggestions. At 390px viewport, measured document width 375px (no horizontal overflow). Saved desktop/mobile screenshots in the parent outputs folder. Restored normal viewport. This is basic responsive/functional QA, not a comprehensive accessibility/performance audit.

## Unfinished / blocked

No selected authorized live search provider/key; the 20 PTT/Dcard API coverage experiment is NOT RUN. PTT terms request returned 403; Fandango terms redirected to a region error, both recorded without bypass. Brave plan-specific storage/mixed ranking rights remain unresolved. TMDB, SQLite, verified real-world five-language aliases, full scope filtering, bounded retry/AI, live quality metrics and final course report are not complete. Teacher rubric/member assignments are unavailable. No API credentials or paid accounts were created.

## Known limits

Lexical OR retrieval can return partial matches and is not a semantic resolver. Demo M uses fictional title/alias token evidence, not real identity verification. Japanese/Korean bigram smoke tests do not establish production language coverage. Missing source/date scores cap a full title match at 75. The new UI does not claim genuine professional reviewers or streaming availability. Local-first policy does not merge live results. New CI has to run on the feature PR.

## Next exact command

`.\mvnw.cmd test`

Then `.\mvnw.cmd spring-boot:run`. Next independent backlog task: P2-01 SQLite schema/repository on a separate branch after review; provider selection/rubric confirmation remain external inputs.

## Final build gate

The first final verify attempt failed because the running demo JVM held the JAR open on Windows. Stopped only the verified demo processes (29112 launcher, 37484 JVM), then ran .\mvnw.cmd -B --no-transfer-progress clean verify: BUILD SUCCESS, 44 tests, zero failures/errors/skips, 6.035s. git diff --check is clean after removing an extra trailing blank line. This local success does not assert GitHub CI status.
