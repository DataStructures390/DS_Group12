# Group 12 — Codex Development Work Order

> **Repository:** https://github.com/DataStructures390/DS_Group12  
> **Project:** TV Series Search Engine with Smart Ranking & AI-Assisted Query Expansion  
> **Document version:** 2026-11-03 · Initial orchestration plan  
> **Intended reader:** Codex (coding agent) and all Group 12 contributors  
> **Status:** An execution brief, **not** a claim that the listed features are complete.

---

## 0. Start here: Instructions to Codex

You are working **inside the existing** \`DataStructures390/DS_Group12\` repository. Your job is to **inspect, plan, implement and verify the next smallest useful increments**, not generate decorative files or rewrite the project from scratch.

**On your first run, perform the following in order:**

1. Confirm that the Git remote points to **DataStructures390/DS_Group12**, then inspect the active branch and uncommitted changes. Never assume you are in the correct repository.
2. Read \`README.md\`, \`docs/PROJECT_SPEC.md\`, \`docs/ROADMAP.md\`, \`TODO.md\`, and every relevant folder's \`TODO.md\`.
3. Inspect \`pom.xml\`, \`src/main\`, \`src/test\`, and \`.github/workflows/java-ci.yml\`. **Reuse existing classes** unless a change is justified.
4. Run \`mvn test\` in a working Java 17+ / Maven environment. If Maven is unavailable, clearly report that the test could not run; do not mark it passed.
5. Compare actual code with documentation. Build a short **verified baseline** of what works, what is only scaffolded, and what is blocked by external APIs or permissions.
6. Write actionable work items in \`docs/EXECUTION_BACKLOG.md\` with: ID, priority, owner = unassigned, dependent tasks, affected files, acceptance criteria, tests and status. **If the file already exists, update it without discarding contributors' edits.**
7. Create a working branch (e.g. \`feat/stage1-local-search\`) for implementation. Do not force-push or casually rewrite \`main\`.
8. Complete **one minimal, end-to-end, locally testable feature** from the P0/P1 backlog. Add JUnit tests, run them, and provide a clear commit/PR or change summary.
9. Update only the TODO checkboxes backed by actual test or review evidence. Record remaining work and the exact next command for another developer to continue.
10. Continue with the next independent high-priority task only when it will not disrupt other contributors.

**Default first implementation focus:** make the current local \`InvertedIndex\` actually usable through \`SearchService\` against a **small, self-authored/licensed test collection**, with tests. Leave all unapproved external web sources as \`NoOp\`.

**Do not ask the team to restate information already documented in this repo.** If multiple choices remain undecided, make the simplest reversible implementation consistent with the specs and record the assumption.

---

## 1. Non-negotiable product scope

1. **Target:** live-action scripted television series and miniseries from any country.
2. **Exclude:** films, animation/anime, reality TV, variety shows and other out-of-scope media.
3. **Languages:** Traditional Chinese, Simplified Chinese, English, Japanese and Korean.
4. **Cross-language & cross-region:** query language, series origin country and page language are independent. One verified show identity should resolve via canonical series/TMDB ID and validated aliases.
5. **Output:** actual, clickable, verifiable webpage links with source and category; never fabricate URLs or present query suggestions as results.
6. **Categories:** All / Community Discussions / Series Information / Professional Reviews / Streaming Platforms.
7. **Core architecture:** permitted external Web Search API + local SQLite metadata and **self-implemented Inverted Index**.
8. **Data structures:** HashMap, HashSet, ArrayList, PriorityQueue; evaluate Tree/BFS/Stack/Queue only where valuable or required by the confirmed grading rubric.
9. **Initial Smart Ranking:** \`0.45 * Keyword + 0.30 * Series + 0.15 * Source + 0.10 * Freshness\`, with all factors normalized to 0–100. Weights are provisional and must be tested.
10. **Fallback:** normal search → verified multilingual aliases and relevant re-query → broaden permitted sources → optional AI-generated **candidate keywords only** → verify candidates with actual metadata/search → honest no-result response if unsuccessful.
11. **Budget:** 4–5 contributors, roughly 4–6 development weeks, combined API budget approximately NT$300–1,000.
12. **Sources under consideration:** TMDB, Wikipedia, PTT, Dcard, Rotten Tomatoes, Douban, and official streaming sites, **only where access and use are allowed**.

Read \`docs/PROJECT_SPEC.md\` as the authority for currently agreed product behavior. Do not silently shrink it to a single-series search engine or expand it into a chatbot.

---

## 2. Baseline confirmed on 2026-11-03

These statements distinguish **existing source code** from **verified behavior**.

| Area | Repository state | Honest status |
|---|---|---|
| Specification / roadmap / README | Markdown documents exist | Documented |
| Per-module TODOs | TODO files exist in key directories | Documented |
| Project skeleton | \`pom.xml\`, Java 17 / Spring Boot source, static frontend, GitHub Actions | Source code present |
| Local index | \`indexing/InvertedIndex.java\`, \`Tokenizer.java\` | Basic in-memory index exists, **not wired into live search** |
| Ranking | \`RankingAlgorithm.java\`, \`ResultMerger.java\` | Basic weighted scoring, heap Top-K, exact-URL dedup source exists |
| API | \`GET /api/search?q=...&limit=...\` | Stage 0 returns explicit \`NO_LIVE_SEARCH\` when the provider is \`NoOp\` |
| Source adapters | \`WebSearchClient\`, \`NoOpWebSearchClient\`, \`TmdbClient\` | Interfaces exist; **no live integrations** |
| UI | \`index.html\`, CSS, JS | Stage 0 technical preview; **not final search UI** |
| Tests / CI | JUnit source exists; prior GitHub Actions Java CI runs completed successfully | **Re-run at current HEAD**; do not assume future changes pass |
| SQLite / alias resolver | Documented, not integrated | Not complete |
| AI fallback | Interface only | Not complete |

Prior successful GitHub Actions checks on 2026-11-03: \`29b5d92\` and \`baeba04\`. This is historical evidence, not a replacement for running tests after every subsequent code change.

**Important:** The current README and roadmap may contain planning-stage statements. Treat actual source code and a freshly executed build as the implementation baseline.

---

## 3. Workstreams and owners

Do **not** invent actual team member names or assign work to named people without explicit instruction. Use labels until members claim tasks.

| Workstream | Suggested owner | Files / deliverables | Can progress independently? |
|---|---|---|---|
| A. Java quality + architecture | Member A (unassigned) | Maven CI, interface contract, test fixtures, error handling | Yes; coordinate shared interfaces |
| B. TV metadata + language aliases | Member B (unassigned) | SQLite schema, TMDB adapter, SeriesResolver, alias tests | Yes, after model contract |
| C. Index + ranking | Member C (unassigned) | InvertedIndex integration, scoring features, URL dedup, JUnit | Yes, using fixtures |
| D. Source retrieval + legal review | Member D (unassigned) | Provider matrix, permitted Web Search adapter, rate limiting | Yes; start with feasibility research |
| E. Frontend, evaluation + fallback | Member E if available (unassigned) | Category tabs, error states, query test set, AI fallback last | Mock API first, no need to wait for live API |

For a four-person team, combine A+E or D+E based on competence. **Interfaces and acceptance criteria should be agreed before parallel implementation.**

### Shared contracts to stabilize first

- \`Series\` identity and alias source-of-truth.
- \`SearchResult\` canonical URL, category, series match, provenance and score components.
- \`SearchService.search(query, limit)\` behavior and response statuses.
- \`WebSearchClient\` contract for timeouts, permissible result fields and no-result states.
- \`InvertedIndex\` legal document representation and integration boundary.

Avoid changing public interfaces in one branch while others depend on them without notifying collaborators.

---

## 4. Prioritized implementation backlog

**Priorities:** P0 = must do before dependent work; P1 = core MVP; P2 = quality, completeness; P3 = stretch/rubric-dependent.

### P0 — Baseline and decisions

| ID | Task | Dependencies | Acceptance criteria |
|---|---|---|---|
| P0-01 | Inspect code and run current Maven tests | None | Record \`java -version\`, \`mvn test\` outcome; fix regressions only if reproducible |
| P0-02 | Build task board from all TODOs | P0-01 | Create \`docs/EXECUTION_BACKLOG.md\` with IDs, evidence, owner, status and links |
| P0-03 | Confirm teacher's actual rubric / Stage requirements | Teacher input | Track Tree/BFS/HTML parsing/LLM comparison as **unverified requirements** until confirmed |
| P0-04 | External API rights and feasibility matrix | None | Compare official terms and technical feasibility for display, storage, ranking, paging, PTT/Dcard coverage and costs |
| P0-05 | Freeze minimal model / service interfaces | P0-01 | Record fields, errors, URL/category conventions and tests; avoid broad rewrites |

### P1 — Implement a functioning local-search MVP

| ID | Task | Dependencies | Acceptance criteria |
|---|---|---|---|
| P1-01 | Add legal local document fixtures | P0-01 | At least 8 **self-authored** test docs with explicit series IDs, category, URL or locally testable source; no third-party copied bodies |
| P1-02 | Improve \`Tokenizer\` + Index tests | P1-01 | English/CJK basics, duplicates, replacement/removal, null/empty, AND/OR or documented OR behavior; test pass |
| P1-03 | Connect index to \`SearchService\` | P0-05, P1-01 | Query to indexed document(s) through one service; no web API/network required |
| P1-04 | Real relevance feature computation for local results | P1-03 | K/M/S/F derived from available permitted fields, not hard-coded fake feature scores; explain missing data |
| P1-05 | Ranking, dedup & Top-K tests | P1-04 | Deterministic tie-breaking, canonical URL strategy, clamping, limits, collision tests and sorted results |
| P1-06 | End-to-end demo path | P1-03, P1-05 | \`mvn test\` passes; app exposes actual **local** results in JSON and UI; clearly labeled local demo data |
| P1-07 | Search result categories | P0-05, P1-06 | All / Discussions / Info / Reviews / Platforms filter with fixture-based tests |

**Definition of MVP:** An actual user query returns relevant, ranked, categorized results from legally included local documents, without pretending to query PTT/Dcard or the live web.

### P2 — TV-series identity, multilingual coverage and live retrieval

| ID | Task | Dependencies | Acceptance criteria |
|---|---|---|---|
| P2-01 | SQLite schema + repository layer | P0-05 | Persistent Series/Alias/Document records; repeatable migrations/seeding; tests |
| P2-02 | TMDB adapter | P0-04, P2-01 | Optional secret from environment, TV vs movie filtering, timeouts, invalid-key errors, no logs of keys |
| P2-03 | Verified multilingual alias map | P2-02 | Traditional/Simplified/English/Japanese/Korean tested; same canonical series ID; prevent wrong-title collisions |
| P2-04 | Web Search provider adapter | P0-04 | Only after terms permit intended use; real URLs, rate-limits and metadata, no prohibited storage/re-ranking |
| P2-05 | Merge local and permitted live candidates | P1-06, P2-04 | Dedup, provenance, category, permissions and score features preserved; test with mocks |
| P2-06 | Site relevance/domain filtering | P2-03, P2-05 | Avoid irrelevant films, anime and unrelated pages; preserve credible discussions |
| P2-07 | Local webpage Tree/BFS extension | P0-03, P0-04, P1-06 | Only if teacher requires it and content/site policies allow; bounded depth, no unrestricted crawling |

**Never hard-code API keys in code, Maven files, CI, commits or logs.** If credentials are not available, finish provider interfaces and mock tests; explicitly leave live API tasks blocked.

### P3 — AI fallback, quality and delivery

| ID | Task | Dependencies | Acceptance criteria |
|---|---|---|---|
| P3-01 | Multi-step no-result retry state machine | P2-03, P2-05 | Query → verified aliases → additional permitted sources → final no-result, bounded attempts |
| P3-02 | Optional OpenAI keyword fallback | P3-01 | Only runs on insufficient verified results, capped tokens/requests, validates suggestions before use |
| P3-03 | Cross-language adversarial evaluation | P2-03 | Reviewed tests with multilingual queries resolving across origin countries |
| P3-04 | Rank quality study | P2-05 | Precision@5, relevant-hit rate, zero-result rate with labeled ground truth |
| P3-05 | UX polish and accessibility | P1-07 | Loading/error/empty states, mobile, keyboard, no invented metadata |
| P3-06 | LLM comparison (optional) | P0-03, P3-04 | Compare same queries, disclose differences and measurement method, don't fabricate wins |
| P3-07 | Final report and demo | P3-04 | Run instructions, screenshots of actual behavior, diagrams, limitations and cost report |

---

## 5. What Codex should do in its FIRST implementation session

Do not only produce another plan. **After the baseline review, implement the smallest real slice.**

### Session A — Make local search actually work

1. Inspect \`InvertedIndex\`, \`Tokenizer\`, \`DefaultSearchService\`, \`SearchResult\`, the controller and tests.
2. Write 8–12 self-authored, factual but synthetic local testing documents. Use clearly controlled fixture URLs (for example, \`https://example.org/demo/series-1\`) **only in test fixtures and always label them as fixtures**. They must never be shown as genuine Internet search results.
3. Add a \`LocalDocumentRepository\` or equivalent and a \`LocalSearchProvider\` adapter that queries the actual Inverted Index. Prefer small interfaces that the team can test.
4. Connect the local provider through \`SearchService\` without breaking \`NoOpWebSearchClient\` behavior. Return a distinct status like \`LOCAL_DEMO_RESULTS\`.
5. Implement one transparent keyword scoring mechanism based on *actually indexed legal fields*. Preserve the existing 45/30/15/10 ranking skeleton but do not invent computed attributes or mark them as final.
6. Update the HTML/JS page to display these **explicitly labeled local demo** results, including source/category and whether the URL is an external verified resource or demo fixture. Don't make fixture URLs look like verified websites.
7. Add tests for basic English and Chinese queries, empty queries, deduplication, score ordering, negative/out-of-scope cases and API response statuses.
8. Run the tests, inspect diffs and stage only intended files; update the relevant TODOs and write \`notes/stage-reports/stage0-local-search-report.md\` with **actual** results.
9. Commit on a feature branch and open a PR if repository permissions/workflow permit. If unable, provide a precise change summary and commands to commit.

**Stop condition for Session A:** A truthful, reproducible local search demonstration works and \`mvn test\` is green, without real API credentials.

### Session B — Metadata groundwork (can run in parallel on a different branch)

- Finalize SQLite schema and seed structure, with no unauthorized copyrighted data.
- Implement TMDB adapter behind an interface, after checking official API requirements.
- Add verified alias records and ID mapping; use mock TMDB data if no key is available.
- Add tests for source-country ≠ query-language cases.
- Do not merge a broken or unaudited API integration.

### Session C — Search source feasibility and ranking review

- Prepare \`docs/SOURCE_POLICY.md\`: for each prospective source, document actual rights/terms, what is permitted, cost, quota, and fallback.
- Examine whether the selected Web Search API allows **re-ranking and presenting** results in the planned way. If not, redesign the adapter; do not attempt to bypass provider terms.
- Improve ranking unit tests and explainability while waiting for live provider approval.

---

## 6. Dependency map and schedule

A four-to-six-week **relative** plan; revise with the team's actual deadline. Do not copy another group's dates as commitments.

| Week | Primary target | Evidence |
|---|---|---|
| 1 | Verified Stage 0, task board, local MVP, source feasibility | Tests/CI, local search output and source policy document |
| 2 | SQLite + TMDB foundation, multilingual aliases | Migration tests, cross-language ID tests |
| 3 | Local index improvements and lawful live search adapter | Search and ranking comparisons, API error tests |
| 4 | Full ranking and category UI, bounded retries | UI screenshots, precision sample |
| 5 | Conditional AI fallback, hard cases, performance | Fallback tests, measured query costs |
| 6 | Relevance evaluation, report, deployment/demo | Repeatable runbook and results table |

If a required API is delayed, prioritize local indexing, ranking, test corpus, diagrams and mock-based UI. Do **not** block every workstream waiting for a key.

---

## 7. Quality bar — Definition of Done

A task is not done unless:

- [ ] The change is based on a clear requirement in \`PROJECT_SPEC.md\` or a documented, justified new decision.
- [ ] The exact touched files, dependencies and input/output behavior are documented.
- [ ] Java 17+ compilation and relevant JUnit tests actually run and pass, or the limitation is explicitly recorded.
- [ ] Tests include at least one positive, one negative and one edge case where applicable.
- [ ] No secret, personal credential, private data, disallowed web copy or fabricated result is committed.
- [ ] No unrelated refactor or destruction of another member's changes.
- [ ] Corresponding TODO status is updated based on evidence, not optimism.
- [ ] User-facing result labels accurately distinguish local fixture, verified metadata and permitted live web result.
- [ ] Commit is meaningful, has a descriptive message and can be reviewed.

### Suggested branch and commit pattern

\`\`\`text
feat/local-search-service
feat/tmdb-series-resolver
feat/verified-aliases
feat/search-provider
feat/ranking-features
test/query-regression
docs/stage-report

feat(index): connect InvertedIndex to local search adapter
test(search): cover missing and multilingual queries
docs: record provider usage restrictions
\`\`\`

Prefer PRs with a short summary, test commands and screenshots for UI changes. Protect \`main\` from unstable merges. **Never create empty commits or daily cosmetic edits solely to inflate contribution count.**

---

## 8. Guardrails and engineering principles

### Source legality
- A website being public does not automatically authorize scraping, copying full articles, caching API results or presenting their reordered snippets.
- TMDB is for identifying TV series and metadata, **not** for retrieving PTT/Dcard discussion posts.
- Querying search APIs, using snippets, caching and re-ranking must follow provider-specific terms. Keep data source provenance.
- No attempts to evade CAPTCHA, authentication, rate limits or robots/publisher restrictions.
- Fixture URLs are test data; label them as such. Never claim a fabricated URL exists online.

### AI usage
- AI is **not** a search provider or final ranking oracle.
- Restrict AI to proposals for candidate shows, aliases or queries when deterministic search is insufficient.
- Verify candidates against canonical series IDs and a permitted retrieval source before showing them.
- Add retry and token budgets, timeouts and invalid-response tests.

### Java and data structures
- Keep core search and ranking algorithms in Java, not hidden inside a paid API.
- Be able to show where HashMap, HashSet, Inverted Index and PriorityQueue are actually used.
- If Tree/BFS or Stack are formally required by the teacher, implement them as a real tested behavior rather than dummy classes.
- Estimate time/space complexity and test multilingual tokenization collisions.

### Scope control
- Prefer a small working slice over an elegant architecture with no working search.
- Do not replace the existing project with another group's code or reproduce their copyrighted written material.
- Make new design decisions explicit before changing core agreed product behavior.
- Do not invent a member's identity, deadline, report result, API authorization or measured performance.

---

## 9. Expected outputs from each Codex session

Conclude each session with the following Markdown summary (write it into the PR description or \`notes/\` when useful):

\`\`\`md
## Session Summary — YYYY-MM-DD

**Task IDs:** P1-01, P1-03
**Branch / Commit / PR:** ...
**Actual changes:** ...
**Files touched:** ...
**Commands run:** ...
**Test results:** ... (with pass/fail or unable-to-run)
**Verified behavior:** ...
**Unfinished or blocked:** ...
**Risk / known limitation:** ...
**Next highest-priority task:** ...
\`\`\`

Update \`docs/EXECUTION_BACKLOG.md\` and the relevant \`TODO.md\` for each completed milestone.

---

## 10. First message to send Codex

Copy and send the following message to Codex after it has access to this repository:

> 請開啟 \`DataStructures390/DS_Group12\`，先完整讀取根目錄的 \`CODEX_WORK_PLAN.md\`，並依照第 0、4、5 節開始工作。先盤點現有程式、確認 \`mvn test\`，建立可追蹤的執行 Backlog；接著在新的 feature branch 實作第一個真正可測試的本地影集搜尋功能。請保留現有程式與規格、不使用假的網頁搜尋結果、不提交任何 API Key；每完成一個項目都附上實際測試與 Git 變更紀錄。如果無法執行任何步驟，請明確記錄原因與下一步，而不是宣稱完成。

**Start with P0-01 → P0-02 → P1-01 → P1-03 → P1-04 → P1-06.**
