package statuscheck;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import statuscheck.domain.Fail;
import statuscheck.domain.RowItem;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.domain.SessionRow;
import statuscheck.domain.Success;

class SessionRowTest {
	private static final Instant T = Instant.parse("2026-09-28T00:00:00Z");

	private static RowItem scanned(String id, String url, Success s) {
		return new RowItem.Scanned(new ScanResult(id, new ScanRequest(id, url), s));
	}

	private static RowItem scanned(String id, String url, Fail f) {
		return new RowItem.Scanned(new ScanResult(id, new ScanRequest(id, url), f));
	}

	@Test
	void roundTripKeepsPendingSuccessAndFail() {
		List<RowItem> items = List.of(new RowItem.Pending(new ScanRequest("1", "a.com")),
				scanned("2", "b.com", new Success(T, 200, 42)),
				scanned("3", "c.com", new Fail(T, 404, "Client failed to make a request.")));

		assertEquals(items, SessionRow.toRowItem(SessionRow.toSessionRow(items)));
	}

	@Test
	void failWithNullReasonIsAccepted() {
		List<RowItem> items = List.of(scanned("1", "a.com", new Fail(T, 0, null)));

		assertEquals(items, SessionRow.toRowItem(SessionRow.toSessionRow(items)));
	}

	@Test
	void pendingRowIgnoresOutcome() {
		assertDoesNotThrow(() -> new SessionRow("Pending", null, "1", "a.com", null, 0, 0, null));
	}

	@Test
	void scannedRowWithoutOutcomeIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new SessionRow("Scanned", null, "1", "a.com", T, 200, 1, ""));
	}

	@Test
	void unknownOutcomeIsRejectedInsteadOfBecomingFail() {
		assertThrows(IllegalArgumentException.class,
				() -> new SessionRow("Scanned", "Sucess", "1", "a.com", T, 200, 1, ""));
	}

	@Test
	void unknownOrMissingRowTypeIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new SessionRow("Done", "Success", "1", "a.com", T, 200, 1, ""));
		assertThrows(IllegalArgumentException.class,
				() -> new SessionRow(null, "Success", "1", "a.com", T, 200, 1, ""));
	}

	@Test
	void missingIdOrUrlIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new SessionRow("Pending", "", null, "a.com", null, 0, 0, ""));
		assertThrows(IllegalArgumentException.class,
				() -> new SessionRow("Pending", "", "1", " ", null, 0, 0, ""));
	}

	@Test
	void scannedRowWithoutTimeIsRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new SessionRow("Scanned", "Success", "1", "a.com", null, 200, 1, ""));
	}

	@Test
	void duplicateRowIdIsRejected() {
		SessionRow first = new SessionRow("Pending", "", "1", "a.com", null, 0, 0, "");
		SessionRow second = new SessionRow("Pending", "", "1", "b.com", null, 0, 0, "");

		assertThrows(IllegalArgumentException.class, () -> SessionRow.toRowItem(List.of(first, second)));
	}

	@Test
	void nullRowIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> SessionRow.toRowItem(java.util.Arrays.asList((SessionRow) null)));
	}

	@Test
	void credentialsAreRemovedBeforeSessionPersistence() {
		RowItem item = new RowItem.Pending(
				new ScanRequest("1", "https://user:pass@example.com/path"));

		SessionRow row = SessionRow.toSessionRow(List.of(item)).get(0);

		assertEquals("https://example.com/path", row.url());
	}
}
