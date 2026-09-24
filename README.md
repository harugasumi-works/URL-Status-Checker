# URL Status Checker

A concurrent URL health-checker built around Data-Oriented Programming (DOP), functional-style pipelines, and structured concurrency on modern Java.

The design follows one core principle: **data stays as data**. Domain values (`ScanRequest`, `Outcome`, `ScanResult`) are immutable records and sealed types with no behavior attached; all logic lives in small, composable functions that transform one immutable value into the next.


## How it works

1. **Input** — the user adds URLs through the UI, backed by a sealed `RowItem` (`Pending`/`Scanned`) shown in a single `TableView`, updated in place as results arrive.

2. **Scan trigger** — clicking Scan rebuilds `Operator.requestList` from every row (regardless of prior state, so nothing is ever permanently skipped), then runs `Operator.setUp()` to build one `Callable<ScanResult>` per request.

3. **Execution** — `Operator.executeScan()` forks every task into a `StructuredTaskScope`, joined by a custom `Joiner` (`CustomJoin`) that routes each result into successes or failures as it completes, and reports individual task-level failures via an injected fail-safe callback rather than crashing the scan.

4. **Network layer** — each request goes out via `java.net.http.HttpClient` with browser-like headers, returning a `Success` or `Fail` `Outcome` — network errors and malformed URLs are both caught and converted into a normal `Fail` result rather than throwing.

5. **Aggregation** — once all tasks complete, `Report.summarize()` folds the results into a `CountStat` (total/success/failure counts) alongside the raw success/failure lists.

6. **Export** — `JSON_DTO`/`CSV_DTO` convert the report into `JSON`/`CSV` records, memoized via `LazyConstant` so repeated exports don't reconvert, and written out through the OS file picker.

7. **UI responsiveness** — the whole scan runs inside a `javafx.concurrent.Task` on a background thread, so the UI never freezes during network calls; per-row UI updates from `CustomJoin`'s callback are marshaled back onto the JavaFX Application Thread via `Platform.runLater`.

## Tech

- Java 26 (structured concurrency / `StructuredTaskScope`, virtual threads, sealed interfaces, records, pattern matching)
- `LazyConstant` (JEP 526/531, preview) for memoized JSON/CSV export data
- `java.net.http.HttpClient` for HTTPS requests, with browser-like headers to avoid trivial bot-detection false positives
- JavaFX, with scans run via `javafx.concurrent.Task` to keep the UI responsive during network calls
- JUnit 5, including real-network integration tests (`@Tag("integration")`)

## Status

Built incrementally, phase by phase:

- [x] Phase 1 — Core data types (`ScanRequest`, `Outcome`, `ScanResult`)
- [x] Phase 2 — Execute stage (HTTPS request per URL)
- [x] Phase 3 — Concurrent execution via `StructuredTaskScope` + custom `Joiner`
- [x] Phase 4 — Aggregate stage (stats over `ExecutionResult`)
- [x] Phase 5 — Sinks (JSON, CSV export, UI output)
- [x] Phase 6 — Input sources: manual entry via UI (bulk/file import intentionally out of scope)
- [x] Phase 7 — Testing: unit tests for all pure logic + one real-network integration test
- [x] Phase 8 — Bug fixes & decoupling: `CustomJoin`/`Operator` failure callbacks injected via constructor (no hardcoded UI dependency), scan moved off the JavaFX Application Thread via `Task`, malformed-URL handling

## Known limitations

- No bulk/file-based URL import — URLs are entered manually through the UI
- Export destination is user-chosen via the OS file picker; no built-in disk-space handling

## Running
```
./gradlew run
```
> Requires `--enable-preview` for structured concurrency and `LazyConstant` — make sure your `build.gradle` compiler/run tasks pass that flag.

## Testing
```
./gradlew test
```
> Integration tests (tagged `integration`) make real HTTPS requests and are slower than the unit suite.


