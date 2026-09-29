# How to Use

## 1. Start the Application

From the repository root:

### macOS / Linux

```bash
cd StatusCheck
./gradlew run
```

### Windows

```bat
cd StatusCheck
gradlew.bat run
```

The application opens in a maximized JavaFX window.

---

## 2. Add a URL Manually

Use the **Enter Text** field at the top of the application.

1. Enter a URL.
2. Press **Enter**.
3. The URL is added to the table.

Both `http://` and `https://` URLs are supported.

If no scheme is specified, `https://` is used automatically.

For example:

```text
example.com
```

is normalized to:

```text
https://example.com
```

Invalid URLs are rejected and displayed as a notification.

Duplicate URLs are not added again.

---

## 3. Add Multiple URLs with Bulk Import

Click **Bulk Import** to open the import window.

You can either:

- Enter multiple URLs manually, one per line.
- Select a `.txt` file.

### Example

```text
https://example.com
https://github.com
http://example.org
example.net
```

Click **Add** to validate and add the URLs.

The application reports:

- Number of URLs added
- Number of duplicates
- Number of invalid entries

Invalid entries remain in the import field so they can be corrected.

### Import from a Text File

Click **Import from Files** and select a `.txt` file.

The file is read as a list of URL entries. Empty lines are ignored.

---

## 4. Scan URLs

After adding URLs, click **Scan**.

The application checks all URLs concurrently and updates the table as results become available.

The table contains:

| Column | Description |
|---|---|
| URL | URL being checked |
| Status | Success or failure |
| Code | HTTP status code |
| Detail | Additional result information |

The scanner automatically follows normal HTTP redirects.

A final `3xx` response that is not followed is reported as a failure.

---

## 5. Cancel a Scan

While a scan is running, the **Cancel scan** button becomes available.

Click it to stop the active scan.

The application displays:

```text
Scan cancelled.
```

---

## 6. Re-scan Existing URLs

URLs already present in the table can be scanned again.

Click **Scan** without adding new URLs.

The application rebuilds the scan requests from the current table and checks the existing URLs again.

This allows you to refresh previously collected results without re-entering the URLs.

---

## 7. Delete URLs

Select a row in the table and press the **Delete** key.

The selected URL is removed from the current session.

To remove everything, click **Clear all**.

A confirmation dialog is displayed before the entire list is cleared.

---

## 8. Export Results

Click the **Save** button to open the export menu.

Two formats are available:

- **Export to JSON**
- **Export to CSV**

### JSON

JSON contains the scan report and result information in a structured format.

### CSV

CSV contains the following columns:

```text
timeStamp,url,outcome,statusCode,latencyMs,reason
```

The exported data is suitable for opening in spreadsheet applications or processing with other tools.

---

## 9. URL Credentials and Export Safety

URLs containing credentials are sanitized when data is persisted or exported.

For example:

```text
https://user:password@example.com
```

is sanitized before being written to session storage, exports, or redirect messages.

CSV output also protects URL and reason fields against spreadsheet formula injection.

The URL displayed in the main table may still reflect the URL entered by the user.

---

## 10. Autosave

The application automatically saves the current session approximately every **20 seconds** when changes have been made.

The status bar displays the autosave state.

For example:

```text
Autosave on
```

or:

```text
Last saved: 21:35:42
```

The session is stored at:

```text
~/StatusCheck/session.json
```

The application also keeps a limited number of backup session files.

---

## 11. Restore a Previous Session

When the application starts, it checks for a previously saved session.

If one is available, the application asks:

```text
Restore previous session?
```

Choose **Yes** to restore the previous session.

Choose **No** to start with an empty list.

If the saved session is malformed, it is moved aside and the application starts without restoring it.

---

## 12. Save or Discard When Closing

When closing the application with data in the current session, you may be asked:

```text
Save this session before closing?
```

You can choose to:

- **Save** — save the current session before closing.
- **Discard** — close without saving the current session.
- **Cancel** — keep the application open.

---

## 13. What the Scanner Checks

For each URL, the scanner records information such as:

- HTTP status code
- Success or failure
- Response latency
- Failure reason
- Timestamp

The scanner uses:

- HTTP/HTTPS
- GET requests
- Redirect following
- Request timeout
- Connection timeout
- Concurrent scanning

Network concurrency is limited to prevent an excessive number of simultaneous requests.

Typical failures include:

```text
4xx Client Error
5xx Server Error
Connection failure
Request timeout
Invalid request
```

A successful response is reported for HTTP status codes below `300`.

---

## 14. Typical Workflow

A normal workflow is:

```text
Start application
      ↓
Add URLs
      ↓
Scan
      ↓
Review results
      ↓
Re-scan when necessary
      ↓
Export JSON or CSV
      ↓
Close application
```

For a large list of URLs:

```text
Prepare .txt file
      ↓
Bulk Import
      ↓
Review imported URLs
      ↓
Scan
      ↓
Export results
```

---

## 15. Requirements

The application currently requires:

- JDK 26
- Gradle 9.7.1
- JavaFX 26

The project uses Java preview features, so the application must be run using the provided Gradle configuration.

### Run Tests

Unit tests:

```bash
./gradlew test
```

Integration tests:

```bash
./gradlew integrationTest
```

Integration tests perform real network requests and therefore require network access.
