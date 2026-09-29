package statuscheck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import statuscheck.domain.Fail;
import statuscheck.domain.RowItem;
import statuscheck.domain.ScanRequest;
import statuscheck.domain.ScanResult;
import statuscheck.domain.Success;
import statuscheck.service.ScanService;

public class ScanServiceTest {
	private static final Instant T = Instant.parse("2026-09-28T00:00:00Z");

	@Test
	public void requestsFromReturnsPendingRequests() {
		List<RowItem> items = List.of(
				new RowItem.Pending(new ScanRequest("1", "a.com")),
				new RowItem.Pending(new ScanRequest("2", "b.com")));

		assertEquals(
				List.of(new ScanRequest("1", "a.com"), new ScanRequest("2", "b.com")),
				ScanService.requestsFrom(items));
	}

	@Test
	public void requestsFromReusesContextFromScannedRows() {
		ScanRequest request = new ScanRequest("1", "a.com");
		RowItem item = new RowItem.Scanned(
				new ScanResult("1", request, new Success(T, 200, 10)));

		assertEquals(List.of(request), ScanService.requestsFrom(List.of(item)));
	}

	@Test
	public void requestsFromKeepsPendingAndScannedRows() {
		ScanRequest pending = new ScanRequest("1", "a.com");
		ScanRequest scanned = new ScanRequest("2", "b.com");
		List<RowItem> items = List.of(
				new RowItem.Pending(pending),
				new RowItem.Scanned(new ScanResult("2", scanned, new Fail(T, 404, "Not found"))));

		assertEquals(List.of(pending, scanned), ScanService.requestsFrom(items));
	}

	@Test
	public void requestsFromEmptyListReturnsEmpty() {
		assertEquals(List.of(), ScanService.requestsFrom(List.of()));
	}
}
