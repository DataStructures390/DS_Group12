# Local search runbook

Requires JDK 17+ and network on the first build (Maven/dependencies). No installed Maven or API key required: use the committed Maven Wrapper.

## Windows / VS Code

Open the supplied DS_Group12.code-workspace or open the repository folder in VS Code. In Terminal:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Open http://localhost:8080. Try 港灣燈火, Harbor Lights, 午夜花園, Mountain Train, 攝影. Use the four category buttons and score explanation. Click a result to read the actual local text document. All titles/content are fictional test fixtures, including platform/review categories.

VS Code workspace tasks: **Test Java project** and **Run local search**. Java extensions are recommendations only; no extension was silently installed. Stop the foreground server with Ctrl+C.

Linux/macOS: `./mvnw test` and `./mvnw spring-boot:run`.

## API examples

- http://localhost:8080/api/search?q=Harbor%20Lights
- http://localhost:8080/api/search?q=Harbor%20Lights&limit=1&category=STREAMING_PLATFORM
- http://localhost:8080/demo/documents/harbor-info

Categories: COMMUNITY_DISCUSSION, SERIES_INFORMATION, PROFESSIONAL_REVIEW, STREAMING_PLATFORM. Omit category for all. Limits clamp to 1–50. An unknown category or noninteger limit returns HTTP400.

To package: `.\mvnw.cmd package`, then `java -jar target/tvseries-search-0.1.0-SNAPSHOT.jar`.
If port 8080 is occupied, `java -jar target/tvseries-search-0.1.0-SNAPSHOT.jar --server.port=8081`.

To disable fixtures in PowerShell:

```powershell
$env:LOCAL_DEMO_ENABLED='false'
.\mvnw.cmd spring-boot:run
```

This returns NO_LIVE_SEARCH for unmatched searches and disables /demo/documents routes. Reset with `Remove-Item Env:LOCAL_DEMO_ENABLED` before the next local-demo launch. The UI remains explicitly a demo preview.

No external provider is enabled. Configuring an arbitrary key alone does not activate a provider. The app does not auto-read .env files; actual future adapters must read approved environment configuration. Read SOURCE_POLICY.md before live integration.

## What is tested

See stage report for actual test count/commands. JUnit verifies index, normalization, scoring, dedup, API category/status and demo disable behavior. A twenty-query fictional-corpus regression set is not a live relevance benchmark, nor an independently reviewed multilingual identity dataset.
