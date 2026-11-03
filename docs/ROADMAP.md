# Development Roadmap & Testing Plan

**Status:** planning; no implementation is claimed here.  
**Team:** 4–5 members | **Duration:** 4–6 weeks | **API budget cap:** NT$300–1,000 total.

## Build phases

| Phase | Target work | Acceptance criteria |
|---|---|---|
| 1. Feasibility (Week 1) | Java 17+ / Spring Boot skeleton, WebSearchClient interface, mock web results, research API permissions, test search coverage of PTT/Dcard | App launches; mock integration works; permitted source usage documented |
| 2. Series Data (Week 2) | TMDB adapter, SQLite schema, verified series and multilingual aliases; exclude non-live-action media | Search names and aliases in Chinese/English/Korean/Japanese for a selected sample; no unsupported series claims |
| 3. Local Search (Weeks 2–3) | Self-built HashMap-based Inverted Index using only licensed/self-authored content | Local search works without web API; tests cover tokenization and posting list |
| 4. Smart Ranking (Weeks 3–4) | Candidate merge, HashSet URL dedup, 45/30/15/10 scoring, PriorityQueue, category tabs | Test score calculations and deterministic ordering |
| 5. Fallback (Weeks 4–5) | Retry verified aliases, broaden permitted sources, invoke AI only if insufficient relevant results | AI output verified before search; no fabricated URLs, bounded API usage |
| 6. Quality & Demo (Weeks 5–6) | Hard-keyword test suite, cost tracking, latency, diagram/demo/report | Reproducible test report; realistic sample demo |

## Suggested team split

- **A — External search & permissions:** candidate web API, rate limits, domain coverage.
- **B — TV data & index:** SQLite, TMDB, validated aliases, Inverted Index.
- **C — Ranking:** URL dedup, score normalization, PriorityQueue, explanation.
- **D — Query fallback:** multilingual query expansion, AI boundary, API budgets.
- **E — Frontend & QA (if available):** tabs, search UX, integration tests, demo.

## Test matrix

Aim for 100 manually reviewed queries across:
- 15 full TV titles
- 15 translated titles and Chinese script variants
- 15 character names / nicknames
- 15 plot descriptions
- 15 rare titles
- 10 typos / abbreviations
- 10 ambiguous keywords
- 5 out-of-domain keywords

Include Korean/Japanese queries for US/Taiwan/China series and the reverse.

### Metrics
- Relevant Hit Rate: at least one verified relevant result among answerable queries
- Precision@5: relevant results among top five
- Zero-result Rate (report by answerable vs unanswerable queries)
- AI Fallback Success Rate, latency, API calls and cost

**Do not** count search suggestions as relevant results.

## Immediate next actions
1. Verify exact course rubric requirements regarding crawling, Tree/HTMLHandler, AI usage and final challenge tests.
2. Confirm external API license permits chosen display/merging/ranking.
3. Register TMDB and permitted search API; put secrets in environment variables only.
4. Prepare 20 manually labeled examples and implement Stage 1.
5. Make meaningful commits for each tested milestone, rather than empty activity commits.
