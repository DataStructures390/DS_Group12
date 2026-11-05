# SceneAtlas TMDB live verification

The user reported successful authentication in their own PowerShell on 2026-11-06.
That environment belongs to that terminal; the agent and the already running application do not inherit it.
Do not paste credentials into chat, command arguments, tracked files, or logs.

## Verify the Java adapter

After packaging, run in the terminal holding the environment variable:

```powershell
java "-Dloader.main=com.group12.tvsearch.sources.TmdbVerifyCommand" -cp target/tvseries-search-0.1.0-SNAPSHOT.jar org.springframework.boot.loader.launch.PropertiesLauncher "Breaking Bad"
```

Expected: TMDB_VERIFY: SUCCESS and TV_ID. No credential or response body is printed.
At most two requests; no pagination, retry, storage, or billing setup.
NO_CONFIRMED_SERIES means no unique exact-title, in-scope match; it does not necessarily mean bad credentials.

## Start the website in the same terminal

The developer must first stop the known local server occupying port 8080.

```powershell
java -jar target/tvseries-search-0.1.0-SNAPSHOT.jar --search.collection=curated --search.local-demo.enabled=false --metadata.tmdb.enabled=true
```

Search Breaking Bad. The separate TMDB panel should show the verified original title, origin and canonical TV link.
The lower results remain curated local links, not live web search. Category changes do not re-request TMDB.
The independent metadata endpoint is GET /api/series?q=Breaking%20Bad.
Metadata errors do not block local results. Only one metadata resolution runs at a time; excess requests receive BUSY without queueing.
No global per-account quota or deployment authentication has been added; this remains a local development service.

## Limits and attribution

User selected a separate metadata panel on 2026-11-06.
No poster, synopsis, rating, inferred streaming availability or metadata persistence is added.
The conservative existing exact English/original-title resolver can miss translated titles and ambiguous remakes.
Verified multilingual API resolution remains separate work.

Official logo downloaded unchanged from:
https://www.themoviedb.org/assets/v4/logos/v2/blue_short-8e7b30f73a4020692ccca9c88bafe5dcb6f8a62a4c6bc55cd9ba82bb2cd95f6c.svg

Attribution checked at https://developer.themoviedb.org/docs/faq and https://www.themoviedb.org/about/logos-attribution.

Local tests: 139 passed, zero failures/errors/skips. User ran the packaged Java adapter in the credential-bearing terminal and reported TMDB_VERIFY: SUCCESS / TV_ID: 1396 for Breaking Bad on 2026-11-06. Browser live verification passed: Breaking Bad, origin US, canonical TMDB TV link 1396, and five local results displayed together.
