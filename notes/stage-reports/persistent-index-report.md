# Session Summary — 2026-11-04 — Persistent local index

**Tasks:** P2-01 indexing bridge; P2-03 alias-resolution groundwork only.
**Branch:** feat/persistent-index-aliases, stacked on PR #3.
**Actual changes:** consistent SQLite snapshot of series/verified aliases/evidence/documents; complete off-thread-style build then atomic publication; exact canonical-ID isolation; explicit ambiguous/no-result states; existing ranking/dedup; offline search CLI with UTF-8 output. No automatic web integration.
**Files:** AliasEvidence, SqliteMetadataRepository snapshot reader, MetadataSearchIndex, MetadataSearchCommand, MetadataSearchIndexTest, docs/backlog.
**Tests:** .\mvnw.cmd test: 97 passed, zero failed/error/skipped, 11.395s. Includes 12 persistent-index cases. Actual existing work/demo-metadata.sqlite query 港灣燈火 produced four local records, each score75. Initial Windows CLI output encoding was corrected to explicitly emit UTF-8.
**Verified:** synthetic aliases in five languages converge on the same fixture documents; unverified guesses excluded; title collision returns ambiguity; failed rebuild preserves the last snapshot.
**Not complete:** real-world verified multilingual dataset, web SearchService persistence toggle, live external retrieval, retries/AI and final quality study. Mock/synthetic tests do not count as actual live coverage.
**Next command:** .\mvnw.cmd test. Review the stacked PR chain before merging/retargeting; no main changes were made.
