# Search algorithm hardening and offline efficiency experiment

## Correctness checks

- InvertedIndex: 800 deterministic mixed add/replace/remove operations are compared against an independent brute-force document scan after every operation. Empty replacements and missing tokens are included.
- PriorityQueue Top-K: 1,000 synthetic candidates, nonfinite/out-of-range factors, seven limit values and shuffled input orders are compared with an independent full-sort score oracle.
- Persistent index: a controlled in-progress rebuild keeps readers on the complete old snapshot; after publication the same alias switches entirely to the new canonical ID's documents.
- TMDB response identity: fractional IDs, numeric title fields, non-object rows and invalid country codes now fail with safe MALFORMED_RESPONSE rather than being coerced into a plausible identity.
- Tokenizer uses reusable compiled Patterns rather than recompiling identical patterns for every document/query. Unicode and existing search behavior tests remain unchanged.
- UI no-result hints now use the active collection's examples. The checked collection no longer suggests fictional titles. Error updates are guarded against superseded requests; curated results now explain alias-derived K/M and missing S/F.
- Java CI now runs the committed Maven Wrapper and full verify/package, so the pinned distribution/checksum is exercised on Linux/Java17 rather than relying only on the runner's Maven.

Full local tests after the production changes: **127 passed, zero failures/errors/skips**. Final verify/CI evidence is recorded in the stage report and delivery report.

## Reproduce the efficiency experiment

```powershell
.\mvnw.cmd compile
java --class-path target/classes evaluation/IndexBenchmark.java
```

The benchmark creates only original synthetic tokens: 100, 1,000 and 10,000 documents, each with three terms. Queries include a unique term, a common term and an absent term. Before timing, exact result sets are compared between the actual InvertedIndex and a direct scan. Each path warms 100 calls; each reported query value is the median of 11 batches of 100 calls. Both paths allocate/consume result sets. Build timing excludes corpus generation.

Measured on this Windows/JDK21.0.12.1 host after the compiled-pattern change:

| Documents | Build ms | Unique term index µs | Unique term scan µs | Common term hits | Common index µs | Common scan µs |
|---:|---:|---:|---:|---:|---:|---:|
| 100 | 2.814 | 6.769 | 2.419 | 1 | 6.309 | 1.578 |
| 1,000 | 10.097 | 2.485 | 19.427 | 10 | 2.638 | 5.417 |
| 10,000 | 52.372 | 1.465 | 57.836 | 100 | 4.499 | 63.232 |

Raw current-run data: evaluation/benchmark-sample.json. The original exploratory run is separately retained in delivery outputs. This single-process experiment is not JMH, an SLA or proof of a universal speedup: small data can favor a scan, JIT/GC/background activity affect results, sizes run sequentially, and no confidence interval or reliable heap measurement is claimed. Decreasing query timings as corpus size rises should NOT be interpreted as larger indexes inherently being faster. No live network/relevance/AI quality conclusions follow from synthetic timings.

The initial run exposed repeated regex compilation. The tokenizer was changed and the experiment rerun; because JVM warmup/process variation are not rigorously controlled, the two runs are not presented as a statistically established optimization win.
