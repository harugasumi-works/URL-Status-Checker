package statuscheck.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import statuscheck.domain.Fail;
import statuscheck.domain.RowItem;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.domain.Success;

public class ScanSummaryTest {
	private static final Instant T = Instant.parse("2026-09-28T00:00:00Z");

	@Test
	public void summaryCountsSuccessAndFailure() {
		List<RowItem> items = List.of(
				new RowItem.Scanned(new ScanResult("1", new ScanRequest("1", "a.com"), new Success(T, 200, 10))),
				new RowItem.Scanned(new ScanResult("2", new ScanRequest("2", "b.com"), new Fail(T, 404, "Not found"))));

		assertEquals("Scan finished: 1 OK, 1 failed", ScanSummary.format(items, Set.of("1", "2"), 0));
	}

	@Test
	public void summaryCountsPendingRowsAsNotScanned() {
		List<RowItem> items = List.of(
				new RowItem.Pending(new ScanRequest("1", "a.com")),
				new RowItem.Scanned(new ScanResult("2", new ScanRequest("2", "b.com"), new Success(T, 200, 10))));

		assertEquals("Scan finished: 1 OK, 0 failed, 1 not scanned",
				ScanSummary.format(items, Set.of("1", "2"), 0));
	}

	@Test
	public void summaryReportsTaskFailures() {
		List<RowItem> items = List.of(
				new RowItem.Pending(new ScanRequest("1", "a.com")));

		assertEquals(
				"Scan finished: 0 OK, 0 failed, 1 not scanned\n\n1 scan task(s) failed unexpectedly; those rows are still pending.",
				ScanSummary.format(items, Set.of("1"), 1));
	}

	@Test
	public void rowsOutsideCurrentScanAreIgnored() {
		List<RowItem> items = List.of(
				new RowItem.Scanned(new ScanResult("1", new ScanRequest("1", "a.com"), new Success(T, 200, 10))),
				new RowItem.Scanned(new ScanResult("2", new ScanRequest("2", "b.com"), new Fail(T, 404, "Not found"))));

		assertEquals("Scan finished: 1 OK, 0 failed", ScanSummary.format(items, Set.of("1"), 0));
	}
}
