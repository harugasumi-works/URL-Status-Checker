# Development

Setup, testing, and project history for URL Status Checker.

> Back to the [README](../README.md) · Design notes: [ARCHITECTURE.md](ARCHITECTURE.md)

## Requirements

- JDK 26
- Gradle 9.7.1 (the Gradle wrapper is included)
- JavaFX 26 (resolved by the Gradle JavaFX plugin)

The Gradle build enables Java preview features for compile, test, and run tasks, so no extra flags are needed.

## Tech stack

- **Java 26**: records, sealed types, pattern matching, and structured concurrency APIs used by the project.
- **JavaFX 26**: desktop UI and `javafx.concurrent.Task` integration.
- **Jackson 3.2.2**: JSON and CSV serialization.
- **JUnit Jupiter 5.10.2**: unit and integration testing.
- **Gradle 9.7.1**: build and test automation.
- **GitHub Actions**: runs `./gradlew assemble` and `./gradlew test` on every push and pull request.

## Running

The Gradle project is under `StatusCheck/`.

**macOS / Linux:**

```bash
cd StatusCheck
./gradlew run
```

**Windows:**

```bat
cd StatusCheck
gradlew.bat run
```

## Testing

Run the tests from `StatusCheck/`. The standard `test` task runs the offline/unit test suite and excludes tests tagged `integration`. `integrationTest` runs the real-network tests and requires internet access, so it is not part of CI.

**macOS / Linux:**

```bash
cd StatusCheck
./gradlew test
./gradlew integrationTest
```

**Windows:**

```bat
cd StatusCheck
gradlew.bat test
gradlew.bat integrationTest
```

The test suite covers parsing, import/export services, scanning, session-row conversion, CSV/JSON output, autosave behavior, and URL credential sanitization. Integration tests make real HTTPS requests and therefore depend on network access.

## Development phases

The project was built incrementally. The earlier eight phases describe the original DOP-oriented core, while the later phases reflect the substantial changes made from **September 26, 2026 onward**.

### Earlier phases

- [x] **Phase 1 — Core data types**: `ScanRequest`, `Outcome`, `Success`, `Fail`, and `ScanResult`.
- [x] **Phase 2 — Network execution**: HTTPS request creation and status classification.
- [x] **Phase 3 — Structured concurrency**: concurrent execution with `StructuredTaskScope` and a custom joiner.
- [x] **Phase 4 — Aggregation**: success/failure statistics and reporting.
- [x] **Phase 5 — Output**: JSON/CSV conversion and JavaFX export UI.
- [x] **Phase 6 — Manual input**: interactive URL entry from the desktop UI.
- [x] **Phase 7 — Testing**: unit tests plus real-network integration tests.
- [x] **Phase 8 — Error handling and decoupling**: background scanning, malformed-URL handling, callback injection, and removal of hardcoded UI dependencies from the scanner.

### September 26–29, 2026: current application evolution

- [x] **Phase 9 — Row deletion and scanner purity (September 26)**  
  Added single-row and clear-all deletion, with the scanner made data-independent by moving result storage outside the concurrency operator.  
  [Commit: `73575a2`](https://github.com/harugasumi-works/URL-Status-Checker/commit/73575a2e8569363d895c88938c19766dd8c17390)

- [x] **Phase 10 — Bulk import and UI restructuring (September 27)**  
  Added the bulk-import window and `.txt` import support, while reorganizing the JavaFX package structure and updating tests.  
  [Commit: `7e80728`](https://github.com/harugasumi-works/URL-Status-Checker/commit/7e807286fdae7e722ca68d7248e373db4c6ca4b7)

- [x] **Phase 11 — Autosave and data-loss prevention (September 28)**  
  Added periodic autosave, session restoration, close-time save/discard/cancel behavior, and persistence recovery mechanisms.  
  [Commit: `b151233`](https://github.com/harugasumi-works/URL-Status-Checker/commit/b1512331387c217358ac6afcc7445487ad7266c2)

- [x] **Phase 12 — Scan/session reliability hardening (September 28)**  
  Fixed re-scan behavior, pending-row clearing, deleted-row races, malformed session handling, background autosave threading, and several MainButtons/Operator edge cases.  
  Representative commits: [`bca129c`](https://github.com/harugasumi-works/URL-Status-Checker/commit/bca129c522658b3c13bc295f82c35768ed845be8), [`c2a6e60`](https://github.com/harugasumi-works/URL-Status-Checker/commit/c2a6e60ea75a0f69c4e1056d07afb0e447199346), [`8fcc316`](https://github.com/harugasumi-works/URL-Status-Checker/commit/8fcc316488b20eea146213e102aede0ade17977d), [`f365874`](https://github.com/harugasumi-works/URL-Status-Checker/commit/f36587435caa5a91e09297fee2bb9c53932dab9d)

- [x] **Phase 13 — Input validation and output security (September 28–29)**  
  Improved URL parsing for IDNs and underscored hosts, exposed rejected/duplicate import lines, normalized manual input, prevented CSV formula injection, added URL credential sanitization, and fixed autosave retry behavior.  
  Representative commits: [`c303bbb`](https://github.com/harugasumi-works/URL-Status-Checker/commit/c303bbba98d3a895c5391a0d2b423db4245e6987), [`d65ee3b`](https://github.com/harugasumi-works/URL-Status-Checker/commit/d65ee3b1595e94d62789c5caa8f71dfc9029c199), [`3af8dd4`](https://github.com/harugasumi-works/URL-Status-Checker/commit/3af8dd4737d9f93731ed51da9bc36137b44e80e0)

- [x] **Phase 14 — Service decoupling, HTTP support, and test cleanup (September 29)**  
  Split UI orchestration from scan/import/export logic, introduced dedicated services, moved dialogs and buttons into their current packages, enabled HTTP as well as HTTPS, and added/updated tests to match the refactored code.  
  Representative commits: [`8faa558`](https://github.com/harugasumi-works/URL-Status-Checker/commit/8faa558889cd58ed8c53c39c4a4e253ea7cb996c), [`70ea9ac`](https://github.com/harugasumi-works/URL-Status-Checker/commit/70ea9ac979e2860c3ea6e3b8efd8e2e70aecaa10), [`f151640`](https://github.com/harugasumi-works/URL-Status-Checker/commit/f1516405b50a833e33d963df6c01ad60ffa20487), [`60ac7eb`](https://github.com/harugasumi-works/URL-Status-Checker/commit/60ac7ebdfdb0624ad8278937513e22d49123e5ae)

## Current state

The project is best understood as a small desktop application that uses DOP-inspired immutable data modeling inside an OOP application shell. The architecture has moved significantly beyond the original pure-DOP experiment as features such as deletion, bulk import, autosave, cancellation, HTTP support, session recovery, credential sanitization, and service decoupling were added.
