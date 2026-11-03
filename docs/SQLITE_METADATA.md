# SQLite metadata foundation — P2-01

Implemented separately from the local-search PR. Schema/repository are usable but are not yet connected to SearchService; normal app startup does not create a database, seed fictional rows or read external sources.

## Contract

SqliteMetadataRepository(Path) creates parent directories and applies transactional V1 DDL on a new database. PRAGMA user_version=1 records migration state; reopening is repeatable, and schema versions above 1 are rejected. Foreign keys and a bounded busy timeout are enabled per connection. All operations use prepared statements; no SQL is constructed from titles or aliases.

- Series: existing Series record, positive canonical ID, original title, origin country independent of query language, optional unique external ID. saveSeries requires explicit caller confirmation of live-action scripted scope. Actual TMDB genre/type validation is P2-02, not supplied by this boolean.
- Aliases: existing SeriesAlias record with normalized NFKC/lowercase lookup key, locale, verified flag and evidence. Verified entries require nonblank evidence. Unverified guesses cannot resolve a title. Colliding verified titles return all candidate IDs in stable order, never an arbitrary winner. This persists caller evidence; it does not independently verify that evidence.
- Documents: MetadataDocument contains canonical series ID, URL, category, body, provenance and license. Nonblank usage evidence is required. This is not permission to ingest a third-party body merely by entering a license string.
- Unique external IDs/URLs prevent different records from being silently combined. UPSERT preserves existing dependent rows. Every connection closes after its operation.
- Demo seeding runs in one transaction and can be repeated. It refuses a database containing a non-DEMO origin to prevent demo IDs overwriting real metadata. Use a dedicated demo database. Re-seeding deliberately refreshes project demo rows, so do not store user edits in that database.

## Offline seed utility (Windows)

```powershell
.\mvnw.cmd test
.\mvnw.cmd dependency:build-classpath '-Dmdep.outputFile=target/runtime-classpath.txt'
$metadataClasspath = 'target/classes;' + (Get-Content target/runtime-classpath.txt -Raw).Trim()
java -cp $metadataClasspath com.group12.tvsearch.sources.MetadataSeedCommand data/demo.sqlite
```

The command prints 12 original fictional documents and makes no external request. Run it twice to confirm idempotent seeding. SQLite files are ignored by Git. Unix uses ':' rather than ';' as the Java classpath separator.

## Evidence

SqliteMetadataRepositoryTest covers persistence/reopen/migration version, orphan constraints, evidence requirements, unverified aliases, alias collisions/fullwidth normalization/SQL-looking input, unique external IDs/URLs, child-preserving updates, repeatable seeding, protection of non-demo metadata, rollback and future-schema rejection. Full suite: **51 tests, zero failures/errors/skips**, JDK21 with release17. An actual on-disk seed/reseed was also run under the parent work directory; its result is recorded in the stage report.

Dependency: [xerial sqlite-jdbc 3.50.3.0](https://github.com/xerial/sqlite-jdbc/releases/tag/3.50.3.0). Migration/foreign-key behavior follows [SQLite PRAGMA](https://www.sqlite.org/pragma.html) and [foreign-key documentation](https://www.sqlite.org/foreignkeys.html).

## Remaining boundary

No live TMDB data, verified real-world alias dataset, search-index rebuild integration or live document ingestion is included. A short fixed-DDL migration loader is intentionally used for V1; introduce a proper versioned migration strategy when adding V2, and do not edit V1 for existing databases. Cross-process concurrent first-time migration is not a supported workflow; initialize once before multiple app instances. Future alias resolution must explicitly handle ambiguity.
