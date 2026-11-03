# Checked multilingual collection

This opt-in mode is a small real-series local collection, separate from the fictional Stage 1 demo. It connects the SQLite repository and rebuilt InvertedIndex to SearchService/API/UI. It makes NO live search or TMDB call.

## What was checked

On 2026-11-04, opened the linked Wikipedia language/variant pages and checked their titles, same-series identity and country facts. Three stable project canonical IDs are used; none is represented as a TMDB ID.

| Canonical ID | Original title | Origin | Traditional / Simplified | Japanese | Korean |
|---|---|---|---|---|---|
| 100001 | Breaking Bad | US | 絕命毒師 / 绝命毒师 | ブレイキング・バッド | 브레이킹 배드 |
| 100002 | Stranger Things | US | 怪奇物語 / 怪奇物语 | ストレンジャー・シングス 未知の世界 | 기묘한 이야기 |
| 100003 | Dark | DE | 闇 / 暗黑 | ダーク | 다크 |

All 15 evidence URLs are stored directly alongside locale/title facts in src/main/resources/data/curated-series.json. These are **15 language/variant views of 12 underlying articles**, because Traditional/Simplified are views of the same Chinese article. They are not 15 independent publishers or live search discoveries. The initial guessed Dark Chinese disambiguation URL was 404; it was excluded and replaced using the actual interlanguage link to /闇 before inclusion.

The result heading identifies the article/title and locale. Bodies are short ORIGINAL project-authored navigation notes containing minimal country/type facts, not copied Wikipedia summaries, full articles or search API snippets. Metadata evidence preserves source URL and check date. Linked articles retain their own licenses and attribution; no images or full bodies are ingested. This limited use does not approve general Wikipedia ingestion or unrelated third-party sources.

Evidence entry points:

- [Breaking Bad](https://en.wikipedia.org/wiki/Breaking_Bad), [繁體](https://zh.wikipedia.org/zh-tw/绝命毒师), [简体](https://zh.wikipedia.org/zh-cn/绝命毒师), [日本語](https://ja.wikipedia.org/wiki/ブレイキング・バッド), [한국어](https://ko.wikipedia.org/wiki/브레이킹_배드).
- [Stranger Things](https://en.wikipedia.org/wiki/Stranger_Things), [繁體](https://zh.wikipedia.org/zh-tw/怪奇物语), [简体](https://zh.wikipedia.org/zh-cn/怪奇物语), [日本語](https://ja.wikipedia.org/wiki/ストレンジャー・シングス_未知の世界), [한국어](https://ko.wikipedia.org/wiki/기묘한_이야기_(미국의_드라마)).
- [Dark](https://en.wikipedia.org/wiki/Dark_(TV_series)), [繁體](https://zh.wikipedia.org/zh-tw/闇), [简体](https://zh.wikipedia.org/zh-cn/闇), [日本語](https://ja.wikipedia.org/wiki/ダーク_(テレビドラマ)), [한국어](https://ko.wikipedia.org/wiki/다크_(드라마)).

## Run in Windows / VS Code

Use workspace task **Run checked multilingual collection**, or after packaging:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/tvseries-search-0.1.0-SNAPSHOT.jar --search.collection=curated --search.local-demo.enabled=false
```

For direct Maven execution:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--search.collection=curated --search.local-demo.enabled=false"
```

First explicit curated-mode startup creates data/curated.sqlite and imports the checked catalog transactionally. Later startup reads existing data and validates its full contents against the bundled catalog. It refuses modified, demo or unrelated databases without overwriting them. If the curated catalog changes, explicitly select a NEW dedicated path with --search.curated.database=data/curated-v2.sqlite rather than overwrite contributor data. SQLite files remain ignored by Git. Default search.collection=demo keeps the original fictional demonstration.

GET /api/collection tells the UI which mode is active and supplies accurate banner/examples. CURATED_LOCAL_RESULTS identifies the checked local collection. Five aliases of one title return the five linked views for that canonical ID. Only SERIES_INFORMATION is populated; discussions, reviews and platforms correctly return NO_RESULTS. No playback availability or professional reviewer is invented.

## Verification and limits

Full `clean verify`: **118 tests, zero failures/errors/skips**, 9.387s on Java21/release17. Fifteen real title queries verify stable IDs and exact stored URL sets. Tests also cover cross-country/language behavior, mode separation, category empty states and refusal to relabel a fictional/modified database.

Browser: Korean Stranger Things query returned actual English/Japanese/Korean/Chinese links, and the English result opened the actual Wikipedia page. Japanese ダーク query returned the German series. At 390px, document width was 375px without horizontal overflow. Saved desktop/mobile screenshots to delivery outputs.

This is a deliberately tiny curated sample, not broad/global coverage, a live provider, a semantic plot resolver or a statistically valid quality benchmark. No precision/recall or LLM advantage is claimed from the 15 alias checks. K/M derive from stored fields; S/F remain zero. The check date is NOT treated as article publication freshness. Rights/credentials for live retrieval and the PTT/Dcard experiment remain unresolved.
