# Persistent local index and verified-alias groundwork

MetadataSearchIndex makes an explicit offline path from SQLite rows to the existing HashMap-based InvertedIndex, URL dedup and PriorityQueue ranking. It is a separate library/CLI; normal browser SearchService still uses the Stage 1 fixture provider. No external API, AI or automatic scrape is invoked.

## Identity and provenance

The index rebuild reads Series, verified Alias + evidence and permitted Document rows in one SQLite read transaction. Only aliases with verified=true enter the index; guessed/unverified alias records do not become search terms. Original titles also map to canonical IDs. A normalized exact name mapping to multiple IDs yields AMBIGUOUS_SERIES with no guessed results. An exact unambiguous title limits retrieved documents to that canonical ID, preventing shared-title tokens in other shows from overriding identity.

Nonexact keyword queries use documented OR retrieval. They are not a semantic plot resolver. Each document indexes its permitted heading/body, original series title and stored verified aliases. K/M are actual term-coverage features; source/date remain missing/zero. Category filtering precedes dedup/Top-K. A deterministic document-ID order governs first-wins canonical duplicate selection.

Repository evidence is persisted and exposed in the consistent snapshot. The implementation does not independently validate a claimed license or alias proof. Five-language tests use ORIGINAL FICTIONAL mappings explicitly authored for tests; they do not establish verified names of actual shows. P2-03 real-world dataset acceptance remains open.

## Rebuild behavior

A complete in-memory snapshot is published atomically only after successful read/build. Concurrent readers keep the old snapshot throughout rebuilding or on failure. Changes in SQLite are not automatically reflected until rebuild() is called. The current version rebuilds the whole collection; no incremental index persistence or polling job is added.

## Offline run (Windows)

First follow SQLITE_METADATA.md to build the classpath and seed data/demo.sqlite. Then:

```powershell
$metadataClasspath = 'target/classes;' + (Get-Content target/runtime-classpath.txt -Raw).Trim()
java -cp $metadataClasspath com.group12.tvsearch.sources.MetadataSearchCommand data/demo.sqlite 港灣燈火
```

Expected: PERSISTED_LOCAL_RESULTS: 4 local records, each 75.0 for a complete demo title match. The command emits UTF-8, checks that the database exists and does not silently create an empty database for a typo. Stored URLs are local fixture references in this demo; arbitrary stored URLs are not asserted to be verified external websites.

## Evidence and remaining work

Full suite: **97 tests, zero failures/errors/skips**, including 12 persistent-index cases for five-language fixture aliases, exclusion of guesses, ambiguity, exact-ID isolation, category/limit states, successful/failed rebuild, stable dedup and empty databases. Actual on-disk seeded SQLite search returned four local records. Parent TMDB PR Java17 CI passed (41671065050).

Next steps require actual verified alias sources and a decision on persistent provider integration into the web contract. Do not change normal app startup to ingest remote content or silently overwrite local data. Live retrieval rights, credentials, quality evaluation and full five-language dataset remain separate gates.
