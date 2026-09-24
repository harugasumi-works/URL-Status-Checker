# URL Status Checker

A concurrent URL health-checker built around Data-Oriented Programming (DOP), functional-style pipelines, and structured concurrency on modern Java.

The design follows one core principle: **data stays as data**. Domain values (`ScanRequest`, `Outcome`, `ScanResult`) are immutable records and sealed types with no behavior attached; all logic lives in small, composable functions that transform one immutable value into the next.

## How it works

1. **Input** — the user adds URLs through the UI, backed by a sealed `RowItem` (`Pending`/`Scanned`) shown in a single `TableView`, updated in place as results arrive. Enter the host and path *without* a scheme (e.g. `example.com/health`); `https://` is prepended automatically.

2. **Scan trigger** — clicking **Scan** (`ButtonFactory.scanButton()`) rebuilds the request list from every row (regardless of prior state, so nothing is ever permanently skipped) and calls `Operator.scanAll()`.

3. **Execution** — `Operator.scanAll()` wraps each request in a `Callable<ScanResult>` and forks it into a `StructuredTaskScope`, joined by a custom `Joiner` (`CustomJoin`). As each task completes, `CustomJoin` routes its result into successes or failures and notifies the UI through an injected `onResult` callback. Task-level failures are reported through an injected `onTaskFailure` callback rather than crashing the scan.

4. **Network layer** — each request goes out via a shared `java.net.http.HttpClient` (5 s request timeout, 10 s connect timeout) with browser-like headers, and is classified into an `Outcome`:

   | Status code | Outcome |
   |-------------|---------|
   | `< 400`     | `Success` (with latency in ms) |
   | `400–499`   | `Fail` — client error |
   | `>= 500`    | `Fail` — server error |
   | no response | `Fail` — malformed URL or network error |

   Network errors and malformed URLs are caught and converted into a normal `Fail` result rather than thrown.

5. **Aggregation** — once all tasks complete, `Report.summarize()` folds the results into a `CountStat` (total/success/failure counts) alongside the raw success/failure lists.

6. **Export** — `ReturnOutput.output()` builds a `ScanOutput` whose `JSON` and `CSV` values are memoized with `LazyConstant` (`JsonDto` converts the `Report`, `CsvDto` converts the `ExecutionResult`), so repeated exports don't reconvert. The file is written through the OS file picker via the **Save** menu.

7. **UI responsiveness** — the whole scan runs inside a `javafx.concurrent.Task` on a background thread, so the UI never freezes during network calls; per-row updates from `CustomJoin`'s callback are marshaled back onto the JavaFX Application Thread via `Platform.runLater`.

## Tech

- Java 26 (structured concurrency / `StructuredTaskScope`, virtual threads, sealed interfaces, records, pattern matching), compiled and run with `--enable-preview`
- `LazyConstant` (JEP 526/531, preview) for memoized JSON/CSV export data
- `java.net.http.HttpClient` for HTTPS requests, with browser-like headers to avoid trivial bot-detection false positives
- JavaFX 26, with scans run via `javafx.concurrent.Task` to keep the UI responsive during network calls
- Jackson 3 (`tools.jackson`) for JSON and CSV serialization
- JUnit 5, including real-network integration tests (`@Tag("integration")`)

## Status

Built incrementally, phase by phase:

- [x] Phase 1 — Core data types (`ScanRequest`, `Outcome`, `ScanResult`)
- [x] Phase 2 — Execute stage (HTTPS request per URL)
- [x] Phase 3 — Concurrent execution via `StructuredTaskScope` + custom `Joiner`
- [x] Phase 4 — Aggregate stage (stats over `ExecutionResult`)
- [x] Phase 5 — Sinks (JSON, CSV export, UI output)
- [x] Phase 6 — Input sources: manual entry via UI (bulk/file import intentionally out of scope)
- [x] Phase 7 — Testing: unit tests for all pure logic + real-network integration tests
- [x] Phase 8 — Bug fixes & decoupling: `CustomJoin`/`Operator` failure callbacks injected via constructor/parameters (no hardcoded UI dependency), scan moved off the JavaFX Application Thread via `Task`, malformed-URL handling

## Known limitations

- No bulk/file-based URL import — URLs are entered manually through the UI
- HTTPS only — the `https://` scheme is always prepended, so plain `http://` URLs can't be checked
- Redirects are not followed, so a `3xx` response is reported as `Success`
- Export destination is user-chosen via the OS file picker; no built-in disk-space handling

## Requirements

- JDK 26 (the Gradle build uses a Java 26 toolchain)
- Gradle 9.7.1

## Running

The Gradle project lives in the `StatusCheck/` subdirectory:

```bash
cd StatusCheck
./gradlew run
```

## Testing

```bash
cd StatusCheck
./gradlew test
```

> Integration tests (tagged `integration`) make real HTTPS requests and are slower than the unit suite.
